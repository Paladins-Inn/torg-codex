package de.paladinsinn.torg.codex.data.markup;

import de.paladinsinn.torg.codex.data.markup.ConditionalBlockProcessor;
import de.paladinsinn.torg.codex.data.markup.GameTokenProcessor;
import de.paladinsinn.torg.codex.data.markup.MarkdownProcessor;
import de.paladinsinn.torg.codex.data.markup.RawHtmlProcessor;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TorgMarkupServiceTest {

    private final de.paladinsinn.torg.codex.data.markup.TorgMarkupService
        service = new de.paladinsinn.torg.codex.data.markup.TorgMarkupService(
            new ConditionalBlockProcessor(),
            new de.paladinsinn.torg.codex.data.markup.EntityReferenceProcessor(),
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
