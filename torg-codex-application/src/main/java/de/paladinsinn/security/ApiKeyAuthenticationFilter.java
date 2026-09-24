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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * Servlet filter that extracts a DriveThruRPG API key from the
 * {@code Authorization: ApiKey <key>} header an authenticates the request.
 *
 * <p>If no {@code ApiKey} header is present and no authentication has already
 * been restored, the filter installs {@link NotLoggedInUserDetails} as the
 * anonymous principal before continuing the filter chain.</p>
 *
 * <p>On authentication failure a {@code 401 Unauthorized} response is returned
 * immediately.</p>
 */
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    /** Authorization scheme prefix expected in the {@code Authorization} header. */
    public static final String API_KEY_PREFIX = "ApiKey ";
    private final AuthenticationManager authenticationManager;
    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final @NonNull HttpServletResponse response,
            final @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(API_KEY_PREFIX)) {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                final NotLoggedInUserDetails userDetails = NotLoggedInUserDetails.INSTANCE;
                final Authentication authentication = new AnonymousAuthenticationToken(
                        getClass().getName(),
                        userDetails,
                        userDetails.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("No API key authentication header found. owned={}, authorities={}", userDetails.ownedCodexIds(), userDetails.getAuthorities());
            }
            filterChain.doFilter(request, response);
            return;
        }

        final String apiKey = header.substring(API_KEY_PREFIX.length()).strip();
        log.debug("Received API key authentication request.");
        try {
            final Authentication auth = authenticationManager.authenticate(
                    ApiKeyAuthenticationToken.unauthenticated(apiKey)
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("API key authentication successful. user={}, owned={}, authorities={}", auth.getName(), ((DriveThruPublicationOwner) Objects.requireNonNull(auth.getPrincipal())).ownedCodexIds(), auth.getAuthorities());

            filterChain.doFilter(request, response);
        } catch (final AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            log.warn("API key authentication failed: {}", ex.getMessage());
            response.sendError(HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        }
    }
}
