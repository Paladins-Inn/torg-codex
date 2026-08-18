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

public class TorgMarkupService {

    private final ConditionalBlockProcessor conditionalProcessor;
    private final EntityReferenceProcessor entityProcessor;
    private final RawHtmlProcessor rawHtmlProcessor;
    private final GameTokenProcessor gameTokenProcessor;
    private final MarkdownProcessor markdownProcessor;

    public TorgMarkupService(
            ConditionalBlockProcessor conditionalProcessor,
            EntityReferenceProcessor entityProcessor,
            RawHtmlProcessor rawHtmlProcessor,
            GameTokenProcessor gameTokenProcessor,
            MarkdownProcessor markdownProcessor
    ) {
        this.conditionalProcessor = conditionalProcessor;
        this.entityProcessor = entityProcessor;
        this.rawHtmlProcessor = rawHtmlProcessor;
        this.gameTokenProcessor = gameTokenProcessor;
        this.markdownProcessor = markdownProcessor;
    }

    public String render(String rawText, Set<String> ownedProducts) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        String result = rawText;
        result = conditionalProcessor.process(result, ownedProducts);
        result = entityProcessor.process(result);
        result = rawHtmlProcessor.process(result);
        result = gameTokenProcessor.process(result);
        result = markdownProcessor.process(result);
        return result;
    }
}
