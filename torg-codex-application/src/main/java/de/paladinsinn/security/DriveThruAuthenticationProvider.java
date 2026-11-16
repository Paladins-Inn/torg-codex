/*
 * Copyright (c) 2026.  Roland T. Lichti <rlichti@kaiserpfalz-edv.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * ERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * You may contact me via email rlichti@kaiserpfalz-edv.de or via mail
 *
 * Kaiserpfalz EDV-Service
 * Roland T. Lichti
 * Darmstädter Str. 12
 * 64625 Bensheim
 * GERMANY
 */

package de.paladinsinn.security;

import de.paladinsinn.drivethru.ApiKeyFingerprint;
import de.paladinsinn.drivethru.DriveThruLibrarySnapshot;
import de.paladinsinn.drivethru.DriveThruRPGService;
import de.paladinsinn.drivethru.LibraryLoadCoordinator;
import de.paladinsinn.drivethru.token.DrivethruToken;
import de.paladinsinn.drivethru.token.NoValidTokenException;
import de.paladinsinn.torg.codex.application.port.in.CatalogPublicationReference;
import de.paladinsinn.torg.codex.application.port.in.CatalogReferenceQuery;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Authenticates an {@link ApiKeyAuthenticationToken} by calling the DriveThruRPG
 * {@code /auth_key} endpoint. On success, the returned access token
 * are wrapped in a {@link DriveThruUserDetails} and stored in the security context.
 *
 * <p>Additionally, all DriveThruRPG product IDs owned by the customer are resolved
 * to their {@code codexId} strings via {@link CatalogReferenceQuery} and stored in
 * {@link DriveThruUserDetails#ownedCodexIds()}.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DriveThruAuthenticationProvider implements AuthenticationProvider {
    private final DriveThruRPGService driveThruRPGService;
    private final CatalogReferenceQuery catalogReferenceQuery;
    @Setter(onMethod_ = @Autowired)
    private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;
    @Setter(onMethod_ = @Autowired(required = false))
    private LibraryLoadCoordinator libraryLoadCoordinator;

    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        Observation observation = Observation.createNotStarted("drivethru.authentication", observationRegistry)
                .contextualName("drivethru authenticate")
                .start();
        try (Observation.Scope ignored = observation.openScope()) {
            log.atInfo()
                    .addKeyValue("event.action", "drivethru-auth-started")
                    .log("DriveThruRPG authentication started.");
            Authentication result = authenticateKey(authentication);
            observation.lowCardinalityKeyValue("outcome", "success");
            return result;
        } catch (RuntimeException failure) {
            String outcome = failure instanceof BadCredentialsException ? "rejected" : "error";
            observation.lowCardinalityKeyValue("outcome", outcome);
            // Upstream exception messages and causes may contain credentials.
            observation.error(new IllegalStateException("DriveThruRPG authentication " + outcome));
            throw failure;
        } finally {
            observation.stop();
        }
    }

    private Authentication authenticateKey(final Authentication authentication) {
        final String apiKey = (String) authentication.getPrincipal();
        log.debug("Authenticating DriveThruRPG API key.");
        String stage = "token-exchange";
        try {
            final DrivethruToken token = driveThruRPGService.getToken(apiKey);
            stage = "library-load";
            log.debug("DriveThruRPG token exchange succeeded; resolving publication ownership.");
            final List<String> ownedCodexIds = resolveOwnedCodexIds(apiKey, token);
            final DrivethruToken sanitizedToken = sanitizeToken(token);
            final DriveThruUserDetails userDetails = new DriveThruUserDetails(sanitizedToken, ownedCodexIds);
            log.info("DriveThruRPG authentication successful. ownedCodexIds={}", ownedCodexIds.size());
            return ApiKeyAuthenticationToken.authenticated(userDetails);
        } catch (final NoValidTokenException e) {
            log.atDebug()
                    .addKeyValue("stage", stage)
                    .addKeyValue("exceptionClass", e.getClass().getName())
                    .log("DriveThruRPG authentication rejected.");
            log.atInfo()
                    .addKeyValue("event.action", "drivethru-auth-rejected")
                    .addKeyValue("reason", "invalid-api-key")
                    .log("DriveThruRPG authentication failed: invalid API key.");
            throw new BadCredentialsException("Invalid DriveThruRPG API key.", e);
        }
    }
    @Override
    public boolean supports(final @NonNull Class<?> authClass) {
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
    private List<String> resolveOwnedCodexIds(
            final String apiKey,
            final DrivethruToken token) throws NoValidTokenException {
        final List<Integer> productIds;
        if (libraryLoadCoordinator != null) {
            DriveThruLibrarySnapshot snapshot = libraryLoadCoordinator.loadCoordinated(
                    ApiKeyFingerprint.of(apiKey), token);
            if (token.getCustomerId() == null && snapshot.customerId() != null) {
                token.setCustomerId(snapshot.customerId());
            }
            productIds = snapshot.productIds();
        } else {
            productIds = driveThruRPGService.getIdsOfOwnedProducts(token);
        }
        if (productIds.isEmpty()) {
            return List.of();
        }
        final List<String> codexIds = catalogReferenceQuery.findPublicationsByProductIds(Set.copyOf(productIds)).stream()
                .map(CatalogPublicationReference::codexId)
                .distinct()
                .toList();
        log.debug("Resolved {} owned codex ids from {} product ids.", codexIds.size(), productIds.size());
        return codexIds;
    }

    private static DrivethruToken sanitizeToken(final DrivethruToken token) {
        DrivethruToken copy = token.sessionCopy();
        return copy != null ? copy : token;
    }
}
