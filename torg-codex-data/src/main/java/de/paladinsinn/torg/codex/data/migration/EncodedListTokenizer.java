/*
 * Copyright (c) 2026. Roland T. Lichti <rlichti@kaiserpfalz-edv.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package de.paladinsinn.torg.codex.data.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Parses complete Python-style string-list literals without interpreting statement markup. */
public final class EncodedListTokenizer {

    private EncodedListTokenizer() {
    }

    public static Optional<List<String>> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        final String input = value.trim();
        if (input.length() < 2 || input.charAt(0) != '[' || input.charAt(input.length() - 1) != ']') {
            return Optional.empty();
        }
        final List<String> result = new ArrayList<>();
        int index = 1;
        while (true) {
            index = skipWhitespace(input, index);
            if (index == input.length() - 1) {
                return Optional.of(List.copyOf(result));
            }
            final char quote = input.charAt(index);
            if (quote != '\'' && quote != '"') {
                return Optional.empty();
            }
            index++;
            final StringBuilder statement = new StringBuilder();
            boolean closed = false;
            while (index < input.length() - 1) {
                char current = input.charAt(index++);
                if (current == '\\') {
                    if (index >= input.length() - 1) {
                        return Optional.empty();
                    }
                    current = input.charAt(index++);
                    statement.append(current);
                } else if (current == quote) {
                    closed = true;
                    break;
                } else {
                    statement.append(current);
                }
            }
            if (!closed) {
                return Optional.empty();
            }
            result.add(statement.toString());
            index = skipWhitespace(input, index);
            if (index >= input.length()) {
                return Optional.empty();
            }
            if (input.charAt(index) == ']') {
                return index == input.length() - 1 ? Optional.of(List.copyOf(result)) : Optional.empty();
            }
            if (input.charAt(index) != ',') {
                return Optional.empty();
            }
            index++;
        }
    }

    public static String toMarkdown(List<String> statements) {
        if (statements.isEmpty()) {
            return "";
        }
        if (statements.size() == 1) {
            return statements.getFirst();
        }
        return statements.stream().map(statement -> "- " + statement).collect(java.util.stream.Collectors.joining("\n"));
    }

    private static int skipWhitespace(String input, int index) {
        while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
            index++;
        }
        return index;
    }
}
