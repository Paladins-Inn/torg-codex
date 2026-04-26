package de.paladinsinn.torg.codex.torgcodex.markup;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class MarkdownProcessor {

    private static final Pattern BULLET_POINT = Pattern.compile("^• ", Pattern.MULTILINE);

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    public String process(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String normalized = BULLET_POINT.matcher(text).replaceAll("- ");
        var document = parser.parse(normalized);
        return renderer.render(document);
    }
}
