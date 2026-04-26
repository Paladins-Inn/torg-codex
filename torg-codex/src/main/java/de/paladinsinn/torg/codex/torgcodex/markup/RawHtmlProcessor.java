package de.paladinsinn.torg.codex.torgcodex.markup;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RawHtmlProcessor {

    private static final Pattern HTML_BLOCK = Pattern.compile("\\[HTML:(.*?)\\]");

    public String process(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        Matcher matcher = HTML_BLOCK.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(1)));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
