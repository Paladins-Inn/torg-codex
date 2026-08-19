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

public class GameTokenProcessor {

    private static final Pattern GAME_TOKEN = Pattern.compile("\\[([a-zA-Z][a-zA-Z0-9]*)]");

    /**
     * Replaces game-token markup with its HTML representation.
     *
     * @param text the markup to process
     * @return the processed markup
     */
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
