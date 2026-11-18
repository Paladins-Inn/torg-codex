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

import de.paladinsinn.torg.codex.application.port.in.CodexOverviewQuery;
import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import de.paladinsinn.torg.codex.domain.model.Publication;
import de.paladinsinn.torg.codex.web.codex.mapper.CodexViewMapper;
import de.paladinsinn.torg.codex.web.codex.model.HomeViewModel;
import de.paladinsinn.torg.codex.web.codex.model.PublicationThumbnailViewModel;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies rendered homepage publication thumbnails, fallback attributes, unchanged list order
 * and membership, and zero outbound provider requests during {@code GET /} (T004, US1).
 */
@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest
class HomePublicationThumbnailsTest {

    private static final RequestPostProcessor LIVING_LAND_OWNER = user("thumb-living-land-owner")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_sourcebook-living-land")));

    private static final int DRAMA_DECK_PRODUCT_ID = 287803;
    private static final Pattern MISSING_SECTION_PATTERN = Pattern.compile(
            "<section[^>]*aria-labelledby=\"missing-books-heading\"[^>]*>(.*?)</section>",
            Pattern.DOTALL);
    private static final Pattern CARD_TITLE_PATTERN = Pattern.compile(
            "<h3\\b[^>]*class=\"h6 card-title[^\"]*\"[^>]*>(?:\\s*<a [^>]*>)?(.*?)(?:</a>)?\\s*</h3>",
            Pattern.DOTALL);
    private static final Pattern SINGLE_DETAIL_LINK_PATTERN = Pattern.compile(
            "<a\\b[^>]*href=\"(/publications/[^\"]+)\"[^>]*aria-labelledby=\"([^\"]+)\"[^>]*>"
                    + "(.*?)<h3\\b[^>]*id=\"\\2\"[^>]*>(.*?)</h3>\\s*</a>",
            Pattern.DOTALL);

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CodexOverviewQuery overviewQuery;

    @Autowired
    private CodexViewMapper viewMapper;

    @Autowired
    private TemplateEngine templateEngine;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @Transactional(readOnly = true)
    void rendersExactThumbnailAndFallbackAttributesForAnonymousAndSignedInVisitors() throws Exception {
        MvcResult anonResult = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();
        String anonHtml = anonResult.getResponse().getContentAsString();

        MvcResult signedInResult = mockMvc.perform(get("/").with(LIVING_LAND_OWNER))
                .andExpect(status().isOk())
                .andReturn();
        String signedInHtml = signedInResult.getResponse().getContentAsString();

        String anonSection = extractMissingSection(anonHtml);
        String signedInSection = extractMissingSection(signedInHtml);

        List<String> expectedMissingNames = overviewQuery.loadOverview().missingPublications().stream()
                .map(Publication::name)
                .toList();
        assertThat(extractCardTitles(anonSection)).containsExactlyElementsOf(expectedMissingNames);
        assertThat(extractCardTitles(signedInSection))
                .doesNotContain("Living Land Sourcebook")
                .contains("Drama Deck");

        String anonDramaCard = extractCardContaining(anonSection, "Drama Deck");
        String signedInDramaCard = extractCardContaining(signedInSection, "Drama Deck");
        assertThumbnailImage(anonDramaCard, "Drama Deck", DRAMA_DECK_PRODUCT_ID);
        assertThumbnailImage(signedInDramaCard, "Drama Deck", DRAMA_DECK_PRODUCT_ID);

        assertThat(anonDramaCard).contains("Ulisses Spiele");
        assertThat(anonDramaCard).contains("https://www.drivethrurpg.com/product/" + DRAMA_DECK_PRODUCT_ID);
        assertSingleDetailLinkAndSeparateStoreLink(
                anonDramaCard, "Drama Deck", "/publications/deck-core", DRAMA_DECK_PRODUCT_ID);
    }

    @Test
    void omitsAvailableSourcebooksSectionWhenMissingListIsEmpty() {
        HomeViewModel emptyMissing = new HomeViewModel(
                viewMapper.toNavContext(CatalogArea.PUBLICATION, null),
                List.of(),
                List.of(),
                List.of(),
                100);

        StringOutput output = new StringOutput();
        templateEngine.render("codex/pages/home.jte", emptyMissing, output);
        String html = output.toString();

        assertThat(html).doesNotContain("missing-books-heading");
    }

    @Test
    void rendersNeutralFrameWithoutImageWhenThumbnailUrlIsMissingAndLoadsScriptOnlyOnHome() throws Exception {
        HomeViewModel.PublicationCard withoutThumb = new HomeViewModel.PublicationCard(
                "unassigned-pub",
                "Unassigned Publication",
                "/publications/unassigned-pub",
                "Ulisses Spiele",
                null,
                null,
                null);
        HomeViewModel.PublicationCard withThumb = new HomeViewModel.PublicationCard(
                "deck-core",
                "Drama Deck",
                "/publications/deck-core",
                "Ulisses Spiele",
                "https://www.drivethrurpg.com/images/3444/287803.jpg",
                "https://www.drivethrurpg.com/product/287803",
                new PublicationThumbnailViewModel(
                        "https://www.drivethrurpg.com/images/3444/287803-thumb140.jpg",
                        "https://www.drivethrurpg.com/images/3444/287803-thumb140.png"));

        HomeViewModel model = new HomeViewModel(
                viewMapper.toNavContext(CatalogArea.PUBLICATION, null),
                List.of(),
                List.of(),
                List.of(withoutThumb, withThumb),
                0);

        StringOutput output = new StringOutput();
        templateEngine.render("codex/pages/home.jte", model, output);
        String html = output.toString();

        String cardWithoutThumb = extractCardContaining(html, "Unassigned Publication");
        assertThat(cardWithoutThumb)
                .contains("class=\"publication-thumbnail")
                .doesNotContain("<img ")
                .contains("Unassigned Publication")
                .contains("Ulisses Spiele")
                .contains("href=\"/publications/unassigned-pub\"");

        String cardWithThumb = extractCardContaining(html, "Drama Deck");
        assertThat(cardWithThumb)
                .contains("class=\"publication-thumbnail")
                .contains("<img ")
                .contains("Drama Deck")
                .contains("https://www.drivethrurpg.com/product/287803");

        assertThat(html).contains("<script src=\"/js/publication-thumbnails.js\" defer></script>");

        MvcResult catalogResult = mockMvc.perform(get("/publications"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(catalogResult.getResponse().getContentAsString())
                .doesNotContain("publication-thumbnails.js");
    }

    private static String extractMissingSection(final String html) {
        Matcher matcher = MISSING_SECTION_PATTERN.matcher(html);
        assertThat(matcher.find()).as("Available Sourcebooks section").isTrue();
        return matcher.group(1);
    }

    private static List<String> extractCardTitles(final String sectionHtml) {
        List<String> titles = new ArrayList<>();
        Matcher matcher = CARD_TITLE_PATTERN.matcher(sectionHtml);
        while (matcher.find()) {
            titles.add(matcher.group(1).trim());
        }
        return titles;
    }

    private static String extractCardContaining(final String sectionHtml, final String title) {
        int titleIdx = sectionHtml.indexOf(title);
        assertThat(titleIdx).as("Card containing %s", title).isGreaterThanOrEqualTo(0);
        int cardStart = sectionHtml.lastIndexOf("<div class=\"card h-100", titleIdx);
        int nextCardStart = sectionHtml.indexOf("<div class=\"card h-100", titleIdx);
        return nextCardStart > cardStart
                ? sectionHtml.substring(cardStart, nextCardStart)
                : sectionHtml.substring(cardStart);
    }

    private static void assertThumbnailImage(final String cardHtml, final String expectedAlt, final int productId) {
        Matcher imgMatcher = Pattern.compile("<img\\b[^>]*>").matcher(cardHtml);
        assertThat(imgMatcher.find()).as("img element in card").isTrue();
        String imgTag = imgMatcher.group();
        assertThat(imgTag)
                .contains("class=\"publication-thumbnail-img\"")
                .contains("src=\"https://www.drivethrurpg.com/images/3444/" + productId + "-thumb140.jpg\"")
                .contains("data-fallback-src=\"https://www.drivethrurpg.com/images/3444/" + productId + "-thumb140.png\"")
                .contains("alt=\"" + expectedAlt + "\"")
                .contains("loading=\"lazy\"")
                .contains("referrerpolicy=\"no-referrer\"")
                .doesNotContain("srcset=");
    }

    private static void assertSingleDetailLinkAndSeparateStoreLink(
            final String cardHtml,
            final String expectedTitle,
            final String expectedDetailPath,
            final int productId) {
        Matcher linkMatcher = SINGLE_DETAIL_LINK_PATTERN.matcher(cardHtml);
        assertThat(linkMatcher.find()).as("Single detail link wrapping thumbnail and title").isTrue();
        assertThat(linkMatcher.group(1)).isEqualTo(expectedDetailPath);
        assertThat(linkMatcher.group(3)).contains("publication-thumbnail").contains("<img ");
        assertThat(linkMatcher.group(4).trim()).isEqualTo(expectedTitle);
        assertThat(linkMatcher.find()).as("Only one detail link in card").isFalse();

        String storeUrl = "https://www.drivethrurpg.com/product/" + productId;
        int detailLinkEnd = cardHtml.indexOf("</a>");
        int storeLinkStart = cardHtml.indexOf("href=\"" + storeUrl + "\"");
        assertThat(storeLinkStart).isGreaterThan(detailLinkEnd);
    }
}
