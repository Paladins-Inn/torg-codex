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
import org.springframework.security.authentication.AbstractAuthenticationToken;
/**
 * Spring Security token for DriveThruRPG API-key based authentication.
 * Unauthenticated: principal = raw API key (String).
 * Authenticated: principal = DriveThruUserDetails, credentials cleared.
 */
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {
    private final Object principal;
    private Object credentials;
    /**
     * Creates an unauthenticated token carrying a raw API key.
     *
     * @param apiKey the raw API key
     * @return the unauthenticated token
     */
    public static ApiKeyAuthenticationToken unauthenticated(final String apiKey) {
        return new ApiKeyAuthenticationToken(apiKey, apiKey);
    }
    /**
     * Creates an authenticated token for a DriveThruRPG user.
     *
     * @param userDetails the authenticated user
     * @return the authenticated token
     */
    public static ApiKeyAuthenticationToken authenticated(final DriveThruUserDetails userDetails) {
        final ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(userDetails, null);
        token.setAuthenticated(true);
        return token;
    }
    private ApiKeyAuthenticationToken(final Object principal, final Object credentials) {
        super(principal instanceof DriveThruUserDetails ud ? ud.getAuthorities() : null);
        this.principal   = principal;
        this.credentials = credentials;
    }
    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }
    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        credentials = null;
    }
}
