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

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import de.paladinsinn.drivethru.ApiKeyFingerprint;
import de.paladinsinn.drivethru.client.DriveThruProperties;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end cardinality and data-protection verification across request boundary ECS logs and
 * {@code torg.codex.web.page} meters using injected credential/session/slug sentinels (US4, T053,
 * FR-015, FR-016, SC-005, SC-006, ADR-022).
 */
@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest
@WireMockTest
@ExtendWith(OutputCaptureExtension.class)
@Sql(scripts = "/sql/catalog-drm-seed.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "/sql/catalog-drm-cleanup.sql", executionPhase = AFTER_TEST_CLASS)
class UserRequestTelemetryTest {

    private static final String SENTINEL_BAD_KEY = "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef";
    private static final String SENTINEL_VALID_KEY = "aabbccddeeff00112233445566778899aabbccdd";
    private static final String SENTINEL_QUERY = "sentinel-search-term-991122";
    private static final String SENTINEL_SLUG_ONE = "sentinel-missing-slug-alpha-001";
    private static final String SENTINEL_SLUG_TWO = "sentinel-missing-slug-bravo-002";
    private static final int MAX_PAGE_SERIES = 24;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DriveThruProperties driveThruProperties;

    @Autowired
    private MeterRegistry meterRegistry;

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
    void neverLeaksSentinelsRawPathsOrWarnErrorLogsOnDomainOutcomesAndBoundsMetricCardinality(
            final CapturedOutput output) throws Exception {
        int logStart = output.getOut().length();

        // 1. Vary slugs, UUIDs, search queries, invalid keys, CSRF rejections, teasers, and 404s
        mockMvc.perform(get("/spells").param("q", SENTINEL_QUERY))
                .andExpect(status().isOk());
        mockMvc.perform(get("/spells/" + SENTINEL_SLUG_ONE))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/spells/" + SENTINEL_SLUG_TWO))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/spells/drm-spell-aysle"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/spells/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/spells")
                        .header(HttpHeaders.AUTHORIZATION, "ApiKey " + SENTINEL_BAD_KEY))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/login")
                        .param("apiKey", SENTINEL_BAD_KEY)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/login")
                        .param("apiKey", SENTINEL_VALID_KEY))
                .andExpect(status().isForbidden());

        MockHttpSession initialSession = new MockHttpSession(null, "sentinel-session-id-before-login-998877");
        MvcResult signedIn = mockMvc.perform(post("/login")
                        .session(initialSession)
                        .param("apiKey", SENTINEL_VALID_KEY)
                        .with(csrf()))
                .andExpect(status().isFound())
                .andReturn();
        MockHttpSession session = (MockHttpSession) signedIn.getRequest().getSession(false);
        assertThat(session).isNotNull();
        String fingerprintHex = ApiKeyFingerprint.of(SENTINEL_VALID_KEY).value();

        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().isFound());

        String emittedLogs = output.getOut().substring(logStart);
        assertThat(emittedLogs).contains("user-request-completed");

        for (String forbidden : List.of(
                SENTINEL_BAD_KEY,
                SENTINEL_VALID_KEY,
                SENTINEL_QUERY,
                SENTINEL_SLUG_ONE,
                SENTINEL_SLUG_TWO,
                "sentinel-session-id-before-login-998877",
                fingerprintHex,
                "wiremock-jwt-bearer-token-777",
                "wiremock-refresh-token",
                "ROLE_sourcebook-aysle")) {
            assertThat(emittedLogs)
                    .as("Emitted ECS logs must never contain sentinel %s", forbidden)
                    .doesNotContain(forbidden);
        }

        // Domain outcomes (401 invalid key, 403 teaser/CSRF, 404 missing slug) emit ZERO WARN/ERROR lines
        List<String> unexpectedWarnOrError = emittedLogs.lines()
                .filter(line -> (line.contains("\"level\":\"WARN\"") || line.contains("\"level\":\"ERROR\""))
                        && !line.contains("Catalog entry without product assignment hidden by DRM"))
                .toList();
        assertThat(unexpectedWarnOrError).isEmpty();

        // Metric tags are strictly bounded and free of sentinels
        List<Meter> pageMeters = List.copyOf(meterRegistry.find("torg.codex.web.page").meters());
        assertThat(pageMeters).isNotEmpty().hasSizeLessThanOrEqualTo(MAX_PAGE_SERIES);
        Set<String> serializedMeterIds = pageMeters.stream()
                .map(m -> m.getId().toString())
                .collect(Collectors.toSet());
        for (String meterRepr : serializedMeterIds) {
            assertThat(meterRepr).doesNotContain(
                    SENTINEL_BAD_KEY,
                    SENTINEL_VALID_KEY,
                    SENTINEL_QUERY,
                    SENTINEL_SLUG_ONE,
                    SENTINEL_SLUG_TWO,
                    fingerprintHex);
        }
    }
}
