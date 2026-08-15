package de.paladinsinn.security;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import lombok.extern.slf4j.Slf4j;
/**
 * Spring Security configuration for DriveThruRPG API-key authentication.
 *
 * <h2>Authentication flow</h2>
 * <pre>
 *   HTTP request
 *     → Authorization: ApiKey &lt;api-key&gt;
 *     → {@link ApiKeyAuthenticationFilter}
 *     → {@link DriveThruAuthenticationProvider}
 *     → DriveThruRPG /token endpoint
 *     → {@link DriveThruUserDetails} stored in SecurityContextHolder
 * </pre>
 *
 * <h2>Public endpoints</h2>
 * <ul>
 *   <li>{@code GET /actuator/health/**} – liveness / readiness probes</li>
 * </ul>
 * All other requests require a valid DriveThruRPG API key.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableCaching
@Slf4j
public class DriveThruSecurityConfig {
    /**
     * Builds the {@link AuthenticationManager} backed exclusively by the
     * {@link DriveThruAuthenticationProvider}.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            final DriveThruAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }
    /**
     * Configures the main security filter chain:
     * <ul>
     *   <li>Stateless session – no HTTP session is created</li>
     *   <li>CSRF disabled – pure REST API, no form-based flows</li>
     *   <li>API-key filter inserted before {@link UsernamePasswordAuthenticationFilter}</li>
     *   <li>Actuator health endpoints are public; everything else requires authentication</li>
     * </ul>
     *
     * <p>This chain has {@code @Order(2)} so that application modules can override it
     * with a higher-priority chain ({@code @Order(1)}).</p>
     */
    @Bean
    @Order(2)
    public SecurityFilterChain driveThruSecurityFilterChain(
            final HttpSecurity http,
            final AuthenticationManager authenticationManager) throws Exception {
        final ApiKeyAuthenticationFilter apiKeyFilter =
                new ApiKeyAuthenticationFilter(authenticationManager);
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll()
                .anyRequest().authenticated());
        log.info("DriveThruRPG API-key security filter chain configured.");
        return http.build();
    }
}
