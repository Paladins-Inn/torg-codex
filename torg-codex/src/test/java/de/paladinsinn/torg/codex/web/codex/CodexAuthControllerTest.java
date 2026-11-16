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

package de.paladinsinn.torg.codex.web.codex;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import de.paladinsinn.drivethru.DriveThruLibraryCacheConfig;
import de.paladinsinn.drivethru.client.DriveThruProperties;
import de.paladinsinn.security.DriveThruSessionSyncState;
import de.paladinsinn.security.DriveThruUserDetails;
import de.paladinsinn.security.SyncStatus;
import de.paladinsinn.torg.codex.application.port.in.CatalogReferenceQuery;
import de.paladinsinn.torg.codex.application.port.in.CatalogPublicationReference;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
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
 * End-to-end WireMock authentication test for dedicated DriveThruRPG API-key login and logout
 * pages (User Story 4, FR-014, SC-006, ADR-012, ADR-022).
 *
 * <p>Uses {@link WireMockTest} (annotation-based lifecycle) together with declarative file-based
 * stubs under {@code src/test/resources/mappings} and {@code src/test/resources/__files}, while
 * sharing the cached {@link SpringBootTest} Testcontainers context.
 */
@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest
@WireMockTest
@ExtendWith(OutputCaptureExtension.class)
@Sql(scripts = "/sql/catalog-drm-seed.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "/sql/catalog-drm-cleanup.sql", executionPhase = AFTER_TEST_CLASS)
class CodexAuthControllerTest {

    private static final String VALID_API_KEY = "aabbccddeeff00112233445566778899aabbccdd";
    private static final String SLOW_LIBRARY_KEY = "9988776655443322110099887766554433221100";
    private static final String INVALID_FILE_KEY = "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef";
    private static final String INVALID_401_KEY = "1122334455667788990011223344556677889900";
    private static final int SLOW_LIBRARY_DELAY_MS = 35_000;
    private static final long LOGIN_DEADLINE_NANOS = Duration.ofSeconds(3).toNanos();
    private static final long SYNC_WAIT_NANOS = Duration.ofSeconds(5).toNanos();
    private static final long POLL_PARK_NANOS = Duration.ofMillis(10).toNanos();
    private static final Pattern CSRF_INPUT_PATTERN = Pattern.compile(
            "<input[^>]*name=\"_csrf\"[^>]*value=\"([^\"]+)\"");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DriveThruProperties driveThruProperties;

    @Autowired
    private CatalogReferenceQuery references;

    @Autowired
    private MeterRegistry meters;

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
    void dedicatedLoginAndLogoutPagesEstablishAndClearDriveThruOwnershipSession() throws Exception {
        // 1. Anonymous visitor sees reader-themed login page (no Pico CSS CDN) and 403 teaser on Aysle spell
        String loginPage = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(loginPage)
                .contains("codex-reader-theme", "DriveThruRPG", "name=\"apiKey\"")
                .doesNotContain("picocss");

        mockMvc.perform(get("/spells/drm-spell-aysle"))
                .andExpect(status().isForbidden());

        // 2. Valid API key login (backed by file-based WireMock mappings) redirects to / and unlocks Aysle asynchronously
        MvcResult loginResult = mockMvc.perform(post("/login").param("apiKey", VALID_API_KEY).with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();
        awaitSyncTerminalState(session, SyncStatus.COMPLETED);

        SecurityContext context = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
        DriveThruUserDetails user = (DriveThruUserDetails) context.getAuthentication().getPrincipal();
        assertThat(user.getUsername()).isEqualTo("424242").isNotEqualTo(VALID_API_KEY);
        assertThat(user.getCustomerId()).isEqualTo("424242");
        assertThat(user.token().getApplicationKey()).isNull();
        assertThat(meters.get("drivethru.authentication").tag("outcome", "success").timer().count()).isPositive();
        assertThat(meters.get("drivethru.http.client.requests")
                .tags("operation", "token-exchange", "outcome", "success").counter().count()).isPositive();

        String unlockedDetail = mockMvc.perform(get("/spells/drm-spell-aysle").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(unlockedDetail).contains("DRM-spell-Aysle", "DRM-AYSLE-OWNER-ONLY", "href=\"/logout\"");

        // 3. Dedicated GET /logout renders sign-out confirmation page
        String logoutPage = mockMvc.perform(get("/logout").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(logoutPage).contains("codex-reader-theme", "Sign Out", "action=\"/logout\"");

        // 4. POST /logout invalidates session and redirects to /login?logout
        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?logout"));
        assertThat(session.isInvalid()).isTrue();

        String signedOutLoginPage = mockMvc.perform(get("/login").param("logout", ""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(signedOutLoginPage).contains("signed out");
    }

    @Test
    void invalidApiKeysReturn401LoginPageLogAtInfoOnlyAndNeverLeakCredentials(final CapturedOutput output)
            throws Exception {
        WireMock.stubFor(WireMock.post(urlPathEqualTo("/api/vBeta/auth_key"))
                .withQueryParam("applicationKey", equalTo(INVALID_401_KEY))
                .willReturn(aResponse().withStatus(401).withBody("{\"status\":\"unauthorized\"}")));

        int snapshot = output.getOut().length();

        String invalidKeyHtml = mockMvc.perform(post("/login").param("apiKey", INVALID_FILE_KEY).with(csrf()))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertThat(invalidKeyHtml).contains("codex-reader-theme", "Invalid DriveThruRPG API key");

        String http401Html = mockMvc.perform(post("/login").param("apiKey", INVALID_401_KEY).with(csrf()))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertThat(http401Html).contains("codex-reader-theme", "Invalid DriveThruRPG API key");

        String recentLogs = output.getOut().substring(snapshot);
        assertThat(recentLogs).contains("user-login-rejected");
        assertThat(recentLogs).doesNotContain(INVALID_FILE_KEY, INVALID_401_KEY, VALID_API_KEY);

        List<String> warnOrErrorLines = recentLogs.lines()
                .filter(line -> line.contains("\"level\":\"WARN\"") || line.contains("\"level\":\"ERROR\""))
                .toList();
        assertThat(warnOrErrorLines).isEmpty();
    }

    @Test
    void upstreamFailureIsNotReportedAsAnInvalidKey(final CapturedOutput output) throws Exception {
        WireMock.stubFor(WireMock.post(urlPathEqualTo("/api/vBeta/auth_key")).atPriority(1)
                .willReturn(aResponse().withStatus(503).withBody(INVALID_401_KEY)));
        int snapshot = output.getOut().length();

        String html = mockMvc.perform(post("/login").param("apiKey", INVALID_401_KEY).with(csrf()))
                .andExpect(status().isServiceUnavailable())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("temporarily unavailable").doesNotContain("Invalid DriveThruRPG API key");
        assertThat(output.getOut().substring(snapshot))
                .contains("user-login-unavailable")
                .doesNotContain(INVALID_401_KEY, "invalid-api-key", "user-login-rejected");
    }

    @Test
    void apiKeyHeaderUsesTheSameReferenceProtocolAndFailureClassification() throws Exception {
        mockMvc.perform(get("/api/spells").header("Authorization", "ApiKey " + VALID_API_KEY))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/spells").header("Authorization", "ApiKey " + INVALID_FILE_KEY))
                .andExpect(status().isUnauthorized());
        WireMock.stubFor(WireMock.post(urlPathEqualTo("/api/vBeta/auth_key")).atPriority(1)
                .withQueryParam("applicationKey", equalTo(INVALID_401_KEY))
                .willReturn(aResponse().withStatus(503).withBody(INVALID_401_KEY)));
        mockMvc.perform(get("/api/spells").header("Authorization", "ApiKey " + INVALID_401_KEY))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void tokenFirstLoginRedirectsAndRendersFreeTierDestinationUnderThreeSecondsWhileLibraryRemainsPending()
            throws Exception {
        WireMock.stubFor(WireMock.post(urlPathEqualTo("/api/vBeta/auth_key")).atPriority(1)
                .withQueryParam("applicationKey", equalTo(SLOW_LIBRARY_KEY))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"token\":\"slow-library-bearer\",\"refreshToken\":\"slow-refresh\","
                                + "\"refreshTokenTTL\":4102444800}")));
        WireMock.stubFor(WireMock.get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withHeader("Authorization", equalTo("slow-library-bearer"))
                .willReturn(aResponse()
                        .withFixedDelay(SLOW_LIBRARY_DELAY_MS)
                        .withHeader("Content-Type", "application/vnd.api+json")
                        .withBodyFile("drivethru/owned-products-424242-response.json")));

        MvcResult loginView = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginView.getRequest().getSession(false);
        assertThat(session).isNotNull();
        String anonymousSessionId = session.getId();
        Matcher csrfMatcher = CSRF_INPUT_PATTERN.matcher(loginView.getResponse().getContentAsString());
        assertThat(csrfMatcher.find()).isTrue();
        String formCsrf = csrfMatcher.group(1);

        long startNanos = System.nanoTime();
        mockMvc.perform(post("/login")
                        .session(session)
                        .param("apiKey", SLOW_LIBRARY_KEY)
                        .param("_csrf", formCsrf))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));
        String homeHtml = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String coreSpellHtml = mockMvc.perform(get("/spells/drm-spell-core").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long elapsedNanos = System.nanoTime() - startNanos;

        assertThat(elapsedNanos)
                .as("SC-001: Login + destination render must finish under 3s while library takes 35s")
                .isLessThan(LOGIN_DEADLINE_NANOS);
        assertThat(session.getId()).isNotEqualTo(anonymousSessionId);
        assertThat(homeHtml).contains("DriveThruRPG Connected");
        assertThat(coreSpellHtml).contains("DRM-CORE-PASSAGE", "DRM-AYSLE-TEASER");

        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().isFound());
    }

    @Test
    void tokenFailuresStartZeroBackgroundLibraryRequestsWhileBackgroundLibraryFailureKeepsFreeTierSession()
            throws Exception {
        WireMock.resetAllRequests();
        WireMock.stubFor(WireMock.post(urlPathEqualTo("/api/vBeta/auth_key")).atPriority(1)
                .withQueryParam("applicationKey", equalTo(INVALID_401_KEY))
                .willReturn(aResponse().withStatus(503)));

        MvcResult tokenOutage = mockMvc.perform(post("/login").param("apiKey", INVALID_401_KEY).with(csrf()))
                .andExpect(status().isServiceUnavailable())
                .andReturn();
        MockHttpSession outageSession = (MockHttpSession) tokenOutage.getRequest().getSession(false);
        assertThat(outageSession == null ? null : outageSession.getAttribute("SPRING_SECURITY_CONTEXT")).isNull();
        WireMock.verify(0, getRequestedFor(urlPathEqualTo("/api/vBeta/order_products")));

        String failingLibraryKey = "5544332211009988776655443322110055443322";
        WireMock.stubFor(WireMock.post(urlPathEqualTo("/api/vBeta/auth_key")).atPriority(1)
                .withQueryParam("applicationKey", equalTo(failingLibraryKey))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"token\":\"failing-lib-bearer\",\"refreshToken\":\"r\",\"refreshTokenTTL\":4102444800}")));
        WireMock.stubFor(WireMock.get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withHeader("Authorization", equalTo("failing-lib-bearer"))
                .willReturn(aResponse().withStatus(503)));

        MvcResult result = mockMvc.perform(post("/login").param("apiKey", failingLibraryKey).with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        awaitSyncTerminalState(session, SyncStatus.FAILED);

        mockMvc.perform(get("/spells/drm-spell-core").session(session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/spells/drm-spell-aysle").session(session))
                .andExpect(status().isForbidden());
    }

    private static void awaitSyncTerminalState(final MockHttpSession session, final SyncStatus expected) {
        long deadline = System.nanoTime() + SYNC_WAIT_NANOS;
        while (System.nanoTime() < deadline) {
            Object raw = session.getAttribute(DriveThruSessionSyncState.SYNC_STATE_KEY);
            if (raw instanceof DriveThruSessionSyncState state && state.status() == expected) {
                return;
            }
            LockSupport.parkNanos(POLL_PARK_NANOS);
        }
        assertThat(session.getAttribute(DriveThruSessionSyncState.SYNC_STATE_KEY))
                .isInstanceOfSatisfying(DriveThruSessionSyncState.class,
                        state -> assertThat(state.status()).isEqualTo(expected));
    }

    @Test
    void bulkPublicationQueryMatchesLocalisedProductsWithoutDuplicates() {
        assertThat(references.findPublicationsByProductIds(Set.of(216248, 269318, 284210)))
                .extracting(CatalogPublicationReference::codexId)
                .containsExactlyInAnyOrder("core-rulebook", "sourcebook-aysle");
        assertThat(references.findPublicationsByProductIds(Set.of(269318)))
                .extracting(CatalogPublicationReference::codexId).containsExactly("core-rulebook");
        assertThat(references.findPublicationsByProductIds(Set.of(Integer.MAX_VALUE))).isEmpty();
        assertThat(references.findPublicationsByProductIds(Set.of())).isEmpty();
    }
}
