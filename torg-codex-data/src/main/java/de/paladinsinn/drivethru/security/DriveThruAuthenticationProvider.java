package de.paladinsinn.drivethru.security;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import de.paladinsinn.drivethru.DriveThruRPGService;
import de.paladinsinn.drivethru.token.DrivethruToken;
import de.paladinsinn.drivethru.token.NoValidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
/**
 * Authenticates an {@link ApiKeyAuthenticationToken} by calling the DriveThruRPG
 * {@code /token} endpoint. On success, the returned bearer token and customer id
 * are wrapped in a {@link DriveThruUserDetails} and stored in the security context.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DriveThruAuthenticationProvider implements AuthenticationProvider {
    private final DriveThruRPGService driveThruRPGService;
    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        final String apiKey = (String) authentication.getPrincipal();
        log.debug("Authenticating DriveThruRPG API key.");
        try {
            final DrivethruToken token = driveThruRPGService.getToken(apiKey);
            final DriveThruUserDetails userDetails = new DriveThruUserDetails(token);
            log.info("DriveThruRPG authentication successful. customerId={}", userDetails.getCustomerId());
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
}
