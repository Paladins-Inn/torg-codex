package de.paladinsinn.torg.codex.torgcodex.markup;

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
