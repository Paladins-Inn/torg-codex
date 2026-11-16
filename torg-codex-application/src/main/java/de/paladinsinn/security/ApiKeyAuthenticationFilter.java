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
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
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
 * <p>Invalid credentials return {@code 401 Unauthorized}; upstream service failures
 * return {@code 503 Service Unavailable}.</p>
 *
 * <p>When {@link #setSessionAuthentication(ApiKeySessionAuthentication)} is configured,
 * successful authentication is persisted and reused only within the HTTP session.
 * Otherwise the filter retains its stateless behavior.</p>
 */
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    /** Authorization scheme prefix expected in the {@code Authorization} header. */
    public static final String API_KEY_PREFIX = "ApiKey ";
    private final AuthenticationManager authenticationManager;
    @Setter
    private ApiKeySessionAuthentication sessionAuthentication;
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
                log.debug("No API key authentication header found. owned={}, authorities={}",
                        userDetails.ownedCodexIds(), userDetails.getAuthorities());
            }
            filterChain.doFilter(request, response);
            return;
        }

        final String apiKey = header.substring(API_KEY_PREFIX.length()).strip();
        log.debug("Received API key authentication request.");
        try {
            final Authentication auth = resolveHeaderAuthentication(apiKey, request, response);
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("API key authentication successful. user={}, owned={}, authorities={}",
                    auth.getName(),
                    ((DriveThruPublicationOwner) Objects.requireNonNull(auth.getPrincipal())).ownedCodexIds(),
                    auth.getAuthorities());
        } catch (final AuthenticationServiceException ex) {
            clearAuthentication(request, response);
            log.warn("DriveThruRPG authentication service unavailable.");
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "DriveThruRPG is temporarily unavailable.");
            return;
        } catch (final AuthenticationException ex) {
            clearAuthentication(request, response);
            log.info("API key authentication rejected.");
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid DriveThruRPG API key.");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private Authentication resolveHeaderAuthentication(
            final String apiKey,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        if (sessionAuthentication == null) {
            return authenticationManager.authenticate(ApiKeyAuthenticationToken.unauthenticated(apiKey));
        }
        if (isApiRoute(request)) {
            if (request.getSession(false) == null) {
                return authenticationManager.authenticate(ApiKeyAuthenticationToken.unauthenticated(apiKey));
            }
            return sessionAuthentication.authenticateHeaderWithExistingSession(apiKey, request);
        }
        return sessionAuthentication.authenticate(apiKey, request, response);
    }

    private static boolean isApiRoute(final HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        String contextPath = request.getContextPath();
        String path = contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)
                ? uri.substring(contextPath.length())
                : uri;
        return "/api".equals(path) || path.startsWith("/api/");
    }

    private void clearAuthentication(final HttpServletRequest request, final HttpServletResponse response) {
        if (sessionAuthentication == null) {
            SecurityContextHolder.clearContext();
        } else {
            sessionAuthentication.clear(request, response);
        }
    }
}
