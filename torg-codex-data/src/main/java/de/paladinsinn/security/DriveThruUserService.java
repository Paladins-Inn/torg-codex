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
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import de.paladinsinn.drivethru.DriveThruRPGService;
import de.paladinsinn.drivethru.products.OwnedProduct;
import de.paladinsinn.drivethru.token.NoValidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
/**
 * Service that operates on behalf of the currently authenticated
 * DriveThruRPG user stored in the Spring Security context.
 *
 * <p>Use {@link #getCurrentUser()} to obtain the user details (including the
 * bearer token) for direct API calls, or use the convenience methods which
 * delegate to {@link DriveThruRPGService} automatically.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DriveThruUserService {
    private final DriveThruRPGService driveThruRPGService;
    // -------------------------------------------------------------------------
    // Current-user access
    // -------------------------------------------------------------------------
    /**
     * Returns the {@link DriveThruUserDetails} of the currently authenticated user.
     *
     * @return current user or {@link Optional#empty()} if no authenticated user is present
     */
    public Optional<DriveThruUserDetails> getCurrentUser() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof ApiKeyAuthenticationToken token
                && token.isAuthenticated()
                && token.getPrincipal() instanceof DriveThruUserDetails userDetails) {
            return Optional.of(userDetails);
        }
        return Optional.empty();
    }
    /**
     * Returns the {@link DriveThruUserDetails} of the currently authenticated user,
     * throwing an {@link IllegalStateException} if no authenticated user is present.
     *
     * @return current user (never {@code null})
     * @throws IllegalStateException if called outside an authenticated request
     */
    public DriveThruUserDetails requireCurrentUser() {
        return getCurrentUser().orElseThrow(() ->
                new IllegalStateException("No authenticated DriveThruRPG user in security context."));
    }
    // -------------------------------------------------------------------------
    // DriveThruRPG operations for the current user
    // -------------------------------------------------------------------------
    /**
     * Retrieves all product ids owned by the currently authenticated user,
     * iterating through all pages automatically.
     *
     * @return list of owned product ids
     * @throws NoValidTokenException if the token in the security context is no longer valid
     */
    public List<Integer> getOwnedProductIds() throws NoValidTokenException {
        final DriveThruUserDetails user = requireCurrentUser();
        log.debug("Retrieving owned product ids. customerId={}", user.getCustomerId());
        return driveThruRPGService.getIdsOfOwnedProducts(user.getToken());
    }
    /**
     * Retrieves a single page of products owned by the currently authenticated user.
     *
     * @param page      1-based page number
     * @param pageSize  items per page
     * @param archived  {@code 0} = exclude archived, {@code 1} = include archived
     * @return list of owned products on the requested page
     */
    public List<OwnedProduct> getOwnedProducts(final int page, final int pageSize, final int archived) {
        final DriveThruUserDetails user = requireCurrentUser();
        log.debug("Retrieving owned products. customerId={}, page={}, pageSize={}", user.getCustomerId(), page, pageSize);
        return driveThruRPGService.getOwnedProducts(user.getToken(), page, pageSize, archived);
    }
}
