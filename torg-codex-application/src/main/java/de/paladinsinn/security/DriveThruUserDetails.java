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

import de.paladinsinn.drivethru.token.DrivethruToken;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Spring Security {@link UserDetails} wrapping a DriveThruRPG bearer token.
 *
 * <p>The {@link #getUsername()} returns the DriveThruRPG {@code customers_id};
 * the {@link #getBearerToken()} and {@link #token ()} methods expose the
 * token data for downstream API calls.</p>
 *
 * <p>The {@link #ownedCodexIds ()} method provides a read-only list of
 * codex identifiers (e.g. {@code "core-rulebook"}) for all publications the
 * authenticated user owns on DriveThruRPG.</p>
 *
 * @param token         The validated DriveThruRPG token obtained from the API.
 * @param ownedCodexIds Codex identifiers of all publications owned by this user on DriveThruRPG
 *                      (e.g. {@code "core-rulebook"}, {@code "sourcebook-aysle"}).
 *
 *                      <p>Populated at authentication time by looking up each owned DriveThruRPG
 *                      product ID in the {@code torg_publication} table.</p>
 */
public record DriveThruUserDetails(@ToString.Include DrivethruToken token,
                                   List<String> ownedCodexIds) implements UserDetails {

    private static final GrantedAuthority ROLE_USER =
            new SimpleGrantedAuthority("ROLE_DRIVETHRU_USER");

    // -------------------------------------------------------------------------
    // UserDetails
    // -------------------------------------------------------------------------
    @Override
    public String getUsername() {
        return token.getCustomerId();
    }

    /**
     * Not used – authentication is API-key based, not password based.
     */
    @Override
    public String getPassword() {
        return null;
    }

    /**
     * Returns the user's granted authorities.
     *
     * <ul>
     *   <li>{@code ROLE_DRIVETHRU_USER} – always present for authenticated users.</li>
     *   <li>{@code ROLE_<codexId>} – one entry per owned publication, e.g.
     *       {@code ROLE_core-rulebook}. These authorities are read by the application's
     *       product-ownership resolver to resolve product-gated markup blocks.</li>
     * </ul>
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        final List<GrantedAuthority> authorities = new ArrayList<>(ownedCodexIds.size() + 1);
        authorities.add(ROLE_USER);
        ownedCodexIds.stream()
                .map(id -> new SimpleGrantedAuthority("ROLE_" + id))
                .forEach(authorities::add);
        return Collections.unmodifiableList(authorities);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // -------------------------------------------------------------------------
    // DriveThruRPG helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the DriveThruRPG customer id (same as {@link #getUsername()}).
     *
     * @return customer id
     */
    public String getCustomerId() {
        return token.getCustomerId();
    }

    /**
     * Returns the {@code "Bearer <accessToken>"} string ready for the
     * {@code Authorization} header of subsequent DriveThruRPG API calls.
     *
     * @return bearer token header value
     */
    public String getBearerToken() {
        return token.getBearerToken();
    }

    /**
     * Returns a read-only list of codex identifiers for all publications this
     * user owns on DriveThruRPG (e.g. {@code "core-rulebook"}, {@code "sourcebook-aysle"}).
     *
     * @return unmodifiable list of owned codex ids; never {@code null}
     */
    @Override
    public List<String> ownedCodexIds() {
        return Collections.unmodifiableList(ownedCodexIds);
    }
}

