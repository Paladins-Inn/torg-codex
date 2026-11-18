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
import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end MockMvc tests for User Story 1 (landing overview, generic 17-category tabular views,
 * htmx partial table updates, DRM filtering, and usage counter increments).
 */
@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest
@Sql(scripts = "/sql/catalog-drm-seed.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "/sql/catalog-drm-cleanup.sql", executionPhase = AFTER_TEST_CLASS)
class CodexCategoryBrowseControllerTest {

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
    void landingPageRendersAllSeventeenCategoryLinksZeroNavbarInputsAndNoSpuriousCategoryCounterIncrements()
            throws Exception {
        double beforeFamilyCategory = counterValue(CatalogUsageObservabilityAdapter.FAMILY_CATEGORY_COUNTER);

        MockHttpServletResponse response = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andReturn()
                .getResponse();

        String html = response.getContentAsString();
        assertThat(html).contains("<!DOCTYPE html>", "Torg Codex", "href=\"/login\"");
        for (CatalogArea area : CatalogArea.values()) {
            assertThat(html).as("navbar/home link for %s", area).contains("href=\"" + area.canonicalPath() + "\"");
        }
        int navStart = html.indexOf("<nav");
        int navEnd = html.indexOf("</nav>");
        assertThat(navStart).isGreaterThanOrEqualTo(0);
        assertThat(html.substring(navStart, navEnd)).doesNotContain("<input");

        assertThat(counterValue(CatalogUsageObservabilityAdapter.FAMILY_CATEGORY_COUNTER))
                .isEqualTo(beforeFamilyCategory);
    }

    @ParameterizedTest
    @EnumSource(CatalogArea.class)
    void everyCategoryRendersTabularViewWithVaryHeaderDrmFilteringAndUsageIncrement(final CatalogArea area)
            throws Exception {
        double beforeCategory = meterRegistry.get(CatalogUsageObservabilityAdapter.CATEGORY_COUNTER)
                .tag("category", area.slug())
                .counter()
                .count();
        double beforeFamily = counterValue(CatalogUsageObservabilityAdapter.FAMILY_CATEGORY_COUNTER);

        MockHttpServletResponse response = mockMvc.perform(get(area.canonicalPath()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.VARY, "HX-Request"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andReturn()
                .getResponse();

        String html = response.getContentAsString();
        assertThat(html).contains("<!DOCTYPE html>", "<table", "id=\"catalog-results\"");

        if (area.isProductBound()) {
            String seedPrefix = "DRM-" + area.productBoundType().orElseThrow().tag() + "-";
            assertThat(html).contains(">" + seedPrefix + "Core</a>");
            assertThat(html).doesNotContain(
                    ">" + seedPrefix + "Aysle</a>",
                    ">" + seedPrefix + "Third</a>",
                    ">" + seedPrefix + "Unassigned</a>");
        }

        assertThat(meterRegistry.get(CatalogUsageObservabilityAdapter.CATEGORY_COUNTER)
                .tag("category", area.slug())
                .counter()
                .count()).isEqualTo(beforeCategory + 1.0);
        assertThat(counterValue(CatalogUsageObservabilityAdapter.FAMILY_CATEGORY_COUNTER))
                .isEqualTo(beforeFamily + 1.0);
    }

    @Test
    void perkGroupsAliasRedirectsPreservingQueryParameters() throws Exception {
        mockMvc.perform(get("/perk-groups").param("clearance", "alpha"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/groups?clearance=alpha"));
    }

    @Test
    void htmxFilterRequestReturnsFragmentOnlyAndCountsValidCosmAndClearanceFacets() throws Exception {
        double beforeCosmFamily = counterValue(CatalogUsageObservabilityAdapter.FAMILY_COSM_COUNTER);
        double beforeGroupingFamily = counterValue(CatalogUsageObservabilityAdapter.FAMILY_GROUPING_COUNTER);

        MockHttpServletResponse response = mockMvc.perform(get("/items")
                        .param("cosm", "drm-cosm")
                        .param("clearance", "alpha")
                        .param("q", "DRM-item")
                        .header("HX-Request", "true")
                        .header("HX-Target", "catalog-results"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.VARY, "HX-Request"))
                .andReturn()
                .getResponse();

        String fragment = response.getContentAsString();
        assertThat(fragment).doesNotContain("<!DOCTYPE html>", "<nav");
        assertThat(fragment).contains("<table", ">DRM-item-Core</a>");
        assertThat(fragment).doesNotContain(">DRM-item-Aysle</a>", ">DRM-item-Unassigned</a>");

        assertThat(counterValue(CatalogUsageObservabilityAdapter.FAMILY_COSM_COUNTER))
                .isEqualTo(beforeCosmFamily + 1.0);
        assertThat(counterValue(CatalogUsageObservabilityAdapter.FAMILY_GROUPING_COUNTER))
                .isEqualTo(beforeGroupingFamily + 1.0);
    }

    @Test
    void emptyFilterResultShowsResettableEmptyStateAndUnknownCategoryReturnsNotFound() throws Exception {
        String emptyFragment = mockMvc.perform(get("/perks")
                        .param("q", "zz-no-such-perk-99999")
                        .header("HX-Request", "true")
                        .header("HX-Target", "catalog-results"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(emptyFragment).contains("No matching entries", "href=\"/perks\"");

        mockMvc.perform(get("/non-existent-category"))
                .andExpect(status().isNotFound());
    }

    @Test
    @SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
    @Sql(scripts = "/sql/publication-content-tiles-seed.sql", executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/publication-content-tiles-cleanup.sql", executionPhase = AFTER_TEST_METHOD)
    void publicationFilterSupportsFullAndHtmxRequestsClearResetAliasAndInvalidInputs() throws Exception {
        String paidAUuid = "e9c00000-0001-4000-8000-000000000001";
        String emptyPubUuid = "e9c00000-0001-4000-8000-000000000003";
        RequestPostProcessor paidAOwner = user("browse-paid-a")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_pct-paid-a")));

        // Full page with known publication UUID + search + sort
        String fullHtml = mockMvc.perform(get("/spells")
                        .with(paidAOwner)
                        .param("publication", paidAUuid.toUpperCase())
                        .param("q", "Shared")
                        .param("sort", "name")
                        .param("dir", "desc"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.VARY, "HX-Request"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andReturn().getResponse().getContentAsString();
        assertThat(fullHtml)
                .contains("Publication: <strong>PCT Paid Sourcebook A</strong>")
                .contains("hx-history=\"false\"")
                .contains("<input type=\"hidden\" name=\"publication\" value=\"" + paidAUuid + "\">")
                .contains(">PCT Shared Spell</a>")
                .doesNotContain(">PCT Paid A Only Spell</a>")
                .contains("href=\"/spells?q=Shared&amp;sort=name&amp;dir=desc\"")
                .contains("href=\"/spells\"");

        // HTMX partial request preserves publication and returns fragment only
        String partialHtml = mockMvc.perform(get("/spells")
                        .with(paidAOwner)
                        .param("publication", paidAUuid)
                        .header("HX-Request", "true")
                        .header("HX-Target", "catalog-results"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.VARY, "HX-Request"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andReturn().getResponse().getContentAsString();
        assertThat(partialHtml)
                .doesNotContain("<!DOCTYPE html>", "<nav")
                .contains("Publication: <strong>PCT Paid Sourcebook A</strong>")
                .contains("Showing <strong>2</strong> of <strong>2</strong>")
                .contains("publication=" + paidAUuid);

        // Known empty publication shows normal empty state with publication still named
        String emptyHtml = mockMvc.perform(get("/spells")
                        .with(paidAOwner)
                        .param("publication", emptyPubUuid))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(emptyHtml)
                .contains("Publication: <strong>PCT Empty Publication</strong>")
                .contains("No matching entries found for the selected criteria.");

        // Unknown UUID, blank, malformed, and repeated values -> Publication unavailable, 0 matches, no raw echo
        for (String[] params : new String[][] {
                {UUID.randomUUID().toString()},
                {""},
                {"<script>bad</script>"},
                {paidAUuid, emptyPubUuid}
        }) {
            var req = get("/spells").with(paidAOwner);
            for (String p : params) {
                req.param("publication", p);
            }
            String invalidHtml = mockMvc.perform(req)
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(invalidHtml)
                    .contains("Publication unavailable")
                    .contains("No matching entries found for the selected criteria.")
                    .doesNotContain("<script>bad</script>");
        }

        // Alias redirect preserves publication param; reference routes ignore publication param
        mockMvc.perform(get("/perk-groups").param("publication", paidAUuid))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/groups?publication=" + paidAUuid));

        String refHtml = mockMvc.perform(get("/publications").param("publication", "not-a-uuid"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(refHtml).doesNotContain("Publication unavailable");
    }

    private double counterValue(final String name) {
        Counter counter = meterRegistry.find(name).counter();
        return counter == null ? 0.0 : counter.count();
    }
}
