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

package de.paladinsinn.security;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import de.paladinsinn.drivethru.ApiKeyFingerprint;
import de.paladinsinn.drivethru.client.DriveThruProperties;
import de.paladinsinn.drivethru.token.DrivethruToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying that {@code Authorization: ApiKey} header requests remain
 * session-free when stateless and still resolve complete ownership synchronously even when an
 * existing browser session is only partially synchronized (FR-013, T016, T022).
 */
@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest(classes = de.paladinsinn.torg.codex.TorgCodexApplication.class)
@WireMockTest
@Sql(scripts = "/sql/catalog-drm-seed.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "/sql/catalog-drm-cleanup.sql", executionPhase = AFTER_TEST_CLASS)
class AsyncLibraryHeaderAuthenticationTest {

    private static final String VALID_API_KEY = "aabbccddeeff00112233445566778899aabbccdd";
    private static final String AYSLE_SPELL_UUID = "d7a00000-000b-4000-8000-000000000002";

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
    void statelessApiKeyHeaderResolvesFullOwnershipWithoutAllocatingHttpSession() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/spells/" + AYSLE_SPELL_UUID)
                        .header(HttpHeaders.AUTHORIZATION, "ApiKey " + VALID_API_KEY))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
        assertThat(result.getResponse().getContentAsString()).contains("DRM-AYSLE-OWNER-ONLY");
    }

    @Test
    void headerRequestCarryingPartiallyLoadedBrowserSessionResolvesFullyWithoutClobberingNewerSession()
            throws Exception {
        MockHttpSession partialSession = new MockHttpSession();
        DrivethruToken freeTierToken = new DrivethruToken();
        freeTierToken.setCustomerId("424242");
        SecurityContext partialContext = SecurityContextHolder.createEmptyContext();
        partialContext.setAuthentication(ApiKeyAuthenticationToken.authenticated(
                new DriveThruUserDetails(freeTierToken.sessionCopy(), List.of())));
        UUID runningGen = UUID.randomUUID();
        partialSession.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, partialContext);
        partialSession.setAttribute(
                ApiKeySessionAuthentication.KEY_FINGERPRINT_ATTR, ApiKeyFingerprint.of(VALID_API_KEY));
        partialSession.setAttribute(
                DriveThruSessionSyncState.SYNC_STATE_KEY,
                DriveThruSessionSyncState.running(runningGen, Instant.now()));

        // Without header, partially loaded session still sees 403 teaser for Aysle spell
        mockMvc.perform(get("/api/spells/" + AYSLE_SPELL_UUID).session(partialSession))
                .andExpect(status().isForbidden());

        // With ApiKey header on the same partially loaded session, single-request REST call resolves fully!
        String apiJson = mockMvc.perform(get("/api/spells/" + AYSLE_SPELL_UUID)
                        .session(partialSession)
                        .header(HttpHeaders.AUTHORIZATION, "ApiKey " + VALID_API_KEY))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(apiJson).contains("DRM-AYSLE-OWNER-ONLY");

        // Running browser generation is not prematurely destroyed or replaced with a raw-key token
        DriveThruSessionSyncState stateAfter = (DriveThruSessionSyncState) partialSession.getAttribute(
                DriveThruSessionSyncState.SYNC_STATE_KEY);
        assertThat(stateAfter.syncGenerationId()).isEqualTo(runningGen);

        // Also verify a newer browser session key is not overwritten if a header call uses an older key
        String olderHeaderKey = "3322110099887766554433221100998877665544";
        WireMock.stubFor(WireMock.post(urlPathEqualTo("/api/vBeta/auth_key")).atPriority(1)
                .withQueryParam("applicationKey", equalTo(olderHeaderKey))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"token\":\"older-jwt\",\"refreshToken\":\"r\",\"refreshTokenTTL\":4102444800}")));
        WireMock.stubFor(WireMock.get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withHeader("Authorization", equalTo("older-jwt"))
                .willReturn(aResponse().withHeader("Content-Type", "application/vnd.api+json")
                        .withBody("{\"links\":{\"next\":null},\"meta\":{\"itemsPerPage\":200,\"currentPage\":1},"
                                + "\"data\":[]}")));

        mockMvc.perform(get("/api/spells")
                        .session(partialSession)
                        .header(HttpHeaders.AUTHORIZATION, "ApiKey " + olderHeaderKey))
                .andExpect(status().isOk());

        assertThat(partialSession.getAttribute(ApiKeySessionAuthentication.KEY_FINGERPRINT_ATTR))
                .isEqualTo(ApiKeyFingerprint.of(VALID_API_KEY));
    }
}
