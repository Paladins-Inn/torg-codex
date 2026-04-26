package de.paladinsinn.torg.codex.torgcodex.markup;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConditionalBlockProcessorTest {

    private final ConditionalBlockProcessor processor = new ConditionalBlockProcessor();

    @Test
    void positiveCondition_productOwned_includesContent() {
        String input = "before <IF:sourcebook-aysle>included</IF> after";
        String result = processor.process(input, Set.of("sourcebook-aysle"));
        assertEquals("before included after", result);
    }

    @Test
    void positiveCondition_productNotOwned_stripsContent() {
        String input = "before <IF:sourcebook-aysle>removed</IF> after";
        String result = processor.process(input, Set.of());
        assertEquals("before  after", result);
    }

    @Test
    void negatedCondition_productOwned_stripsContent() {
        String input = "before <IF:!sourcebook-aysle>removed</IF> after";
        String result = processor.process(input, Set.of("sourcebook-aysle"));
        assertEquals("before  after", result);
    }

    @Test
    void negatedCondition_productNotOwned_includesContent() {
        String input = "before <IF:!sourcebook-aysle>included</IF> after";
        String result = processor.process(input, Set.of());
        assertEquals("before included after", result);
    }

    @Test
    void adjacentBlocks_differentOwnership() {
        String input = "<IF:!sourcebook-aysle>basic version</IF><IF:sourcebook-aysle>expanded version</IF>";

        String withOwnership = processor.process(input, Set.of("sourcebook-aysle"));
        assertEquals("expanded version", withOwnership);

        String withoutOwnership = processor.process(input, Set.of());
        assertEquals("basic version", withoutOwnership);
    }

    @Test
    void noIfBlocks_textUnchanged() {
        String input = "plain text with **markdown** and [tokens]";
        String result = processor.process(input, Set.of("sourcebook-aysle"));
        assertEquals(input, result);
    }

    @Test
    void multilineContent_insideIfBlock() {
        String input = "<IF:sourcebook-aysle>line one\nline two\nline three</IF>";
        String result = processor.process(input, Set.of("sourcebook-aysle"));
        assertEquals("line one\nline two\nline three", result);
    }

    @Test
    void nullInput_returnsEmpty() {
        assertEquals("", processor.process(null, Set.of()));
    }

    @Test
    void emptyInput_returnsEmpty() {
        assertEquals("", processor.process("", Set.of()));
    }

    @Test
    void unclosedIfBlock_leftAsIs() {
        String input = "before <IF:sourcebook-aysle>no closing tag after";
        String result = processor.process(input, Set.of("sourcebook-aysle"));
        assertEquals(input, result);
    }
}
