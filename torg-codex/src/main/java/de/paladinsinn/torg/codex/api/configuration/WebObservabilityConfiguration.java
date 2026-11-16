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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
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

package de.paladinsinn.torg.codex.api.configuration;

import de.paladinsinn.torg.codex.api.observability.UserRequestLoggingFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumSet;

/**
 * Registers {@link UserRequestLoggingFilter} in the servlet filter chain after
 * {@code ServerHttpObservationFilter} ({@code HIGHEST_PRECEDENCE + 1}) and before Spring Security's
 * {@code FilterChainProxy} ({@code -100}) so trace/span correlation is active and security-handled
 * requests are included (US4, T055, ADR-023).
 */
@Configuration
public class WebObservabilityConfiguration {

    private static final int BEFORE_SPRING_SECURITY_ORDER = -105;

    @Bean
    public UserRequestLoggingFilter userRequestLoggingFilter() {
        return new UserRequestLoggingFilter();
    }

    @Bean
    public FilterRegistrationBean<UserRequestLoggingFilter> userRequestLoggingFilterRegistration(
            final UserRequestLoggingFilter filter) {
        FilterRegistrationBean<UserRequestLoggingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(BEFORE_SPRING_SECURITY_ORDER);
        registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
        return registration;
    }
}
