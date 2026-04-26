package de.paladinsinn.torg.codex.data.markup;

import de.paladinsinn.torg.codex.data.markup.RawHtmlProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
