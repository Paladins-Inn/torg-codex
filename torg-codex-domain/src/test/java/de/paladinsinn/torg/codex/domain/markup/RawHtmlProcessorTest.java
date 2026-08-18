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

import static org.junit.jupiter.api.Assertions.assertEquals;

class RawHtmlProcessorTest {

    private final RawHtmlProcessor processor = new RawHtmlProcessor();

    @Test
    void lineBreak() {
        assertEquals("text<br>more", processor.process("text[HTML:<br>]more"));
    }

    @Test
    void strongTag() {
        assertEquals("<strong>2023-06-19:</strong>", processor.process("[HTML:<strong>2023-06-19:</strong>]"));
    }

    @Test
    void anchorTag() {
        String result = processor.process("[HTML:<a href=\"/decks\">downloadable</a>]");
        assertEquals("<a href=\"/decks\">downloadable</a>", result);
    }

    @Test
    void imageTag() {
        String input = "[HTML:<img class=\"inline-image-small\" src=\"/static/icons/tokens/aysle.webp\">]";
        String result = processor.process(input);
        assertEquals("<img class=\"inline-image-small\" src=\"/static/icons/tokens/aysle.webp\">", result);
    }

    @Test
    void plainTextContent() {
        assertEquals("2021-03-31", processor.process("[HTML:2021-03-31]"));
    }

    @Test
    void multipleBlocks() {
        String input = "[HTML:<strong>a</strong>] and [HTML:<em>b</em>]";
        assertEquals("<strong>a</strong> and <em>b</em>", processor.process(input));
    }

    @Test
    void nullInput_returnsEmpty() {
        assertEquals("", processor.process(null));
    }

    @Test
    void noHtmlBlocks_textUnchanged() {
        String input = "plain text";
        assertEquals(input, processor.process(input));
    }
}
