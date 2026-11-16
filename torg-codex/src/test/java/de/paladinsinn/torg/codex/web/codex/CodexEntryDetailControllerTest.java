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

import de.paladinsinn.torg.codex.api.observability.CatalogUsageObservabilityAdapter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end MockMvc tests for User Story 2 (generic single-element detail views via canonical
 * slug and UUID, prose censoring, embedded list links, publication attribution, and detail counters).
 */
@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest
@Sql(scripts = "/sql/catalog-drm-seed.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "/sql/catalog-drm-cleanup.sql", executionPhase = AFTER_TEST_CLASS)
class CodexEntryDetailControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MeterRegistry meterRegistry;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void canonicalSlugAndUuidRenderIdenticalDetailViewWithCensoredProseAndDrmGrantedCounters() throws Exception {
        double beforeGranted = counterValue(CatalogUsageObservabilityAdapter.OUTCOME_DRM_GRANTED_COUNTER);
        double beforeDetailFamily = counterValue(CatalogUsageObservabilityAdapter.FAMILY_DETAIL_COUNTER);

        String bySlugHtml = mockMvc.perform(get("/items/drm-item-core"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String byUuidHtml = mockMvc.perform(get("/items/d7a00000-0002-4000-8000-000000000001"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(bySlugHtml).isEqualTo(byUuidHtml);
        assertThat(bySlugHtml)
                .contains("DRM-item-Core", "DRM-CORE-PASSAGE", "DRM-AYSLE-TEASER", "href=\"/cosms/drm-cosm\"")
                .doesNotContain("DRM-UPSELL-PASSAGE", "DRM-AYSLE-OWNER-ONLY");

        assertThat(meterRegistry.get(CatalogUsageObservabilityAdapter.DETAIL_COUNTER)
                .tag("category", "items")
                .tag("entry", "drm-item-core")
                .tag("outcome", "drm-granted")
                .counter()
                .count()).isGreaterThanOrEqualTo(2.0);
        assertThat(counterValue(CatalogUsageObservabilityAdapter.OUTCOME_DRM_GRANTED_COUNTER))
                .isEqualTo(beforeGranted + 2.0);
        assertThat(counterValue(CatalogUsageObservabilityAdapter.FAMILY_DETAIL_COUNTER))
                .isEqualTo(beforeDetailFamily + 2.0);
    }

    @Test
    void listDetailRendersAccessibleEmbeddedEntriesAsCanonicalLinksAndPerkGroupsAliasRedirects() throws Exception {
        double beforeGrouping = counterValue(CatalogUsageObservabilityAdapter.FAMILY_GROUPING_COUNTER);

        String listHtml = mockMvc.perform(get("/spell-lists/drm-spell-list-core"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(listHtml)
                .contains("DRM-spell-list-Core", "href=\"/spells/drm-spell-core\"", ">DRM-spell-Core</a>")
                .doesNotContain("DRM-spell-Aysle", "DRM-spell-Third", "DRM-spell-Unassigned");
        assertThat(counterValue(CatalogUsageObservabilityAdapter.FAMILY_GROUPING_COUNTER))
                .isEqualTo(beforeGrouping + 1.0);

        mockMvc.perform(get("/perk-groups/drm-group-core"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/groups/drm-group-core"));
    }

    @Test
    @SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
    @Sql(statements = """
            UPDATE torg_perk SET perk_group = 'drm-group-core'
            WHERE CAST(id AS text) LIKE 'd7a00000-0005-%';
            """)
    void perkGroupDetailRendersAccessiblePerksAsCanonicalLinks() throws Exception {
        String bySlugHtml = mockMvc.perform(get("/groups/drm-group-core"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String byUuidHtml = mockMvc.perform(get("/groups/d7a00000-0006-4000-8000-000000000001"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(bySlugHtml).isEqualTo(byUuidHtml);
        assertThat(bySlugHtml)
                .contains("DRM-group-Core", ">Perks</div>", "href=\"/perks/drm-perk-core\"", ">DRM-perk-Core</a>")
                .doesNotContain("DRM-perk-Aysle", "DRM-perk-Third", "DRM-perk-Unassigned");
    }

    @Test
    void publicReferenceDetailRendersCensoredArticleAndIncrementsPublicReferenceCounters() throws Exception {
        double beforePublicRef = counterValue(CatalogUsageObservabilityAdapter.OUTCOME_PUBLIC_REF_COUNTER);

        String articleHtml = mockMvc.perform(get("/articles/drm-article"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(articleHtml)
                .contains("DRM-article", "DRM-CORE-PASSAGE", "DRM-AYSLE-TEASER")
                .doesNotContain("DRM-UPSELL-PASSAGE", "DRM-AYSLE-OWNER-ONLY");
        assertThat(counterValue(CatalogUsageObservabilityAdapter.OUTCOME_PUBLIC_REF_COUNTER))
                .isEqualTo(beforePublicRef + 1.0);
    }

    private double counterValue(final String name) {
        Counter counter = meterRegistry.find(name).counter();
        return counter == null ? 0.0 : counter.count();
    }
}
