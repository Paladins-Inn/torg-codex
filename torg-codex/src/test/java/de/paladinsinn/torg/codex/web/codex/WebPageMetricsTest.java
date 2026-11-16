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

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import de.paladinsinn.drivethru.client.DriveThruProperties;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collection;
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
 * Integration tests verifying the single {@code torg.codex.web.page} timing metric across all
 * eight user-facing reader page classifications, identical Prometheus-compatible tag keys, and
 * exclusion of notification controls, assets, API, actuator, and pre-dispatch CSRF/CORS rejections
 * (US4, T051, FR-014, SC-005, ADR-023).
 */
@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest
@WireMockTest
@Sql(scripts = "/sql/catalog-drm-seed.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "/sql/catalog-drm-cleanup.sql", executionPhase = AFTER_TEST_CLASS)
class WebPageMetricsTest {

    private static final String PAGE_METER = "torg.codex.web.page";
    private static final String VALID_KEY = "aabbccddeeff00112233445566778899aabbccdd";
    private static final Set<String> EXPECTED_TAG_KEYS = Set.of("page", "class", "method", "exception");
    private static final Set<String> EXPECTED_PAGES = Set.of(
            "home",
            "category",
            "detail",
            "login-view",
            "login-submit",
            "logout-view",
            "library-reload",
            "logout-submit");

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
    void recordsSinglePageMetricForAllEightPagesWithUniformTagKeysAndExcludesNonPageTraffic()
            throws Exception {
        long homeBefore = pageCount("home");
        mockMvc.perform(get("/")).andExpect(status().isOk());
        assertThat(pageCount("home") - homeBefore).isEqualTo(1L);

        long categoryBefore = pageCount("category");
        mockMvc.perform(get("/spells")).andExpect(status().isOk());
        mockMvc.perform(get("/spells")
                        .param("q", "DRM")
                        .header("HX-Request", "true")
                        .header("HX-Target", "catalog-results"))
                .andExpect(status().isOk());
        assertThat(pageCount("category") - categoryBefore).isEqualTo(2L);

        long detailBefore = pageCount("detail");
        mockMvc.perform(get("/spells/drm-spell-core")).andExpect(status().isOk());
        mockMvc.perform(get("/spells/drm-spell-aysle")).andExpect(status().isForbidden());
        mockMvc.perform(get("/spells/non-existent-spell-slug")).andExpect(status().isNotFound());
        assertThat(pageCount("detail") - detailBefore).isEqualTo(3L);

        long loginViewBefore = pageCount("login-view");
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        assertThat(pageCount("login-view") - loginViewBefore).isEqualTo(1L);

        long loginSubmitBefore = pageCount("login-submit");
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .param("apiKey", VALID_KEY)
                        .with(csrf()))
                .andExpect(status().isFound())
                .andReturn();
        assertThat(pageCount("login-submit") - loginSubmitBefore).isEqualTo(1L);
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        long reloadBefore = pageCount("library-reload");
        mockMvc.perform(post("/library/reload").session(session).with(csrf()))
                .andExpect(status().isFound());
        assertThat(pageCount("library-reload") - reloadBefore).isEqualTo(1L);

        long logoutViewBefore = pageCount("logout-view");
        mockMvc.perform(get("/logout").session(session)).andExpect(status().isOk());
        assertThat(pageCount("logout-view") - logoutViewBefore).isEqualTo(1L);

        // Pre-dispatch CSRF/CORS rejections must NOT record page samples (incl. logout-submit)
        long totalBeforeExcluded = totalPageSamples();
        mockMvc.perform(post("/logout").session(session)).andExpect(status().isForbidden());
        mockMvc.perform(post("/login")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .param("apiKey", VALID_KEY)
                        .with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/notifications/stream").session(session)).andExpect(status().isOk());
        mockMvc.perform(post("/notifications/ack")
                        .session(session)
                        .param("id", UUID.randomUUID().toString())
                        .with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/admin/notifications/stream")).andExpect(status().isNoContent());
        mockMvc.perform(get("/js/csrf.js")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api/articles")).andExpect(status().isOk());
        assertThat(totalPageSamples()).isEqualTo(totalBeforeExcluded);

        long logoutSubmitBefore = pageCount("logout-submit");
        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().isFound());
        assertThat(pageCount("logout-submit") - logoutSubmitBefore).isEqualTo(1L);

        // Verify no split handler meter exists and every torg.codex.web.page series shares the exact tag keys
        assertThat(meterRegistry.find("torg.codex.web.handler").timers()).isEmpty();
        Collection<Timer> pageTimers = meterRegistry.find(PAGE_METER).timers();
        assertThat(pageTimers).isNotEmpty();
        assertThat(pageTimers.stream()
                .map(timer -> timer.getId().getTag("page"))
                .collect(Collectors.toSet()))
                .containsAll(EXPECTED_PAGES);

        for (Meter meter : meterRegistry.find(PAGE_METER).meters()) {
            Set<String> tagKeys = meter.getId().getTags().stream()
                    .map(Tag::getKey)
                    .collect(Collectors.toSet());
            assertThat(tagKeys).isEqualTo(EXPECTED_TAG_KEYS);
        }
    }

    private long pageCount(final String pageValue) {
        return meterRegistry.find(PAGE_METER).tag("page", pageValue).timers().stream()
                .mapToLong(Timer::count)
                .sum();
    }

    private long totalPageSamples() {
        return meterRegistry.find(PAGE_METER).timers().stream()
                .mapToLong(Timer::count)
                .sum();
    }
}
