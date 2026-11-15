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

import de.paladinsinn.torg.codex.api.observability.CatalogAccessObservabilityAdapter;
import de.paladinsinn.torg.codex.api.observability.CatalogUsageObservabilityAdapter;
import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Differential UI DRM, purchase-teaser, fail-closed unassigned, usage roll-up counter, and ADR-022
 * logging test suite across all 14 product-bound catalog areas and embedded lists (User Story 3,
 * Constitution Principle V).
 */
@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
@Sql(scripts = "/sql/catalog-drm-seed.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "/sql/catalog-drm-cleanup.sql", executionPhase = AFTER_TEST_CLASS)
class CodexWebUiDifferentialTest {

    private static final RequestPostProcessor AYSLE_OWNER = user("ui-aysle-owner")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_sourcebook-aysle")));

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MeterRegistry meterRegistry;

    private MockMvc mockMvc;

    static Stream<CatalogArea> productBoundAreas() {
        return Arrays.stream(CatalogArea.values()).filter(CatalogArea::isProductBound);
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @ParameterizedTest
    @MethodSource("productBoundAreas")
    void enforcesDifferentialDrmInTablesPurchaseTeasersAndUnassignedFailClosedAcrossAllAreas(
            final CatalogArea area) throws Exception {
        String tag = area.productBoundType().orElseThrow().tag();
        String coreName = "DRM-" + tag + "-Core";
        String aysleName = "DRM-" + tag + "-Aysle";
        String thirdName = "DRM-" + tag + "-Third";
        String unassignedName = "DRM-" + tag + "-Unassigned";
        String aysleSlug = "drm-" + tag + "-aysle";
        String unassignedSlug = "drm-" + tag + "-unassigned";
        if (area == CatalogArea.COSM) {
            aysleSlug = "drm-cosm-aysle";
            unassignedSlug = "drm-cosm-unassigned";
        }

        // 1. Anonymous vs. Aysle Owner table listing
        String anonTable = mockMvc.perform(get(area.canonicalPath()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(anonTable).contains(">" + coreName + "</a>")
                .doesNotContain(aysleName, thirdName, ">" + unassignedName + "</a>");

        String ownerTable = mockMvc.perform(get(area.canonicalPath()).with(AYSLE_OWNER))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(ownerTable).contains(">" + coreName + "</a>", ">" + aysleName + "</a>")
                .doesNotContain(thirdName, ">" + unassignedName + "</a>");

        // 2. Anonymous 403 Purchase Teaser vs. Owner 200 Full Detail
        double beforeTeaser = counterValue(CatalogUsageObservabilityAdapter.OUTCOME_TEASER_COUNTER);
        String teaserHtml = mockMvc.perform(get(area.detailPath(aysleSlug)))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        assertThat(teaserHtml)
                .contains(aysleName, area.singularName(), "Purchase Required", "sourcebook-aysle", "drm-third-product", "href=\"/login\"")
                .doesNotContain("DRM entry text", "DRM-CORE-PASSAGE", "DRM-AYSLE-OWNER-ONLY", "DRM-AYSLE-TEASER");
        assertThat(counterValue(CatalogUsageObservabilityAdapter.OUTCOME_TEASER_COUNTER))
                .isEqualTo(beforeTeaser + 1.0);

        String ownerDetailHtml = mockMvc.perform(get(area.detailPath(aysleSlug)).with(AYSLE_OWNER))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(ownerDetailHtml)
                .contains(aysleName, "DRM-CORE-PASSAGE", "DRM-AYSLE-OWNER-ONLY")
                .doesNotContain("DRM-UPSELL-PASSAGE", "DRM-AYSLE-TEASER");

        // 3. Unassigned entry is fail-closed 404 for both anonymous and owner, never exposing name/teaser
        double beforeWithoutProducts = meterRegistry.get(CatalogAccessObservabilityAdapter.COUNTER_NAME)
                .tag("area", tag).counter().count();
        double beforeNotFound = counterValue(CatalogUsageObservabilityAdapter.OUTCOME_NOT_FOUND_COUNTER);

        String unassignedAnon = mockMvc.perform(get(area.detailPath(unassignedSlug)))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        assertThat(unassignedAnon).contains("Entry Not Found")
                .doesNotContain(unassignedName, "Purchase Required", "DRM entry text");

        mockMvc.perform(get(area.detailPath(unassignedSlug)).with(AYSLE_OWNER))
                .andExpect(status().isNotFound());

        assertThat(meterRegistry.get(CatalogAccessObservabilityAdapter.COUNTER_NAME)
                .tag("area", tag).counter().count()).isEqualTo(beforeWithoutProducts + 2.0);
        assertThat(counterValue(CatalogUsageObservabilityAdapter.OUTCOME_NOT_FOUND_COUNTER))
                .isEqualTo(beforeNotFound + 2.0);
        assertThat(meterRegistry.find(CatalogUsageObservabilityAdapter.DETAIL_COUNTER)
                .tag("entry", unassignedSlug).counter()).isNull();
    }

    @Test
    void embeddedListsShowAysleChildrenToOwnerOnlyAndTeaserAndNotFoundEmitZeroWarnOrErrorLogs(
            final CapturedOutput output) throws Exception {
        String anonList = mockMvc.perform(get("/spell-lists/drm-spell-list-core"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(anonList).contains("DRM-spell-Core").doesNotContain("DRM-spell-Aysle", "DRM-spell-Third");

        String ownerList = mockMvc.perform(get("/spell-lists/drm-spell-list-core").with(AYSLE_OWNER))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(ownerList).contains("DRM-spell-Core", "DRM-spell-Aysle")
                .doesNotContain("DRM-spell-Third", "DRM-spell-Unassigned");

        int logSnapshotLength = output.getOut().length();
        String untrustedSlug = "untrusted-random-probe-987654";

        mockMvc.perform(get("/spells/drm-spell-aysle"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/spells/" + untrustedSlug))
                .andExpect(status().isNotFound());

        // Untrusted missing slug must never create a per-entry Micrometer tag
        assertThat(meterRegistry.find(CatalogUsageObservabilityAdapter.DETAIL_COUNTER)
                .tag("entry", untrustedSlug).counter()).isNull();

        // ADR-022: 403 teaser and 404 missing-slug lookups must emit zero WARN or ERROR log lines
        String recentLogs = output.getOut().substring(logSnapshotLength);
        List<String> nonDataGapWarningsOrErrors = recentLogs.lines()
                .filter(line -> line.contains("\"level\":\"WARN\"") || line.contains("\"level\":\"ERROR\""))
                .filter(line -> !line.contains(CatalogAccessObservabilityAdapter.class.getName()))
                .toList();
        assertThat(nonDataGapWarningsOrErrors).isEmpty();
    }

    private double counterValue(final String name) {
        Counter counter = meterRegistry.find(name).counter();
        return counter == null ? 0.0 : counter.count();
    }
}
