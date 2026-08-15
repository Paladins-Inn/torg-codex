package de.paladinsinn.torg.codex.data.security;
import de.paladinsinn.security.DriveThruSecurityConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
/**
 * Test-only security configuration that disables all HTTP security checks.
 *
 * <p>Include this by annotating the test class with
 * {@code @Import(TestSecurityConfig.class)} when you need a permissive
 * security setup (e.g. for slice tests or integration tests that call REST
 * endpoints without a real DriveThruRPG API key).</p>
 *
 * <p>The {@link DriveThruSecurityConfig}
 * remains active for production contexts.</p>
 */
@TestConfiguration
public class TestSecurityConfig {
    @Bean
    public SecurityFilterChain testSecurityFilterChain(final HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
