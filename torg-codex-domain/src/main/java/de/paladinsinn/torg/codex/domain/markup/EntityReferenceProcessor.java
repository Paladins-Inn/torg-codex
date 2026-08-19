/*
 * Copyright (c) 2026.  Roland T. Lichti <rlichti@kaiserpfalz-edv.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * ERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * You may contact me via email rlichti@kaiserpfalz-edv.de or via mail
 *
 * Kaiserpfalz EDV-Service
 * Roland T. Lichti
 * Darmstädter Str. 12
 * 64625 Bensheim
 * GERMANY
 */

package de.paladinsinn.torg.codex.domain.markup;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityReferenceProcessor {

    private static final Pattern ENTITY_REF = Pattern.compile(
            "<([a-z][-a-z]*):([a-z0-9][-a-z0-9]*)>"
    );

    /**
     * Replaces entity references with links to the referenced entities.
     *
     * @param text the markup to process
     * @return the processed markup
     */
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
