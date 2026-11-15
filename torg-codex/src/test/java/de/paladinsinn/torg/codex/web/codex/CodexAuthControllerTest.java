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
import de.paladinsinn.drivethru.client.DriveThruProperties;
import de.paladinsinn.security.DriveThruUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
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
    private static final String INVALID_FILE_KEY = "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef";
    private static final String INVALID_401_KEY = "1122334455667788990011223344556677889900";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DriveThruProperties driveThruProperties;

    private String originalBaseUrl;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(final WireMockRuntimeInfo wmRuntimeInfo) {
        originalBaseUrl = driveThruProperties.getBaseUrl();
        driveThruProperties.setBaseUrl(wmRuntimeInfo.getHttpBaseUrl() + "/api/vBeta");
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

        // 2. Valid API key login (backed by file-based WireMock mappings) redirects to / and unlocks Aysle
        MvcResult loginResult = mockMvc.perform(post("/login").param("apiKey", VALID_API_KEY))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();
        SecurityContext context = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
        DriveThruUserDetails user = (DriveThruUserDetails) context.getAuthentication().getPrincipal();
        assertThat(user.getUsername()).isEqualTo("424242").isNotEqualTo(VALID_API_KEY);
        assertThat(user.getCustomerId()).isEqualTo("424242");

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
        mockMvc.perform(post("/logout").session(session))
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

        String invalidKeyHtml = mockMvc.perform(post("/login").param("apiKey", INVALID_FILE_KEY))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertThat(invalidKeyHtml).contains("codex-reader-theme", "Invalid DriveThruRPG API key");

        String http401Html = mockMvc.perform(post("/login").param("apiKey", INVALID_401_KEY))
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

        String html = mockMvc.perform(post("/login").param("apiKey", INVALID_401_KEY))
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
    void libraryFailureDoesNotEstablishAPartiallyAuthenticatedSession() throws Exception {
        WireMock.stubFor(WireMock.get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .willReturn(aResponse().withStatus(503)));

        MvcResult result = mockMvc.perform(post("/login").param("apiKey", VALID_API_KEY))
                .andExpect(status().isServiceUnavailable())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session == null ? null : session.getAttribute("SPRING_SECURITY_CONTEXT")).isNull();
    }
}
