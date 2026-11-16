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

package de.paladinsinn.torg.codex.security;

import de.paladinsinn.drivethru.DriveThruRPGService;
import de.paladinsinn.security.ApiKeyAuthenticationFilter;
import de.paladinsinn.security.ApiKeySessionAuthentication;
import de.paladinsinn.security.DriveThruAuthenticationProvider;
import de.paladinsinn.security.DriveThruLibrarySynchronizer;
import de.paladinsinn.security.DriveThruUserDetails;
import de.paladinsinn.security.GenerationAwareSecurityContextRepository;
import de.paladinsinn.security.NotLoggedInUserDetails;
import de.paladinsinn.torg.codex.api.observability.UserRequestLoggingFilter;
import de.paladinsinn.torg.codex.web.notification.NotificationSessionListener;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.session.DisableEncodeUrlFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

/**
 * Web-application security configuration for Torg Codex.
 *
 * <h2>Design decisions</h2>
 * <ul>
 *   <li>All HTTP endpoints are publicly accessible – content protection happens at the
 *       markup level via {@link de.paladinsinn.torg.codex.api.security.CurrentUserCensorFactory}
 *       (which resolves ownership through
 *       {@link de.paladinsinn.torg.codex.api.security.ProductOwnershipResolver})
 *       and {@code <IF:product-id>} blocks in the templates.</li>
 *   <li>Authentication is <em>optional</em>: users may send an
 *       {@code Authorization: ApiKey <key>} header, which triggers DriveThruRPG login.
 *       When present, the owned-product authorities ({@code ROLE_<codexId>}) are added to
 *       the security context so that product-gated content becomes visible.</li>
 *   <li>Sessions are used ({@link SessionCreationPolicy#IF_REQUIRED}) so that a logged-in
 *       user does not have to re-authenticate on every request.</li>
 *   <li>This chain has {@code @Order(1)} and therefore takes precedence over the
 *       {@code DriveThruSecurityConfig} chain (order 2) that is provided by the
 *       {@code torg-codex-data} library.</li>
 * </ul>
 *
 * <h2>Authority → product-id mapping</h2>
 * At login time {@link DriveThruAuthenticationProvider}
 * resolves the owned DriveThruRPG product IDs to their {@code codexId} strings and places
 * them in {@link DriveThruUserDetails#ownedCodexIds()}.
 * {@code DriveThruUserDetails.getAuthorities()} then emits one
 * {@code ROLE_<codexId>} authority per owned publication.
 * {@link de.paladinsinn.torg.codex.api.security.ProductOwnershipResolver} strips the
 * {@code ROLE_} prefix to obtain the product-id set used for markup gating.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(WebCorsProperties.class)
@RequiredArgsConstructor
@Slf4j
public class TorgCodexSecurityConfig {

    private static final List<String> UI_CORS_METHODS = List.of("GET", "HEAD", "POST", "OPTIONS");
    private static final List<String> UI_CORS_HEADERS = List.of(
            "Content-Type", "X-CSRF-TOKEN", "HX-Request", "HX-Target", "HX-Current-URL", "Accept");

    private final ObjectProvider<NotificationSessionListener> notificationSessionListener;
    private final ObjectProvider<MeterRegistry> meterRegistry;
    private final ObjectProvider<UserRequestLoggingFilter> userRequestLoggingFilter;

    /**
     * Shared {@link HttpSessionCsrfTokenRepository} used by both the security filter chain and
     * {@link ApiKeySessionAuthentication} so anti-forgery tokens rotate on login and clear on logout.
     *
     * @return session CSRF token repository
     */
    @Bean
    public HttpSessionCsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    /**
     * Generation-aware {@link org.springframework.security.web.context.SecurityContextRepository}
     * preventing stale request saves from clobbering newer worker batches, newer logins, or logout.
     *
     * @return generation-aware context repository
     */
    @Bean
    public GenerationAwareSecurityContextRepository generationAwareSecurityContextRepository() {
        return new GenerationAwareSecurityContextRepository();
    }

    /**
     * Shares session-local API-key authentication between the login form and header filter.
     *
     * @param authenticationManager the DriveThruRPG authentication manager
     * @param csrfTokenRepository   shared session CSRF token repository
     * @param contextRepository     shared generation-aware context repository
     * @param driveThruRPGService   optional DriveThruRPG service for token-first interactive sign-in
     * @param synchronizer          optional background library synchronizer
     * @param observationRegistry   optional observation registry
     * @return the session authentication service
     */
    @Bean
    public ApiKeySessionAuthentication apiKeySessionAuthentication(
            final AuthenticationManager authenticationManager,
            final HttpSessionCsrfTokenRepository csrfTokenRepository,
            final GenerationAwareSecurityContextRepository contextRepository,
            final ObjectProvider<DriveThruRPGService> driveThruRPGService,
            final ObjectProvider<DriveThruLibrarySynchronizer> synchronizer,
            final ObjectProvider<ObservationRegistry> observationRegistry) {
        ApiKeySessionAuthentication auth = new ApiKeySessionAuthentication(
                authenticationManager, csrfTokenRepository, contextRepository);
        auth.setDriveThruRPGService(driveThruRPGService.getIfAvailable());
        auth.setSynchronizer(synchronizer.getIfAvailable());
        auth.setObservationRegistry(observationRegistry.getIfAvailable(() -> ObservationRegistry.NOOP));
        return auth;
    }

    /**
     * Restrictive CORS source enforced only on browser UI routes (excluding {@code /api/**} and
     * {@code /actuator/**}).
     *
     * @param corsProperties browser UI CORS configuration properties
     * @return dynamic UI CORS configuration source
     */
    @Bean
    public CorsConfigurationSource uiCorsConfigurationSource(final WebCorsProperties corsProperties) {
        return request -> {
            if (isNonBrowserEndpoint(request)) {
                return null;
            }
            CorsConfiguration config = new CorsConfiguration();
            List<String> allowedOrigins = corsProperties.getAllowedOrigins();
            config.setAllowedOrigins(allowedOrigins);
            config.setAllowedMethods(UI_CORS_METHODS);
            config.setAllowedHeaders(UI_CORS_HEADERS);
            if (!allowedOrigins.isEmpty()) {
                config.setAllowCredentials(true);
            }
            return config;
        };
    }

    /**
     * Builds the primary {@link SecurityFilterChain} for the Torg Codex web application.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain torgCodexSecurityFilterChain(
            final HttpSecurity http,
            final AuthenticationManager authenticationManager,
            final ApiKeySessionAuthentication sessionAuthentication,
            final HttpSessionCsrfTokenRepository csrfTokenRepository,
            final GenerationAwareSecurityContextRepository contextRepository,
            final CorsConfigurationSource uiCorsConfigurationSource) {

        final ApiKeyAuthenticationFilter apiKeyFilter =
                new ApiKeyAuthenticationFilter(authenticationManager);
        apiKeyFilter.setSessionAuthentication(sessionAuthentication);

        UserRequestLoggingFilter requestLogger = userRequestLoggingFilter.getIfAvailable();
        if (requestLogger != null) {
            http.addFilterBefore(requestLogger, DisableEncodeUrlFilter.class);
        }
        MeterRegistry registry = meterRegistry.getIfAvailable();
        if (registry != null) {
            http.addFilterBefore(new LogoutTimingFilter(registry), LogoutFilter.class);
        }

        http
            .cors(cors -> cors.configurationSource(uiCorsConfigurationSource))
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(new XorCsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers("/api/**"))
            .securityContext(sc -> sc.securityContextRepository(contextRepository))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .anonymous(anon -> anon
                .principal(NotLoggedInUserDetails.INSTANCE)
                .authorities("ROLE_" + NotLoggedInUserDetails.FREE_PRODUCT_ID))
            // Non-admin reader login (/login) is handled explicitly by CodexAuthController using
            // DriveThruRPG API keys rather than UsernamePasswordAuthenticationFilter (FR-014, D7).
            // Role-based OIDC authentication (ADR-007) is reserved for a separate /admin/** chain (FR-019).
            .logout(logout -> logout
                .logoutRequestMatcher(request ->
                    "POST".equalsIgnoreCase(request.getMethod())
                        && "/logout".equals(request.getRequestURI()))
                .addLogoutHandler((request, response, authentication) -> {
                    notificationSessionListener.ifAvailable(
                            listener -> listener.cleanupSession(request.getSession(false)));
                    contextRepository.markLoggedOut(request);
                })
                .logoutSuccessHandler((request, response, authentication) -> {
                    log.atInfo()
                        .addKeyValue("event.action", "user-logout")
                        .log("DriveThruRPG session signed out");
                    response.sendRedirect("/login?logout");
                })
                .permitAll())
            .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll()
                .anyRequest().permitAll());

        log.info("Torg Codex web security filter chain configured (order=1, UI CSRF/CORS enabled).");
        return http.build();
    }

    private static boolean isNonBrowserEndpoint(final HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)
                ? uri.substring(contextPath.length())
                : uri;
        return "/api".equals(path)
                || path.startsWith("/api/")
                || "/actuator".equals(path)
                || path.startsWith("/actuator/");
    }
}
