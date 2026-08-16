package de.paladinsinn.torg.codex.domain.markup;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityReferenceProcessor {

    private static final Pattern ENTITY_REF = Pattern.compile(
            "<([a-z][-a-z]*):([a-z0-9][-a-z0-9]*)>"
    );

    public String process(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        Matcher matcher = ENTITY_REF.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String typeTag = matcher.group(1);
            String entityId = matcher.group(2);

            var entityType = EntityType.fromTag(typeTag);
            if (entityType.isPresent()) {
                String displayName = EntityType.toDisplayName(entityId);
                String link = "<a href=\"" + entityType.get().urlSegment() + entityId
                        + "\" class=\"entity-ref entity-ref-" + typeTag + "\">"
                        + displayName + "</a>";
                matcher.appendReplacement(result, Matcher.quoteReplacement(link));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
