package de.paladinsinn.torg.codex.data.markup;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GameTokenProcessor {

    private static final Pattern GAME_TOKEN = Pattern.compile("\\[([a-zA-Z][a-zA-Z0-9]*)\\]");

    public String process(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        Matcher matcher = GAME_TOKEN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String tokenName = matcher.group(1);
            String cssClass = GameTokenRegistry.cssClassFor(tokenName);
            String displayName = tokenName.substring(0, 1).toUpperCase() + tokenName.substring(1).toLowerCase();
            String span = "<span class=\"game-token " + cssClass + "\" title=\"" + displayName + "\"></span>";
            matcher.appendReplacement(result, Matcher.quoteReplacement(span));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
