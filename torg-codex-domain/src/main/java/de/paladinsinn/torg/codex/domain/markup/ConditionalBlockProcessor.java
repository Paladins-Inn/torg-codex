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


import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConditionalBlockProcessor {

    private static final int CONTENT_GROUP = 3;

    private static final Pattern IF_BLOCK = Pattern.compile(
            "<IF:(!?)([a-z0-9-]+)>([\\s\\S]*?)</IF>",
            Pattern.DOTALL
    );

    /**
     * Resolves conditional blocks according to the products owned by the current user.
     *
     * @param text the markup to process
     * @param ownedProducts the owned product identifiers
     * @return the markup with conditional blocks resolved
     */
    public String process(String text, Set<String> ownedProducts) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        Matcher matcher = IF_BLOCK.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            boolean negated = "!".equals(matcher.group(1));
            String productId = matcher.group(2);
            String content = matcher.group(CONTENT_GROUP);

            boolean owned = ownedProducts.contains(productId);
            boolean include = negated != owned;

            matcher.appendReplacement(result, Matcher.quoteReplacement(include ? content : ""));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
