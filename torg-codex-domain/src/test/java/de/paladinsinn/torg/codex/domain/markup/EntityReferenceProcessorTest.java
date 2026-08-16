package de.paladinsinn.torg.codex.domain.markup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityReferenceProcessorTest {

    private final de.paladinsinn.torg.codex.domain.markup.EntityReferenceProcessor
        processor = new de.paladinsinn.torg.codex.domain.markup.EntityReferenceProcessor();

    @Test
    void cosmReference() {
        String result = processor.process("see <cosm:aysle> for details");
        assertEquals("see <a href=\"/cosms/aysle\" class=\"entity-ref entity-ref-cosm\">Aysle</a> for details", result);
    }

    @Test
    void perkReference() {
        String result = processor.process("the <perk:negation> Perk");
        assertEquals("the <a href=\"/perks/negation\" class=\"entity-ref entity-ref-perk\">Negation</a> Perk", result);
    }

    @Test
    void spellListReference() {
        String result = processor.process("added to <spell-list:demon-magic>");
        assertEquals("added to <a href=\"/spell-lists/demon-magic\" class=\"entity-ref entity-ref-spell-list\">Demon Magic</a>", result);
    }

    @Test
    void threatWithLongId() {
        String result = processor.process("<threat:tharkold-russian-sso-special-forces-operative>");
        assertEquals("<a href=\"/threats/tharkold-russian-sso-special-forces-operative\" class=\"entity-ref entity-ref-threat\">Tharkold Russian Sso Special Forces Operative</a>", result);
    }

    @Test
    void multipleReferences() {
        String result = processor.process("<perk:brawler> and <perk:brute>");
        assertTrue(result.contains("Brawler</a>"));
        assertTrue(result.contains("Brute</a>"));
    }

    @Test
    void unknownType_leftUnchanged() {
        String input = "<unknown:foo>";
        // "unknown" doesn't start with a letter followed by a valid entity type, so it won't match
        // Actually it will match the regex but EntityType.fromTag returns empty
        String result = processor.process(input);
        assertEquals(input, result);
    }

    @Test
    void nullInput_returnsEmpty() {
        assertEquals("", processor.process(null));
    }

    @Test
    void noReferences_textUnchanged() {
        String input = "plain text without references";
        assertEquals(input, processor.process(input));
    }
}
