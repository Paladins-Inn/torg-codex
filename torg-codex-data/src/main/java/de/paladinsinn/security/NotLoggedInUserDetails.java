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

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Represents an unauthenticated / anonymous visitor of the Torg Codex.
 *
 * <p>This principal is used as the Spring Security <em>anonymous</em> user so that
 * even non-logged-in users automatically own the free {@code core-rulebook} product.
 * As a result, {@link de.paladinsinn.torg.codex.markup.SecuredMarkupService} will
 * expose all core-rulebook content to anonymous visitors without requiring login.</p>
 *
 * <ul>
 *   <li>{@link #getUsername()} → {@value #USERNAME}</li>
 *   <li>{@link #getOwnedCodexIds()} → {@code ["core-rulebook"]}</li>
 *   <li>{@link #getAuthorities()} → {@code [ROLE_core-rulebook]}</li>
 * </ul>
 *
 * <p>Use the pre-built singleton {@link #INSTANCE} wherever this principal is needed.</p>
 */
public final class NotLoggedInUserDetails implements UserDetails {

    /** Display name shown for unauthenticated users. */
    public static final String USERNAME = "Not Logged In";

    /**
     * The single codex product accessible without a DriveThruRPG account.
     * All other products require login.
     */
    public static final String FREE_PRODUCT_ID = "core-rulebook";

    /** Singleton instance – reuse this instead of constructing new objects. */
    public static final NotLoggedInUserDetails INSTANCE = new NotLoggedInUserDetails();

    private static final List<GrantedAuthority> AUTHORITIES =
            List.of(new SimpleGrantedAuthority("ROLE_" + FREE_PRODUCT_ID));

    private static final List<String> OWNED_CODEX_IDS = List.of(FREE_PRODUCT_ID);

    /** Use {@link #INSTANCE} instead. */
    private NotLoggedInUserDetails() {}

    // -------------------------------------------------------------------------
    // UserDetails
    // -------------------------------------------------------------------------

    @Override
    public String getUsername() {
        return USERNAME;
    }

    /** Always {@code null} – no password-based authentication. */
    @Override
    public String getPassword() {
        return null;
    }

    /**
     * Returns {@code [ROLE_core-rulebook]}.
     *
     * <p>{@link de.paladinsinn.torg.codex.markup.SecuredMarkupService} reads these
     * authorities to determine which product-gated markup blocks are visible to the user.</p>
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return AUTHORITIES;
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }

    // -------------------------------------------------------------------------
    // Torg Codex helpers
    // -------------------------------------------------------------------------

    /**
     * Returns a read-only list containing only {@value #FREE_PRODUCT_ID}.
     *
     * @return unmodifiable list of owned codex ids for anonymous users
     */
    public List<String> getOwnedCodexIds() {
        return OWNED_CODEX_IDS;
    }

    @Override
    public String toString() {
        return "NotLoggedInUserDetails{username='" + USERNAME + "', ownedCodexIds=" + OWNED_CODEX_IDS + "}";
    }
}

