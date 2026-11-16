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

package de.paladinsinn.torg.codex.security;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Records the {@code torg.codex.web.page} user-page timer for dispatched {@code POST /logout}
 * requests handled by Spring Security's {@code LogoutFilter}, using the exact same tag-key set
 * ({@code page}, {@code class}, {@code method}, {@code exception}) as {@link TimedAspect}
 * (US4, T056, FR-014, SC-005, ADR-023).
 */
@RequiredArgsConstructor
public class LogoutTimingFilter extends OncePerRequestFilter {

    public static final String PAGE_METER = "torg.codex.web.page";

    private final MeterRegistry meterRegistry;

    @Override
    protected void doFilterInternal(
            final @NonNull HttpServletRequest request,
            final @NonNull HttpServletResponse response,
            final @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!isLogoutSubmission(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        String exceptionTag = TimedAspect.DEFAULT_EXCEPTION_TAG_VALUE;
        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException | Error failure) {
            exceptionTag = failure.getClass().getSimpleName();
            throw failure;
        } finally {
            sample.stop(Timer.builder(PAGE_METER)
                    .tag("page", "logout-submit")
                    .tag("class", "LogoutFilter")
                    .tag("method", "logout")
                    .tag(TimedAspect.EXCEPTION_TAG, exceptionTag)
                    .register(meterRegistry));
        }
    }

    private static boolean isLogoutSubmission(final HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)
                ? uri.substring(contextPath.length())
                : uri;
        return "/logout".equals(path);
    }
}
