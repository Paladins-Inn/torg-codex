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

package de.paladinsinn.torg.codex.web.codex;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import de.paladinsinn.drivethru.DriveThruLibraryCacheConfig;
import de.paladinsinn.drivethru.client.DriveThruProperties;
import de.paladinsinn.security.ApiKeySessionAuthentication;
import de.paladinsinn.security.DriveThruSessionSyncState;
import de.paladinsinn.security.SyncStatus;
import de.paladinsinn.torg.codex.application.port.in.UserNotification;
import de.paladinsinn.torg.codex.application.port.in.UserNotificationCommand;
import de.paladinsinn.torg.codex.application.port.in.UserNotificationQuery;
import de.paladinsinn.torg.codex.application.port.in.UserNotificationRequested;
import de.paladinsinn.torg.codex.web.notification.WebNotificationSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for {@link CodexNotificationController} SSE streaming, CSRF/CORS-protected
 * acknowledgments, sessionless behavior, and reader login/sync lifecycle toasts (US2, FR-005–FR-009,
 * FR-019, ADR-024).
 */
@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest
@WireMockTest
@Sql(scripts = "/sql/catalog-drm-seed.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "/sql/catalog-drm-cleanup.sql", executionPhase = AFTER_TEST_CLASS)
class CodexNotificationControllerTest {

    private static final String VALID_KEY = "aabbccddeeff00112233445566778899aabbccdd";
    private static final String INVALID_KEY = "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef";
    private static final int OVER_LIMIT_IDS = 21;
    private static final long SYNC_WAIT_NANOS = Duration.ofSeconds(5).toNanos();
    private static final long PARK_NANOS = Duration.ofMillis(10).toNanos();

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DriveThruProperties driveThruProperties;

    @Autowired
    private UserNotificationCommand notificationCommand;

    @Autowired
    private UserNotificationQuery notificationQuery;

    @Autowired
    private WebNotificationSupport notificationSupport;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private CacheManager cacheManager;

    private String originalBaseUrl;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(final WireMockRuntimeInfo wmRuntimeInfo) {
        originalBaseUrl = driveThruProperties.getBaseUrl();
        driveThruProperties.setBaseUrl(wmRuntimeInfo.getHttpBaseUrl() + "/api/vBeta");
        Cache libCache = cacheManager.getCache(DriveThruLibraryCacheConfig.LIBRARY_CACHE);
        if (libCache != null) {
            libCache.clear();
        }
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        driveThruProperties.setBaseUrl(originalBaseUrl);
    }

    @Test
    void streamAndAckEndpointsEnforceSessionCsrfCorsAndBatchContracts() throws Exception {
        // Sessionless GET returns 204 without creating a session; sessionless POST returns 403
        MvcResult emptyGet = mockMvc.perform(get("/notifications/stream"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andReturn();
        assertThat(emptyGet.getRequest().getSession(false)).isNull();

        mockMvc.perform(post("/notifications/ack").param("id", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());

        // Session with channel receives SSE stream flushing pending item + live Spring Event push
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest seedReq = new MockHttpServletRequest();
        seedReq.setSession(session);
        String channelId = notificationSupport.getOrCreateChannelId(seedReq, true);

        UserNotification queued = notificationCommand.notifyInfo(
                channelId, "Initial <script>x</script>", "Safe body");

        MvcResult sseResult = mockMvc.perform(get("/notifications/stream")
                        .session(session)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(header().string("X-Accel-Buffering", "no"))
                .andReturn();

        eventPublisher.publishEvent(UserNotificationRequested.warning(
                channelId, "Live Push", "Pushed via Spring Event", null));

        String ssePayload = sseResult.getResponse().getContentAsString();
        assertThat(ssePayload)
                .contains("event:toast", queued.id().toString(), "&lt;script&gt;x&lt;/script&gt;", "Live Push")
                .doesNotContain("<script>x</script>");
        assertThat(notificationQuery.pending(channelId)).hasSize(2);

        // Forbidden Origin or missing CSRF does not mutate queue
        mockMvc.perform(post("/notifications/ack")
                        .session(session)
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .param("id", queued.id().toString())
                        .with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/notifications/ack")
                        .session(session)
                        .param("id", queued.id().toString()))
                .andExpect(status().isForbidden());
        assertThat(notificationQuery.pending(channelId)).hasSize(2);

        // Malformed UUID or > 20 IDs -> 400 Bad Request after valid CSRF
        mockMvc.perform(post("/notifications/ack")
                        .session(session)
                        .param("id", "not-a-uuid")
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        String[] tooMany = java.util.stream.IntStream.range(0, OVER_LIMIT_IDS)
                .mapToObj(i -> UUID.randomUUID().toString())
                .toArray(String[]::new);
        mockMvc.perform(post("/notifications/ack")
                        .session(session)
                        .param("id", tooMany)
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        // Valid idempotent acknowledgment removes only acknowledged UUID
        mockMvc.perform(post("/notifications/ack")
                        .session(session)
                        .param("id", queued.id().toString())
                        .with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/notifications/ack")
                        .session(session)
                        .param("id", queued.id().toString())
                        .with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(notificationQuery.pending(channelId))
                .singleElement()
                .extracting(UserNotification::title)
                .isEqualTo("Live Push");
    }

    @Test
    void loginLifecycleRendersAuthFailureToastAndUncachedSyncStartCompletionAndFailureToasts()
            throws Exception {
        String invalidLoginHtml = mockMvc.perform(post("/login")
                        .param("apiKey", INVALID_KEY)
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertThat(invalidLoginHtml)
                .contains("id=\"codex-toast-container\"", "Authentication Failed", "data-bs-autohide=\"false\"")
                .doesNotContain(INVALID_KEY);

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .param("apiKey", VALID_KEY)
                        .with(csrf()))
                .andExpect(status().isFound())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();
        awaitSyncStatus(session, SyncStatus.COMPLETED);

        String channelId = (String) session.getAttribute(ApiKeySessionAuthentication.NOTIFICATION_CHANNEL_ATTR);
        assertThat(channelId).isNotBlank();
        List<UserNotification> lifecycleToasts = notificationQuery.pending(channelId);
        assertThat(lifecycleToasts).extracting(UserNotification::title)
                .containsExactly("Library Sync Started", "Library Sync Complete");

        String homeHtml = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(homeHtml)
                .contains("id=\"codex-toast-container\"",
                        "data-stream-url=\"/notifications/stream\"",
                        "data-ack-url=\"/notifications/ack\"",
                        "Library Sync Started",
                        "Library Sync Complete",
                        "data-toast-action=\"reload\"");

        // Mid-stream library failure emits sticky ERROR toast
        String failKey = "6655443322110099887766554433221100998877";
        WireMock.stubFor(WireMock.post(urlPathEqualTo("/api/vBeta/auth_key")).atPriority(1)
                .withQueryParam("applicationKey", equalTo(failKey))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"token\":\"fail-sync-jwt\",\"refreshToken\":\"r\",\"refreshTokenTTL\":4102444800}")));
        WireMock.stubFor(WireMock.get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withHeader("Authorization", equalTo("fail-sync-jwt"))
                .willReturn(aResponse().withStatus(503)));

        MvcResult failLogin = mockMvc.perform(post("/login").param("apiKey", failKey).with(csrf()))
                .andExpect(status().isFound())
                .andReturn();
        MockHttpSession failSession = (MockHttpSession) failLogin.getRequest().getSession(false);
        assertThat(failSession).isNotNull();
        awaitSyncStatus(failSession, SyncStatus.FAILED);

        String failChannel = (String) failSession.getAttribute(
                ApiKeySessionAuthentication.NOTIFICATION_CHANNEL_ATTR);
        assertThat(notificationQuery.pending(failChannel)).extracting(UserNotification::title)
                .containsExactly("Library Sync Started", "Library Sync Failed");
    }

    private static void awaitSyncStatus(final MockHttpSession session, final SyncStatus expected) {
        long deadline = System.nanoTime() + SYNC_WAIT_NANOS;
        while (System.nanoTime() < deadline) {
            if (session.getAttribute(DriveThruSessionSyncState.SYNC_STATE_KEY)
                    instanceof DriveThruSessionSyncState state && state.status() == expected) {
                return;
            }
            LockSupport.parkNanos(PARK_NANOS);
        }
        throw new AssertionError("Timed out waiting for " + expected);
    }
}
