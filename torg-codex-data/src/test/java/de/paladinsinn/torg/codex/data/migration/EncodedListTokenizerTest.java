/*
 * Copyright (c) 2026. Roland T. Lichti <rlichti@kaiserpfalz-edv.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package de.paladinsinn.torg.codex.data.migration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EncodedListTokenizerTest {

    @Test
    void convertsSingleAndMultipleStatements() {
        assertThat(convert("['only one']")).hasValue("only one");
        assertThat(convert("['a', 'b']")).hasValue("- a\n- b");
        assertThat(convert("[]")).hasValue("");
    }

    @Test
    void preservesStatementContent() {
        assertThat(convert("[\"has 'inner' quote\", 'contains a, comma']"))
                .hasValue("- has 'inner' quote\n- contains a, comma");
        assertThat(convert("['multi\nline']")).hasValue("multi\nline");
        assertThat(convert("['[Perk:foo]', '*formatted*']")).hasValue("- [Perk:foo]\n- *formatted*");
    }

    @Test
    void ignoresValuesThatAreNotCompleteEncodedLists() {
        assertThat(convert("see [table] for details")).isEmpty();
        assertThat(convert("- already\n- converted")).isEmpty();
        assertThat(convert("['unterminated")).isEmpty();
        assertThat(convert("prefix ['a']")).isEmpty();
    }

    @Test
    void recoversEscapedAndMixedQuotes() {
        assertThat(convert("['it\\'s escaped']")).hasValue("it's escaped");
        assertThat(convert("['single', \"double\"]")).hasValue("- single\n- double");
        assertThat(convert("[\"say \\\"hi\\\"\"]")).hasValue("say \"hi\"");
    }

    @Test
    void toleratesSurroundingWhitespaceAndKeepsSourceOrder() {
        assertThat(convert("  [ 'b' ,\n  'a' ,'c' ]  ")).hasValue("- b\n- a\n- c");
        assertThat(convert("[ ]")).hasValue("");
    }

    @Test
    void rejectsMalformedLists() {
        assertThat(convert("['a' 'b']")).isEmpty();
        assertThat(convert("[1, 2]")).isEmpty();
        assertThat(convert("['a'] trailing")).isEmpty();
        assertThat(convert("['a'], ['b']")).isEmpty();
        assertThat(convert("['dangling\\']")).isEmpty();
        assertThat(convert("[")).isEmpty();
        assertThat(convert(null)).isEmpty();
    }

    @Test
    void convertedValuesAreNotConvertedAgain() {
        for (String encoded : java.util.List.of("['only one']", "['a', 'b']", "[]",
                "[\"has 'inner' quote\", 'contains a, comma']", "['[Perk:foo]', '*formatted*']")) {
            String converted = convert(encoded).orElseThrow();

            assertThat(EncodedListTokenizer.parse(converted)).as("second conversion of %s", encoded).isEmpty();
        }
    }

    private java.util.Optional<String> convert(String value) {
        return EncodedListTokenizer.parse(value).map(EncodedListTokenizer::toMarkdown);
    }
}
