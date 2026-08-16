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

import de.paladinsinn.security.ApiKeyAuthenticationFilter;
import de.paladinsinn.security.DriveThruAuthenticationProvider;
import de.paladinsinn.security.DriveThruUserDetails;
import de.paladinsinn.security.NotLoggedInUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
@Slf4j
public class TorgCodexSecurityConfig {

    /**
     * Builds the primary {@link SecurityFilterChain} for the Torg Codex web application.
     *
     * <p>Key settings:
     * <ul>
     *   <li>CSRF disabled – forms use simple POST without tokens; could be re-enabled if needed</li>
     *   <li>Session created on demand – the API-key filter saves the authenticated principal
     *       in the {@link org.springframework.security.core.context.SecurityContext} which is
     *       subsequently stored in the HTTP session</li>
     *   <li>All requests permitted – authorisation is at content level, not route level</li>
     *   <li>{@link ApiKeyAuthenticationFilter} inserted before basic-auth – skips the chain
     *       step when no {@code Authorization: ApiKey} header is present</li>
     * </ul>
     */
    @Bean
    @Order(1)
    public SecurityFilterChain torgCodexSecurityFilterChain(
            final HttpSecurity http,
            final AuthenticationManager authenticationManager) {

        final ApiKeyAuthenticationFilter apiKeyFilter =
                new ApiKeyAuthenticationFilter(authenticationManager);

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .anonymous(anon -> anon
                .principal(NotLoggedInUserDetails.INSTANCE)
                .authorities("ROLE_" + NotLoggedInUserDetails.FREE_PRODUCT_ID))
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll())
            .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll()
                .anyRequest().permitAll());

        log.info("Torg Codex web security filter chain configured (order=1, all requests permitted).");
        return http.build();
    }
}
