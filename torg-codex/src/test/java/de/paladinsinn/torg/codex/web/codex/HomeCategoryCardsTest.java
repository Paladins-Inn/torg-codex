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

import de.paladinsinn.torg.codex.application.port.in.CodexHomeOverview.CategorySummary;
import de.paladinsinn.torg.codex.application.port.in.CodexOverviewQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
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
 * Characterization test guarding homepage category cards (order, labels, icons, links, and
 * zero/non-zero counts) before and after extracting {@code categoryTile.jte} (T006, FR-007).
 */
@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest
class HomeCategoryCardsTest {

    private static final RequestPostProcessor SIGNED_IN_READER = user("home-card-reader")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_DRIVETHRU_USER")));

    private static final Pattern CATEGORIES_SECTION_PATTERN = Pattern.compile(
            "<section[^>]*aria-labelledby=\"categories-heading\"[^>]*>(.*?)</section>",
            Pattern.DOTALL);
    private static final Pattern CARD_PATTERN = Pattern.compile(
            "<div class=\"card h-100 shadow-sm position-relative\">(.*?)</div>\\s*</div>",
            Pattern.DOTALL);

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CodexOverviewQuery overviewQuery;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @Transactional(readOnly = true)
    void homepageCategoryCardsPreserveOrderLabelsIconsLinksAndCounts() throws Exception {
        List<CategorySummary> expected = overviewQuery.loadOverview().categories();
        assertThat(expected).hasSize(17);

        for (RequestPostProcessor auth : new RequestPostProcessor[] {null, SIGNED_IN_READER}) {
            var request = auth == null ? get("/") : get("/").with(auth);
            String html = mockMvc.perform(request)
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String section = extractCategoriesSection(html);
            List<String> cards = extractCards(section);
            assertThat(cards).hasSize(expected.size());

            for (int i = 0; i < expected.size(); i++) {
                CategorySummary summary = expected.get(i);
                String card = cards.get(i);
                String escapedDisplayName = summary.area().displayName().replace("&", "&amp;");
                assertThat(card)
                        .contains("bi " + summary.area().icon())
                        .contains(escapedDisplayName)
                        .contains(">" + summary.accessibleCount() + "</span>")
                        .contains(summary.area().description())
                        .contains("href=\"" + summary.area().canonicalPath() + "\"")
                        .contains("aria-label=\"Browse " + escapedDisplayName + "\"");
            }
        }
    }

    private static String extractCategoriesSection(final String html) {
        Matcher m = CATEGORIES_SECTION_PATTERN.matcher(html);
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private static List<String> extractCards(final String sectionHtml) {
        Matcher m = CARD_PATTERN.matcher(sectionHtml);
        List<String> cards = new ArrayList<>();
        while (m.find()) {
            cards.add(m.group(1));
        }
        return cards;
    }
}
