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
import de.paladinsinn.torg.codex.application.port.in.UserNotificationQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying shared 12-hour library cache reuse across logout/re-login and
 * stateless {@code Authorization: ApiKey} requests with zero repeated {@code /order_products}
 * calls and single ready confirmation toasts (US3, T041, FR-006, FR-010, FR-011, FR-013, SC-004).
 */
@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest
@WireMockTest
@Sql(scripts = "/sql/catalog-drm-seed.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "/sql/catalog-drm-cleanup.sql", executionPhase = AFTER_TEST_CLASS)
class CachedLibraryLoginTest {

    private static final String CACHED_USER_KEY = "aabbccddeeff00112233445566778899aabbccdd";
    private static final String AYSLE_SPELL_UUID = "d7a00000-000b-4000-8000-000000000002";
    private static final long CACHE_UNLOCK_DEADLINE_NANOS = Duration.ofSeconds(1).toNanos();
    private static final long SYNC_WAIT_NANOS = Duration.ofSeconds(5).toNanos();
    private static final long PARK_NANOS = Duration.ofMillis(10).toNanos();

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DriveThruProperties driveThruProperties;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private UserNotificationQuery notificationQuery;

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
        Cache libCache = cacheManager.getCache(DriveThruLibraryCacheConfig.LIBRARY_CACHE);
        if (libCache != null) {
            libCache.clear();
        }
    }

    @Test
    void reusesCachedLibraryAcrossLogoutReLoginAndStatelessHeaderCallsWithZeroLibraryRequests()
            throws Exception {
        WireMock.resetAllRequests();

        // 1. Cold sign-in loads library from DriveThruRPG once and caches snapshot
        MvcResult firstLogin = mockMvc.perform(post("/login").param("apiKey", CACHED_USER_KEY).with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"))
                .andReturn();
        MockHttpSession firstSession = (MockHttpSession) firstLogin.getRequest().getSession(false);
        assertThat(firstSession).isNotNull();
        awaitSyncStatus(firstSession, SyncStatus.COMPLETED);
        WireMock.verify(1, getRequestedFor(urlPathEqualTo("/api/vBeta/order_products")));

        // 2. Sign out revokes browser entitlements immediately WITHOUT evicting drivethru.library cache
        mockMvc.perform(post("/logout").session(firstSession).with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?logout"));
        assertThat(firstSession.isInvalid()).isTrue();
        mockMvc.perform(get("/spells/drm-spell-aysle"))
                .andExpect(status().isForbidden());

        // 3. Re-sign-in with same API key validates token freshly, issues ZERO /order_products calls,
        // unlocks publications < 1s, and emits ONLY the single "Publications Ready" toast!
        WireMock.resetAllRequests();
        long reloginStart = System.nanoTime();
        MvcResult secondLogin = mockMvc.perform(post("/login").param("apiKey", CACHED_USER_KEY).with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"))
                .andReturn();
        MockHttpSession secondSession = (MockHttpSession) secondLogin.getRequest().getSession(false);
        assertThat(secondSession).isNotNull();

        mockMvc.perform(get("/spells/drm-spell-aysle").session(secondSession))
                .andExpect(status().isOk());
        assertThat(System.nanoTime() - reloginStart)
                .as("SC-004: Cached publications must unlock within 1s of token validation")
                .isLessThan(CACHE_UNLOCK_DEADLINE_NANOS);

        WireMock.verify(1, postRequestedFor(urlPathEqualTo("/api/vBeta/auth_key")));
        WireMock.verify(0, getRequestedFor(urlPathEqualTo("/api/vBeta/order_products")));

        String channelId = (String) secondSession.getAttribute(
                ApiKeySessionAuthentication.NOTIFICATION_CHANNEL_ATTR);
        List<UserNotification> toasts = notificationQuery.pending(channelId);
        assertThat(toasts).singleElement().satisfies(toast -> {
            assertThat(toast.title()).isEqualTo("Publications Ready");
            assertThat(toast.message()).doesNotContain("several minutes");
        });

        // 4. Stateless REST header calls also hit the shared cache with zero /order_products requests
        WireMock.resetAllRequests();
        MvcResult statelessOne = mockMvc.perform(get("/api/spells/" + AYSLE_SPELL_UUID)
                        .header(HttpHeaders.AUTHORIZATION, "ApiKey " + CACHED_USER_KEY))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult statelessTwo = mockMvc.perform(get("/api/spells/" + AYSLE_SPELL_UUID)
                        .header(HttpHeaders.AUTHORIZATION, "ApiKey " + CACHED_USER_KEY))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(statelessOne.getRequest().getSession(false)).isNull();
        assertThat(statelessTwo.getRequest().getSession(false)).isNull();
        WireMock.verify(2, postRequestedFor(urlPathEqualTo("/api/vBeta/auth_key")));
        WireMock.verify(0, getRequestedFor(urlPathEqualTo("/api/vBeta/order_products")));

        // 5. If the key is later revoked upstream, fresh token validation still rejects sign-in!
        WireMock.stubFor(WireMock.post(urlPathEqualTo("/api/vBeta/auth_key")).atPriority(1)
                .withQueryParam("applicationKey", equalTo(CACHED_USER_KEY))
                .willReturn(aResponse().withStatus(401)));
        mockMvc.perform(post("/login").param("apiKey", CACHED_USER_KEY).with(csrf()))
                .andExpect(status().isUnauthorized());
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
