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
import de.paladinsinn.drivethru.ApiKeyFingerprint;
import de.paladinsinn.drivethru.DriveThruLibraryCacheConfig;
import de.paladinsinn.drivethru.DriveThruLibraryCacheService;
import de.paladinsinn.drivethru.client.DriveThruProperties;
import de.paladinsinn.security.ApiKeySessionAuthentication;
import de.paladinsinn.security.DriveThruSessionSyncState;
import de.paladinsinn.security.SyncStatus;
import de.paladinsinn.torg.codex.application.port.in.NotificationActionKind;
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
import java.util.Set;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@code POST /library/reload} covering CSRF/CORS protection, anonymous
 * redirects, safe {@code Referer} handling, htmx fragment responses, cache replacement with newly
 * purchased books, old-cache preservation on failure, and refresh-only expired-token handling
 * without key re-exchange (US3, T042, FR-012, FR-018, FR-019, ADR-023).
 */
@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest
@WireMockTest
@Sql(scripts = "/sql/catalog-drm-seed.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "/sql/catalog-drm-cleanup.sql", executionPhase = AFTER_TEST_CLASS)
class LibraryReloadTest {

    private static final String RELOAD_KEY = "1234567890abcdef1234567890abcdef12345678";
    private static final int PRODUCT_CORE = 216248;
    private static final int PRODUCT_AYSLE = 284210;
    private static final int PRODUCT_LIVING_LAND = 247246;
    private static final long SYNC_WAIT_NANOS = Duration.ofSeconds(5).toNanos();
    private static final long PARK_NANOS = Duration.ofMillis(10).toNanos();

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DriveThruProperties driveThruProperties;

    @Autowired
    private DriveThruLibraryCacheService cacheService;

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
    void enforcesCsrfCorsAnonymousRedirectsSafeReferersAndIncrementalCacheReplacement() throws Exception {
        // Missing CSRF or disallowed Origin -> 403 before controller dispatch
        mockMvc.perform(post("/library/reload"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/library/reload")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        // Anonymous with valid CSRF -> 302 /login (or 200 HX-Redirect: /login for htmx)
        mockMvc.perform(post("/library/reload").with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(post("/library/reload")
                        .header("HX-Request", "true")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Redirect", "/login"));

        // Sign in with initial 2-book library; verify navbar renders Reload Library form
        WireMock.stubFor(WireMock.post(urlPathEqualTo("/api/vBeta/auth_key")).atPriority(1)
                .withQueryParam("applicationKey", equalTo(RELOAD_KEY))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"token\":\"initial-reload-jwt\","
                                + "\"refreshToken\":\"valid-reload-refresh\","
                                + "\"refreshTokenTTL\":4102444800}")));
        WireMock.stubFor(WireMock.get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withHeader("Authorization", equalTo("initial-reload-jwt"))
                .willReturn(aResponse().withHeader("Content-Type", "application/vnd.api+json")
                        .withBodyFile("drivethru/owned-products-424242-response.json")));

        MvcResult loginRes = mockMvc.perform(post("/login").param("apiKey", RELOAD_KEY).with(csrf()))
                .andExpect(status().isFound())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginRes.getRequest().getSession(false);
        assertThat(session).isNotNull();
        awaitSyncStatus(session, SyncStatus.COMPLETED);

        String signedInNavbar = mockMvc.perform(get("/spells").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(signedInNavbar).contains("action=\"/library/reload\"", "Reload Library");

        ApiKeyFingerprint fingerprint = ApiKeyFingerprint.of(RELOAD_KEY);
        assertThat(cacheService.findCachedLibrary(fingerprint)).hasValueSatisfying(snap ->
                assertThat(snap.productIds()).containsExactly(PRODUCT_CORE, PRODUCT_AYSLE));

        // Simulate upstream token expiry + cookie refresh returning wiremock-reloaded-bearer-token
        // (backed by declarative drivethru-owned-products-reloaded.json with newly purchased Living Land!)
        WireMock.stubFor(WireMock.get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withHeader("Authorization", equalTo("initial-reload-jwt"))
                .willReturn(aResponse().withStatus(401)));
        WireMock.stubFor(WireMock.post(urlPathEqualTo("/api/vBeta/auth/refresh")).atPriority(1)
                .withHeader("Cookie", equalTo("refreshToken=valid-reload-refresh"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"token\":\"wiremock-reloaded-bearer-token\","
                                + "\"refreshToken\":\"rotated-reload-refresh\","
                                + "\"refreshTokenTTL\":4102444800}")));

        String channelId = (String) session.getAttribute(ApiKeySessionAuthentication.NOTIFICATION_CHANNEL_ATTR);
        notificationQuery.acknowledge(
                channelId,
                Set.copyOf(notificationQuery.pending(channelId).stream().map(UserNotification::id).toList()));

        // htmx POST /library/reload returns 200 HTML start toast fragment and updates cache on completion
        String fragmentHtml = mockMvc.perform(post("/library/reload")
                        .session(session)
                        .header("HX-Request", "true")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andReturn().getResponse().getContentAsString();
        assertThat(fragmentHtml).contains("Library Sync Started");
        awaitSyncStatus(session, SyncStatus.COMPLETED);

        assertThat(cacheService.findCachedLibrary(fingerprint)).hasValueSatisfying(snap ->
                assertThat(snap.productIds()).containsExactly(PRODUCT_CORE, PRODUCT_AYSLE, PRODUCT_LIVING_LAND));

        // Plain form POST validates same-origin Referer and rejects external Referer back to /
        mockMvc.perform(post("/library/reload")
                        .session(session)
                        .header(HttpHeaders.REFERER, "http://localhost/spells?cosm=aysle")
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/spells?cosm=aysle"));
        awaitSyncStatus(session, SyncStatus.COMPLETED);

        mockMvc.perform(post("/library/reload")
                        .session(session)
                        .header(HttpHeaders.REFERER, "https://evil.example/phish")
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));
        awaitSyncStatus(session, SyncStatus.COMPLETED);
    }

    @Test
    void unrenewableRefreshTokenDuringReloadKeepsEntitlementsAndCacheAndPromptsReSignInWithoutKeyExchange()
            throws Exception {
        WireMock.stubFor(WireMock.post(urlPathEqualTo("/api/vBeta/auth_key")).atPriority(1)
                .withQueryParam("applicationKey", equalTo(RELOAD_KEY))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"token\":\"expiring-jwt\","
                                + "\"refreshToken\":\"dead-refresh\","
                                + "\"refreshTokenTTL\":4102444800}")));
        WireMock.stubFor(WireMock.get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withHeader("Authorization", equalTo("expiring-jwt"))
                .inScenario("expire-later").whenScenarioStateIs("Started")
                .willReturn(aResponse().withHeader("Content-Type", "application/vnd.api+json")
                        .withBodyFile("drivethru/owned-products-424242-response.json"))
                .willSetStateTo("Expired"));

        MvcResult loginRes = mockMvc.perform(post("/login").param("apiKey", RELOAD_KEY).with(csrf()))
                .andExpect(status().isFound())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginRes.getRequest().getSession(false);
        assertThat(session).isNotNull();
        awaitSyncStatus(session, SyncStatus.COMPLETED);

        String channelId = (String) session.getAttribute(ApiKeySessionAuthentication.NOTIFICATION_CHANNEL_ATTR);
        notificationQuery.acknowledge(
                channelId,
                Set.copyOf(notificationQuery.pending(channelId).stream().map(UserNotification::id).toList()));

        // Now expire both access token AND refresh token upstream
        WireMock.resetAllRequests();
        WireMock.stubFor(WireMock.get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withHeader("Authorization", equalTo("expiring-jwt"))
                .willReturn(aResponse().withStatus(401)));
        WireMock.stubFor(WireMock.post(urlPathEqualTo("/api/vBeta/auth/refresh")).atPriority(1)
                .willReturn(aResponse().withStatus(401)));

        mockMvc.perform(post("/library/reload").session(session).with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));
        awaitSyncStatus(session, SyncStatus.REAUTH_REQUIRED);

        // ZERO /auth_key re-exchange calls (session holds no cleartext key!)
        WireMock.verify(0, postRequestedFor(urlPathEqualTo("/api/vBeta/auth_key")));
        WireMock.verify(1, postRequestedFor(urlPathEqualTo("/api/vBeta/auth/refresh")));

        // Existing session entitlements AND cached snapshot remain completely intact!
        mockMvc.perform(get("/spells/drm-spell-aysle").session(session))
                .andExpect(status().isOk());
        assertThat(cacheService.findCachedLibrary(ApiKeyFingerprint.of(RELOAD_KEY))).hasValueSatisfying(snap ->
                assertThat(snap.productIds()).containsExactly(PRODUCT_CORE, PRODUCT_AYSLE));

        // Sticky ERROR toast prompts user to sign in again with internal /login action
        List<UserNotification> pending = notificationQuery.pending(channelId);
        assertThat(pending).last().satisfies(reauthToast -> {
            assertThat(reauthToast.title()).isEqualTo("Sign-in Required");
            assertThat(reauthToast.action()).isNotNull();
            assertThat(reauthToast.action().kind()).isEqualTo(NotificationActionKind.NAVIGATE_URL);
            assertThat(reauthToast.action().targetPath()).isEqualTo("/login");
        });

        // Upstream 503 during reload also preserves old cached snapshot!
        WireMock.stubFor(WireMock.get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withHeader("Authorization", equalTo("expiring-jwt"))
                .willReturn(aResponse().withStatus(503)));
        mockMvc.perform(post("/library/reload").session(session).with(csrf()))
                .andExpect(status().isFound());
        awaitSyncStatus(session, SyncStatus.FAILED);
        assertThat(cacheService.findCachedLibrary(ApiKeyFingerprint.of(RELOAD_KEY))).hasValueSatisfying(snap ->
                assertThat(snap.productIds()).containsExactly(PRODUCT_CORE, PRODUCT_AYSLE));
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
