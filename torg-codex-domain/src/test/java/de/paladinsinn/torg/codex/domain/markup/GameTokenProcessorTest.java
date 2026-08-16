package de.paladinsinn.torg.codex.domain.markup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameTokenProcessorTest {

    private final GameTokenProcessor processor = new GameTokenProcessor();

    @Test
    void knownToken_shock() {
        String result = processor.process("takes [shock] damage");
        assertTrue(result.contains("game-token-shock"));
        assertTrue(result.contains("title=\"Shock\""));
    }

    @Test
    void knownToken_BD() {
        String result = processor.process("adds 1 [BD] to damage");
        assertTrue(result.contains("game-token-bd"));
    }

    @Test
    void knownToken_possibilities() {
        String result = processor.process("spend [possibilities]");
        assertTrue(result.contains("game-token-possibilities"));
    }

    @Test
    void caseInsensitive_Shock() {
        String lower = processor.process("[shock]");
        String upper = processor.process("[Shock]");
        // Both should use game-token-shock CSS class
        assertTrue(lower.contains("game-token-shock"));
        assertTrue(upper.contains("game-token-shock"));
    }

    @Test
    void tokenWithDigits_stymied2() {
        String result = processor.process("[stymied2]");
        assertTrue(result.contains("game-token-stymied2"));
    }

    @Test
    void unknownToken_getsUnknownClass() {
        String result = processor.process("[foobar]");
        assertTrue(result.contains("game-token-unknown"));
    }

    @Test
    void nullInput_returnsEmpty() {
        assertEquals("", processor.process(null));
    }

    @Test
    void noTokens_textUnchanged() {
        String input = "plain text without tokens";
        assertEquals(input, processor.process(input));
    }
}
