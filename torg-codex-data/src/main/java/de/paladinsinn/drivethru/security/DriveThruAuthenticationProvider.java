package de.paladinsinn.drivethru.security;
import java.util.Collections;
import java.util.List;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import de.paladinsinn.drivethru.DriveThruRPGService;
import de.paladinsinn.drivethru.token.DrivethruToken;
import de.paladinsinn.drivethru.token.NoValidTokenException;
import de.paladinsinn.torg.codex.data.model.Publication;
import de.paladinsinn.torg.codex.data.repository.PublicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
/**
 * Authenticates an {@link ApiKeyAuthenticationToken} by calling the DriveThruRPG
 * {@code /token} endpoint. On success, the returned bearer token and customer id
 * are wrapped in a {@link DriveThruUserDetails} and stored in the security context.
 *
 * <p>Additionally, all DriveThruRPG product IDs owned by the customer are resolved
 * to their {@code codexId} strings via {@link PublicationRepository} and stored in
 * {@link DriveThruUserDetails#getOwnedCodexIds()}.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DriveThruAuthenticationProvider implements AuthenticationProvider {
    private final DriveThruRPGService driveThruRPGService;
    private final PublicationRepository publicationRepository;
    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        final String apiKey = (String) authentication.getPrincipal();
        log.debug("Authenticating DriveThruRPG API key.");
        try {
            final DrivethruToken token = driveThruRPGService.getToken(apiKey);
            final List<String> ownedCodexIds = resolveOwnedCodexIds(token);
            final DriveThruUserDetails userDetails = new DriveThruUserDetails(token, ownedCodexIds);
            log.info("DriveThruRPG authentication successful. customerId={}, ownedCodexIds={}",
                    userDetails.getCustomerId(), ownedCodexIds.size());
            return ApiKeyAuthenticationToken.authenticated(userDetails);
        } catch (final NoValidTokenException e) {
            log.warn("DriveThruRPG authentication failed: invalid API key.");
            throw new BadCredentialsException("Invalid DriveThruRPG API key.", e);
        }
    }
    @Override
    public boolean supports(final Class<?> authClass) {
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authClass);
    }

    /**
     * Fetches all product IDs owned by the authenticated user and maps each one
     * to the corresponding {@code codexId} stored in {@code torg_publication}.
     * Product IDs that are not found in the publication table are silently skipped.
     *
     * @param token the validated DriveThruRPG token
     * @return unordered list of owned codex ids; never {@code null}
     */
    private List<String> resolveOwnedCodexIds(final DrivethruToken token) {
        List<Integer> productIds;
        try {
            productIds = driveThruRPGService.getIdsOfOwnedProducts(token);
        } catch (final NoValidTokenException e) {
            log.warn("Could not retrieve owned product ids during authentication – returning empty list.", e);
            return Collections.emptyList();
        }
        final List<String> codexIds = productIds.stream()
                .flatMap(pid -> publicationRepository.findByProductId(pid).stream())
                .map(Publication::getCodexId)
                .distinct()
                .toList();
        log.debug("Resolved {} owned codex ids from {} product ids.", codexIds.size(), productIds.size());
        return codexIds;
    }
}
