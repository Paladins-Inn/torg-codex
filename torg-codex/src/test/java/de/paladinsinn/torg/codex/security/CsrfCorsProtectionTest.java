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

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import de.paladinsinn.drivethru.client.DriveThruProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying browser UI CSRF token lifecycle, genuine HTML form-token round trips,
 * restrictive UI CORS enforcement, stateless {@code /api/**} exemption, and session-free anonymous
 * catalog browsing (FR-019, ADR-023).
 */
@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest
@WireMockTest
@Sql(scripts = "/sql/catalog-drm-seed.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "/sql/catalog-drm-cleanup.sql", executionPhase = AFTER_TEST_CLASS)
class CsrfCorsProtectionTest {

    private static final String VALID_API_KEY = "aabbccddeeff00112233445566778899aabbccdd";
    private static final String CSRF_SESSION_ATTR =
            HttpSessionCsrfTokenRepository.class.getName() + ".CSRF_TOKEN";
    private static final Pattern CSRF_INPUT_PATTERN = Pattern.compile(
            "<input[^>]*name=\"_csrf\"[^>]*value=\"([^\"]+)\"");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DriveThruProperties driveThruProperties;

    @Autowired
    private WebCorsProperties webCorsProperties;

    private String originalBaseUrl;
    private List<String> originalAllowedOrigins;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(final WireMockRuntimeInfo wmRuntimeInfo) {
        originalBaseUrl = driveThruProperties.getBaseUrl();
        originalAllowedOrigins = List.copyOf(webCorsProperties.getAllowedOrigins());
        driveThruProperties.setBaseUrl(wmRuntimeInfo.getHttpBaseUrl() + "/api/vBeta");
        webCorsProperties.setAllowedOrigins(List.of());
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        driveThruProperties.setBaseUrl(originalBaseUrl);
        webCorsProperties.setAllowedOrigins(originalAllowedOrigins);
    }

    @Test
    void rejectsMissingOrInvalidCsrfTokensWithoutMutatingSessionState() throws Exception {
        MvcResult missingLogin = mockMvc.perform(post("/login").param("apiKey", VALID_API_KEY))
                .andExpect(status().isForbidden())
                .andReturn();
        MockHttpSession missingSession = (MockHttpSession) missingLogin.getRequest().getSession(false);
        assertThat(missingSession == null ? null : missingSession.getAttribute("SPRING_SECURITY_CONTEXT")).isNull();

        MvcResult loginPage = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession preLoginSession = (MockHttpSession) loginPage.getRequest().getSession(false);
        assertThat(preLoginSession).isNotNull();

        mockMvc.perform(post("/login")
                        .session(preLoginSession)
                        .param("apiKey", VALID_API_KEY)
                        .param("_csrf", "forged-invalid-token"))
                .andExpect(status().isForbidden());
        assertThat(preLoginSession.getAttribute("SPRING_SECURITY_CONTEXT")).isNull();

        MvcResult signedIn = mockMvc.perform(post("/login")
                        .session(preLoginSession)
                        .param("apiKey", VALID_API_KEY)
                        .with(csrf()))
                .andExpect(status().isFound())
                .andReturn();
        MockHttpSession activeSession = (MockHttpSession) signedIn.getRequest().getSession(false);
        assertThat(activeSession).isNotNull();

        mockMvc.perform(post("/logout").session(activeSession))
                .andExpect(status().isForbidden());
        assertThat(activeSession.isInvalid()).isFalse();

        mockMvc.perform(post("/logout").session(activeSession).param("_csrf", "wrong-token"))
                .andExpect(status().isForbidden());
        assertThat(activeSession.isInvalid()).isFalse();
    }

    @Test
    void genuineRenderedFormTokensSucceedRotateOnLoginAndClearOnLogout() throws Exception {
        MvcResult getLogin = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) getLogin.getRequest().getSession(false);
        assertThat(session).isNotNull();
        String anonymousSessionId = session.getId();
        CsrfToken preLoginRawToken = (CsrfToken) session.getAttribute(CSRF_SESSION_ATTR);
        assertThat(preLoginRawToken).isNotNull();

        String loginHtml = getLogin.getResponse().getContentAsString();
        String renderedLoginToken = extractHiddenCsrfToken(loginHtml);
        assertThat(loginHtml).contains("<meta name=\"_csrf\"", "<meta name=\"_csrf_header\"");

        mockMvc.perform(post("/login")
                        .session(session)
                        .param("apiKey", VALID_API_KEY)
                        .param("_csrf", renderedLoginToken))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));

        assertThat(session.getId()).isNotEqualTo(anonymousSessionId);
        assertThat(session.getAttribute(CSRF_SESSION_ATTR)).isNull();

        mockMvc.perform(post("/logout")
                        .session(session)
                        .param("_csrf", renderedLoginToken))
                .andExpect(status().isForbidden());
        assertThat(session.isInvalid()).isFalse();

        MvcResult getLogout = mockMvc.perform(get("/logout").session(session))
                .andExpect(status().isOk())
                .andReturn();
        CsrfToken postLoginRawToken = (CsrfToken) session.getAttribute(CSRF_SESSION_ATTR);
        assertThat(postLoginRawToken).isNotNull();
        assertThat(postLoginRawToken.getToken()).isNotEqualTo(preLoginRawToken.getToken());
        String logoutHtml = getLogout.getResponse().getContentAsString();
        String renderedLogoutToken = extractHiddenCsrfToken(logoutHtml);

        mockMvc.perform(post("/logout")
                        .session(session)
                        .param("_csrf", renderedLogoutToken))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?logout"));
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void enforcesRestrictiveUiCorsWhileHonoringConfiguredAllowedOriginsAndLeavingApiUnchanged() throws Exception {
        mockMvc.perform(post("/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost")
                        .param("apiKey", VALID_API_KEY)
                        .with(csrf()))
                .andExpect(status().isFound());

        MvcResult crossOriginPost = mockMvc.perform(post("/login")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .param("apiKey", VALID_API_KEY)
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andReturn();
        MockHttpSession evilSession = (MockHttpSession) crossOriginPost.getRequest().getSession(false);
        assertThat(evilSession == null ? null : evilSession.getAttribute("SPRING_SECURITY_CONTEXT")).isNull();

        mockMvc.perform(options("/login")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());

        webCorsProperties.setAllowedOrigins(List.of("https://trusted.paladins-inn.example"));
        mockMvc.perform(options("/login")
                        .header(HttpHeaders.ORIGIN, "https://trusted.paladins-inn.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://trusted.paladins-inn.example"));

        mockMvc.perform(options("/login")
                        .header(HttpHeaders.ORIGIN, "https://untrusted.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/spells")
                        .header(HttpHeaders.AUTHORIZATION, "ApiKey " + VALID_API_KEY)
                        .header(HttpHeaders.ORIGIN, "https://external-api-consumer.example"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/spells")
                        .header(HttpHeaders.ORIGIN, "https://external-api-consumer.example"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void anonymousCatalogGetRequestsDoNotCreateHttpSessions() throws Exception {
        for (String catalogPath : List.of("/", "/spells", "/spells/drm-spell-core")) {
            MvcResult result = mockMvc.perform(get(catalogPath))
                    .andExpect(status().isOk())
                    .andReturn();
            assertThat(result.getRequest().getSession(false))
                    .as("Anonymous GET %s must not allocate an HttpSession", catalogPath)
                    .isNull();
            assertThat(result.getResponse().getContentAsString())
                    .doesNotContain("name=\"_csrf\"");
        }
    }

    private static String extractHiddenCsrfToken(final String html) {
        Matcher matcher = CSRF_INPUT_PATTERN.matcher(html);
        assertThat(matcher.find()).as("Expected hidden _csrf input in HTML").isTrue();
        return matcher.group(1);
    }
}
