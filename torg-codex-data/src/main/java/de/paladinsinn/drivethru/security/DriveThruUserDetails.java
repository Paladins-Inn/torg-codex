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

package de.paladinsinn.drivethru.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import de.paladinsinn.drivethru.token.DrivethruToken;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Spring Security {@link UserDetails} wrapping a DriveThruRPG bearer token.
 *
 * <p>The {@link #getUsername()} returns the DriveThruRPG {@code customers_id};
 * the {@link #getBearerToken()} and {@link #getToken()} methods expose the
 * token data for downstream API calls.</p>
 */
@RequiredArgsConstructor
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class DriveThruUserDetails implements UserDetails {

    private static final GrantedAuthority ROLE_USER =
            new SimpleGrantedAuthority("ROLE_DRIVETHRU_USER");

    /** The validated DriveThruRPG token obtained from the API. */
    @ToString.Include
    private final DrivethruToken token;

    // -------------------------------------------------------------------------
    // UserDetails
    // -------------------------------------------------------------------------

    @Override
    public String getUsername() {
        return token.getCustomerId();
    }

    /** Not used – authentication is API-key based, not password based. */
    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(ROLE_USER);
    }

    @Override public boolean isAccountNonExpired()  { return true; }
    @Override public boolean isAccountNonLocked()   { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()            { return true; }

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
}

