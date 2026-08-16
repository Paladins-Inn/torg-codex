package de.paladinsinn.torg.codex.domain.markup;


import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConditionalBlockProcessor {

    private static final Pattern IF_BLOCK = Pattern.compile(
            "<IF:(!?)([a-z0-9-]+)>([\\s\\S]*?)</IF>",
            Pattern.DOTALL
    );

    public String process(String text, Set<String> ownedProducts) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        Matcher matcher = IF_BLOCK.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            boolean negated = "!".equals(matcher.group(1));
            String productId = matcher.group(2);
            String content = matcher.group(3);

            boolean owned = ownedProducts.contains(productId);
            boolean include = negated != owned;

            matcher.appendReplacement(result, Matcher.quoteReplacement(include ? content : ""));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
