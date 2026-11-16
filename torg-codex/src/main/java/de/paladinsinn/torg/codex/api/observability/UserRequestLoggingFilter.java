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

package de.paladinsinn.torg.codex.api.observability;

import de.paladinsinn.torg.codex.api.observability.UserRequestClassification.Classification;
import de.paladinsinn.torg.codex.api.observability.UserRequestClassification.RequestTier;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lifecycle-aware servlet filter emitting paired native ECS request boundary logs: {@code DEBUG}
 * start and {@code INFO} completion for user requests, {@code TRACE} start and {@code TRACE}
 * completion for background notification SSE/ack controls, and zero logs for assets or actuator
 * probes (US4, T054, FR-015, FR-016, SC-006, ADR-017, ADR-022, ADR-023).
 */
@Slf4j
public class UserRequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            final @NonNull HttpServletRequest request,
            final @NonNull HttpServletResponse response,
            final @NonNull FilterChain filterChain) throws ServletException, IOException {
        Classification classification = UserRequestClassification.classify(request);
        if (classification.tier() == RequestTier.EXCLUDED) {
            filterChain.doFilter(request, response);
            return;
        }

        long startedNanos = System.nanoTime();
        String method = request.getMethod();
        logStart(classification, method);

        AtomicBoolean completedOnce = new AtomicBoolean(false);
        boolean unhandledFailure = false;
        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException | Error failure) {
            unhandledFailure = true;
            throw failure;
        } finally {
            if (!unhandledFailure && request.isAsyncStarted()) {
                registerAsyncCompletion(request, response, classification, method, startedNanos, completedOnce);
            } else if (completedOnce.compareAndSet(false, true)) {
                int finalStatus = unhandledFailure
                        ? HttpStatus.INTERNAL_SERVER_ERROR.value()
                        : response.getStatus();
                logCompletion(request, classification, method, finalStatus, startedNanos);
            }
        }
    }

    private void registerAsyncCompletion(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Classification classification,
            final String method,
            final long startedNanos,
            final AtomicBoolean completedOnce) {
        try {
            request.getAsyncContext().addListener(new AsyncListener() {
                @Override
                public void onComplete(final AsyncEvent event) {
                    completeAsync(response.getStatus());
                }

                @Override
                public void onTimeout(final AsyncEvent event) {
                    completeAsync(response.getStatus());
                }

                @Override
                public void onError(final AsyncEvent event) {
                    int status = response.getStatus();
                    completeAsync(status >= HttpStatus.BAD_REQUEST.value()
                            ? status
                            : HttpStatus.INTERNAL_SERVER_ERROR.value());
                }

                @Override
                public void onStartAsync(final AsyncEvent event) {
                    // Preserve listener across re-started async cycles if any.
                }

                private void completeAsync(final int status) {
                    if (completedOnce.compareAndSet(false, true)) {
                        logCompletion(request, classification, method, status, startedNanos);
                    }
                }
            });
        } catch (IllegalStateException asyncAlreadyCompleted) {
            if (completedOnce.compareAndSet(false, true)) {
                logCompletion(request, classification, method, response.getStatus(), startedNanos);
            }
        }
    }

    private static void logStart(final Classification classification, final String method) {
        if (classification.tier() == RequestTier.BACKGROUND_CONTROL) {
            if (!log.isTraceEnabled()) {
                return;
            }
            log.atTrace()
                    .addKeyValue("event.action", "background-request-started")
                    .addKeyValue("http.request.method", method)
                    .addKeyValue("http.route", classification.startRoute())
                    .log("User request started.");
            return;
        }

        if (!log.isDebugEnabled()) {
            return;
        }
        log.atDebug()
                .addKeyValue("event.action", "user-request-started")
                .addKeyValue("http.request.method", method)
                .addKeyValue("http.route", classification.startRoute())
                .log("User request started.");
    }

    private static void logCompletion(
            final HttpServletRequest request,
            final Classification classification,
            final String method,
            final int statusCode,
            final long startedNanos) {
        long durationMs = TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - startedNanos));
        String route = UserRequestClassification.resolveCompletionRoute(request, classification);

        if (classification.tier() == RequestTier.BACKGROUND_CONTROL) {
            if (!log.isTraceEnabled()) {
                return;
            }
            buildCompletionEvent(log.atTrace(), "background-request-completed", method, route, statusCode, durationMs)
                    .log("User request completed.");
            return;
        }

        if (!log.isInfoEnabled()) {
            return;
        }
        buildCompletionEvent(log.atInfo(), "user-request-completed", method, route, statusCode, durationMs)
                .log("User request completed.");
    }

    private static LoggingEventBuilder buildCompletionEvent(
            final LoggingEventBuilder builder,
            final String action,
            final String method,
            final String route,
            final int statusCode,
            final long durationMs) {
        return builder
                .addKeyValue("event.action", action)
                .addKeyValue("http.request.method", method)
                .addKeyValue("http.route", route)
                .addKeyValue("http.response.status_code", statusCode)
                .addKeyValue("durationMs", durationMs);
    }
}
