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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Unit tests for {@link UserRequestLoggingFilter} covering DEBUG/INFO user boundaries, TRACE-only
 * notification stream/ack controls, INFO-only production mode, async single-completion lifecycle,
 * escaping-exception 500 reporting, and exclusion of assets and actuator probes (US4, T052,
 * FR-015, FR-016, SC-006, ADR-022).
 */
class UserRequestLoggingFilterTest {

    private static final int HTTP_OK = 200;
    private static final int HTTP_NO_CONTENT = 204;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_INTERNAL_ERROR = 500;

    private final UserRequestLoggingFilter filter = new UserRequestLoggingFilter();
    private final Logger logger = (Logger) LoggerFactory.getLogger(UserRequestLoggingFilter.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        originalLevel = logger.getLevel();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        logger.setLevel(originalLevel);
        appender.stop();
    }

    @Test
    void logsDebugStartAndInfoCompletionForUserRequestsAndRespectsInfoOnlyProductionDefault()
            throws Exception {
        logger.setLevel(Level.DEBUG);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/spells/drm-spell-aysle");
        req.setQueryString("q=secret-query");
        MockHttpServletResponse res = new MockHttpServletResponse();

        FilterChain chain = (request, response) -> {
            request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/{category}/{identifier}");
            res.setStatus(HTTP_FORBIDDEN);
        };
        filter.doFilter(req, res, chain);

        assertThat(appender.list).hasSize(2);
        ILoggingEvent started = appender.list.get(0);
        ILoggingEvent completed = appender.list.get(1);

        assertThat(started.getLevel()).isEqualTo(Level.DEBUG);
        assertThat(started.getFormattedMessage()).isEqualTo("User request started.");
        assertThat(started.getKeyValuePairs()).extracting(kv -> kv.key, kv -> kv.value)
                .contains(
                        tuple("event.action", "user-request-started"),
                        tuple("http.request.method", "GET"),
                        tuple("http.route", "reader-page"));

        assertThat(completed.getLevel()).isEqualTo(Level.INFO);
        assertThat(completed.getFormattedMessage()).isEqualTo("User request completed.");
        assertThat(completed.getKeyValuePairs()).extracting(kv -> kv.key, kv -> kv.value)
                .contains(
                        tuple("event.action", "user-request-completed"),
                        tuple("http.request.method", "GET"),
                        tuple("http.route", "/{category}/{identifier}"),
                        tuple("http.response.status_code", HTTP_FORBIDDEN));
        assertThat(completed.getKeyValuePairs()).extracting(kv -> kv.key).contains("durationMs");
        assertThat(started.toString() + completed.toString())
                .doesNotContain("drm-spell-aysle", "secret-query");

        // Production INFO mode omits DEBUG start and emits exactly one INFO completion
        appender.list.clear();
        logger.setLevel(Level.INFO);
        filter.doFilter(new MockHttpServletRequest("GET", "/api/spells"), new MockHttpServletResponse(),
                (r, s) -> r.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/spells"));
        assertThat(appender.list).singleElement()
                .extracting(ILoggingEvent::getLevel)
                .isEqualTo(Level.INFO);
    }

    @Test
    void logsBackgroundNotificationStreamAndAckAtTraceOnlyAndExcludesAssetsAndActuator()
            throws Exception {
        logger.setLevel(Level.DEBUG);
        for (String controlPath : List.of(
                "/notifications/stream",
                "/notifications/ack",
                "/admin/notifications/stream",
                "/admin/notifications/ack")) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", controlPath);
            MockHttpServletResponse res = new MockHttpServletResponse();
            res.setStatus(HTTP_NO_CONTENT);
            filter.doFilter(req, res, (r, s) -> { });
        }
        assertThat(appender.list)
                .as("Notification controls must emit zero DEBUG/INFO logs")
                .isEmpty();

        logger.setLevel(Level.TRACE);
        for (String excludedPath : List.of(
                "/actuator/health",
                "/actuator/prometheus",
                "/webjars/bootstrap/dist/js/bootstrap.bundle.min.js",
                "/themes/codex/codex.css",
                "/js/notifications.js",
                "/css/custom.css",
                "/images/logo.png",
                "/favicon.ico")) {
            filter.doFilter(new MockHttpServletRequest("GET", excludedPath),
                    new MockHttpServletResponse(), (r, s) -> { });
        }
        assertThat(appender.list).isEmpty();

        MockHttpServletRequest traceControl = new MockHttpServletRequest("POST", "/notifications/ack");
        MockHttpServletResponse traceRes = new MockHttpServletResponse();
        traceRes.setStatus(HTTP_NO_CONTENT);
        filter.doFilter(traceControl, traceRes, (r, s) ->
                r.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/notifications/ack"));

        assertThat(appender.list).hasSize(2);
        assertThat(appender.list).extracting(ILoggingEvent::getLevel)
                .containsExactly(Level.TRACE, Level.TRACE);
        assertThat(appender.list.get(0).getKeyValuePairs()).extracting(kv -> kv.key, kv -> kv.value)
                .contains(tuple("event.action", "background-request-started"));
        assertThat(appender.list.get(1).getKeyValuePairs()).extracting(kv -> kv.key, kv -> kv.value)
                .contains(tuple("event.action", "background-request-completed"));
    }

    @Test
    void completesOnceViaAsyncListenerAndReportsFiveHundredWhenExceptionEscapes() throws Exception {
        logger.setLevel(Level.DEBUG);
        MockHttpServletRequest asyncReq = new MockHttpServletRequest("GET", "/");
        asyncReq.setAsyncSupported(true);
        MockHttpServletResponse asyncRes = new MockHttpServletResponse();
        asyncRes.setStatus(HTTP_OK);

        filter.doFilter(asyncReq, asyncRes, (r, s) -> {
            r.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/");
            r.startAsync(r, s);
        });
        // While async is still in flight, only start has been logged
        assertThat(appender.list).hasSize(1);

        AsyncContext asyncCtx = asyncReq.getAsyncContext();
        assertThat(asyncCtx).isInstanceOf(MockAsyncContext.class);
        for (AsyncListener listener : ((MockAsyncContext) asyncCtx).getListeners()) {
            listener.onComplete(new AsyncEvent(asyncCtx, asyncReq, asyncRes));
            listener.onComplete(new AsyncEvent(asyncCtx, asyncReq, asyncRes));
        }
        assertThat(appender.list).hasSize(2);
        assertThat(appender.list.get(1).getLevel()).isEqualTo(Level.INFO);

        // Escaping exception logs completion once with status 500
        appender.list.clear();
        MockHttpServletRequest failingReq = new MockHttpServletRequest("GET", "/spells");
        MockHttpServletResponse failingRes = new MockHttpServletResponse();
        assertThatThrownBy(() -> filter.doFilter(failingReq, failingRes, (r, s) -> {
            throw new ServletException("boom");
        })).isInstanceOf(ServletException.class);

        assertThat(appender.list).hasSize(2);
        assertThat(appender.list.get(1).getKeyValuePairs()).extracting(kv -> kv.key, kv -> kv.value)
                .contains(tuple("http.response.status_code", HTTP_INTERNAL_ERROR));
    }
}
