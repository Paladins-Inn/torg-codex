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

import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for publication detail content tiles (US1) across anonymous, non-owner,
 * other-publication owner, and displayed-publication owner states.
 */
@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest
@Sql(scripts = "/sql/publication-content-tiles-seed.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "/sql/publication-content-tiles-cleanup.sql", executionPhase = AFTER_TEST_CLASS)
class PublicationContentTilesTest {

    private static final String PAID_A_UUID = "e9c00000-0001-4000-8000-000000000001";
    private static final String PAID_A_SLUG_PATH = "/publications/pct-paid-a";
    private static final String PAID_A_UUID_PATH = "/publications/" + PAID_A_UUID;
    private static final String EMPTY_PUB_PATH = "/publications/pct-empty-pub";
    private static final String CORE_PUB_PATH = "/publications/core-rulebook";

    private static final RequestPostProcessor NON_OWNER = user("pct-non-owner")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_DRIVETHRU_USER")));
    private static final RequestPostProcessor PAID_B_OWNER = user("pct-paid-b-owner")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_pct-paid-b")));
    private static final RequestPostProcessor PAID_A_OWNER = user("pct-paid-a-owner")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_pct-paid-a")));

    private static final Pattern CONTENT_SECTION_PATTERN = Pattern.compile(
            "<section[^>]*aria-labelledby=\"publication-content-heading\"[^>]*>(.*?)</section>",
            Pattern.DOTALL);
    private static final Pattern TILE_TITLE_PATTERN = Pattern.compile(
            "<h3\\b[^>]*class=\"h6 card-title[^\"]*\"[^>]*>\\s*<i class=\"bi ([^\"]+)\"[^>]*></i>(.*?)</h3>",
            Pattern.DOTALL);
    private static final Pattern TILE_BADGE_PATTERN = Pattern.compile(
            "<span class=\"badge rounded-pill[^\"]*\">(\\d+)</span>");
    private static final Pattern TILE_LINK_PATTERN = Pattern.compile(
            "<a\\b[^>]*href=\"([^\"]+)\"[^>]*class=\"stretched-link\"[^>]*aria-label=\"([^\"]+)\"[^>]*>");

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void paidPublicationShowsOnlyPositiveAccessibleTilesAndOmitsGridWhenAllZero() throws Exception {
        String anonHtml = mockMvc.perform(get(PAID_A_SLUG_PATH))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(extractContentSection(anonHtml)).isNull();

        String nonOwnerHtml = mockMvc.perform(get(PAID_A_SLUG_PATH).with(NON_OWNER))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(extractContentSection(nonOwnerHtml)).isNull();

        String paidBOwnerHtml = mockMvc.perform(get(PAID_A_SLUG_PATH).with(PAID_B_OWNER))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String paidBSection = extractContentSection(paidBOwnerHtml);
        assertThat(paidBSection).isNotNull();
        assertThat(paidBSection).contains("Content in this publication");
        assertThat(extractTileTitles(paidBSection)).containsExactly("Spells");
        assertThat(extractTileCounts(paidBSection)).containsExactly(1);

        String paidAOwnerHtml = mockMvc.perform(get(PAID_A_SLUG_PATH).with(PAID_A_OWNER))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String paidASection = extractContentSection(paidAOwnerHtml);
        assertThat(paidASection).isNotNull();
        assertThat(extractTileTitles(paidASection)).containsExactly("Spells", "Items &amp; Gear");
        assertThat(extractTileCounts(paidASection)).containsExactly(2, 1);
        assertThat(paidASection).contains(CatalogArea.SPELL.icon(), CatalogArea.ITEM.icon());
        assertThat(paidASection).doesNotContain(
                CatalogArea.PUBLICATION.displayName(),
                CatalogArea.ARTICLE.displayName(),
                CatalogArea.TAG.displayName());

        String uuidRouteHtml = mockMvc.perform(get(PAID_A_UUID_PATH).with(PAID_A_OWNER))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String uuidSection = extractContentSection(uuidRouteHtml);
        assertThat(uuidSection).isNotNull();
        assertThat(extractTileTitles(uuidSection)).containsExactly("Spells", "Items &amp; Gear");
        assertThat(extractTileCounts(uuidSection)).containsExactly(2, 1);
    }

    @Test
    void freeTierPublicationSignedOutShowsPositiveTilesWithoutLinksOrKeyboardActivation() throws Exception {
        String coreHtml = mockMvc.perform(get(CORE_PUB_PATH))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String section = extractContentSection(coreHtml);
        assertThat(section).isNotNull();
        assertThat(extractTileTitles(section)).contains("Perks", "Spells");
        assertThat(extractTileCounts(section)).allMatch(count -> count > 0);
        assertThat(section)
                .doesNotContain("<a ")
                .doesNotContain("href=")
                .doesNotContain("role=\"link\"")
                .doesNotContain("tabindex=")
                .doesNotContain("onclick=");
    }

    @Test
    void knownEmptyPublicationKeepsDetailsWithoutGridAndMissingPublicationReturns404() throws Exception {
        String emptyHtml = mockMvc.perform(get(EMPTY_PUB_PATH).with(PAID_A_OWNER))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(emptyHtml).contains("PCT Empty Publication");
        assertThat(extractContentSection(emptyHtml)).isNull();

        mockMvc.perform(get("/publications/pct-does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void signedInTilesLinkToFilteredListsMatchingTileCountsAndRevisitsEnforceCurrentReaderAccess()
            throws Exception {
        String paidAHtml = mockMvc.perform(get(PAID_A_SLUG_PATH).with(PAID_A_OWNER))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String section = extractContentSection(paidAHtml);
        assertThat(section).isNotNull();

        List<String> hrefs = extractTileHrefs(section);
        List<Integer> counts = extractTileCounts(section);
        assertThat(hrefs).containsExactly(
                "/spells?publication=" + PAID_A_UUID,
                "/items?publication=" + PAID_A_UUID);

        String spellsOwnerHtml = mockMvc.perform(get(hrefs.get(0)).with(PAID_A_OWNER))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(spellsOwnerHtml)
                .contains("Showing <strong>" + counts.get(0) + "</strong> of <strong>" + counts.get(0) + "</strong>")
                .contains(">PCT Paid A Only Spell</a>")
                .contains(">PCT Shared Spell</a>")
                .doesNotContain(">PCT Free Core Spell</a>", ">PCT Unassigned Spell</a>", ">PCT Unrelated Spell</a>");

        // Shared entry visible through ownership of another publication
        String paidBOnPaidAHtml = mockMvc.perform(get(hrefs.get(0)).with(PAID_B_OWNER))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(paidBOnPaidAHtml)
                .contains("Showing <strong>1</strong> of <strong>1</strong>")
                .contains(">PCT Shared Spell</a>")
                .doesNotContain(">PCT Paid A Only Spell</a>");

        // Revisit same URL with weaker grants (anonymous): no locked rows, normal empty state
        String anonListHtml = mockMvc.perform(get(hrefs.get(0)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(anonListHtml)
                .contains("Publication: <strong>PCT Paid Sourcebook A</strong>")
                .contains("Showing <strong>0</strong> of <strong>0</strong>")
                .doesNotContain(">PCT Paid A Only Spell</a>", ">PCT Shared Spell</a>");
    }

    private static List<String> extractTileHrefs(final String sectionHtml) {
        Matcher m = TILE_LINK_PATTERN.matcher(sectionHtml);
        List<String> hrefs = new java.util.ArrayList<>();
        while (m.find()) {
            hrefs.add(m.group(1));
        }
        return hrefs;
    }

    private static String extractContentSection(final String html) {
        Matcher m = CONTENT_SECTION_PATTERN.matcher(html);
        return m.find() ? m.group(1) : null;
    }

    private static List<String> extractTileTitles(final String sectionHtml) {
        Matcher m = TILE_TITLE_PATTERN.matcher(sectionHtml);
        List<String> titles = new java.util.ArrayList<>();
        while (m.find()) {
            titles.add(m.group(2).trim());
        }
        return titles;
    }

    private static List<Integer> extractTileCounts(final String sectionHtml) {
        Matcher m = TILE_BADGE_PATTERN.matcher(sectionHtml);
        List<Integer> counts = new java.util.ArrayList<>();
        while (m.find()) {
            counts.add(Integer.parseInt(m.group(1)));
        }
        return counts;
    }
}
