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

import static org.junit.jupiter.api.Assertions.*;

class MarkdownProcessorTest {

    private final MarkdownProcessor processor = new MarkdownProcessor();

    @Test
    void boldText() {
        String result = processor.process("**bold**");
        assertTrue(result.contains("<strong>bold</strong>"));
    }

    @Test
    void italicText() {
        String result = processor.process("*italic*");
        assertTrue(result.contains("<em>italic</em>"));
    }

    @Test
    void heading() {
        String result = processor.process("## The Law of Magic");
        assertTrue(result.contains("<h2>The Law of Magic</h2>"));
    }

    @Test
    void bulletPoints_unicodeBullet() {
        String result = processor.process("• **Magic:** description\n• **Dark:** other");
        assertTrue(result.contains("<li>"));
        assertTrue(result.contains("<strong>Magic:</strong>"));
    }

    @Test
    void htmlPassthrough() {
        String result = processor.process("text <span class=\"game-token\"></span> more");
        assertTrue(result.contains("<span class=\"game-token\"></span>"));
    }

    @Test
    void nullInput_returnsEmpty() {
        assertEquals("", processor.process(null));
    }

    @Test
    void emptyInput_returnsEmpty() {
        assertEquals("", processor.process(""));
    }
}
