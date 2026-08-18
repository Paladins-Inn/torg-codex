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

package de.paladinsinn.torg.codex.domain.markup;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TorgMarkupServiceTest {

    private final de.paladinsinn.torg.codex.domain.markup.TorgMarkupService
        service = new de.paladinsinn.torg.codex.domain.markup.TorgMarkupService(
            new ConditionalBlockProcessor(),
            new de.paladinsinn.torg.codex.domain.markup.EntityReferenceProcessor(),
            new RawHtmlProcessor(),
            new GameTokenProcessor(),
            new MarkdownProcessor()
    );

    @Test
    void fullPipeline_withConditionalAndTokens() {
        String input = """
                <IF:sourcebook-aysle>**Aysle** content with [shock] damage</IF>\
                <IF:!sourcebook-aysle>Basic content</IF>""";

        String result = service.render(input, Set.of("sourcebook-aysle"));
        assertTrue(result.contains("<strong>Aysle</strong>"));
        assertTrue(result.contains("game-token-shock"));
        assertFalse(result.contains("Basic content"));
    }

    @Test
    void fullPipeline_withEntityRefAndHtml() {
        String input = "Uses the <perk:negation> Perk. [HTML:<br>]See also [possibilities].";
        String result = service.render(input, Set.of());
        assertTrue(result.contains("<a href=\"/perks/negation\""));
        assertTrue(result.contains("<br>"));
        assertTrue(result.contains("game-token-possibilities"));
    }

    @Test
    void fullPipeline_realWorldExcerpt() {
        String input = """
                <IF:sourcebook-tharkold>**Tharkold:** You may spend a Tharkold Possibility to re-roll [BD] of damage.</IF>""";
        String result = service.render(input, Set.of("sourcebook-tharkold"));
        assertTrue(result.contains("<strong>Tharkold:</strong>"));
        assertTrue(result.contains("game-token-bd"));
    }

    @Test
    void nullInput_returnsEmpty() {
        assertEquals("", service.render(null, Set.of()));
    }

    @Test
    void blankInput_returnsEmpty() {
        assertEquals("", service.render("   ", Set.of()));
    }

    @Test
    void plainMarkdown_noCustomMarkup() {
        String result = service.render("## Title\n\n**Bold** text", Set.of());
        assertTrue(result.contains("<h2>Title</h2>"));
        assertTrue(result.contains("<strong>Bold</strong>"));
    }
}
