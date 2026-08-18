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

package de.paladinsinn.torg.codex.markup.spring;

import de.paladinsinn.torg.codex.domain.markup.ConditionalBlockProcessor;
import de.paladinsinn.torg.codex.domain.markup.EntityReferenceProcessor;
import de.paladinsinn.torg.codex.domain.markup.GameTokenProcessor;
import de.paladinsinn.torg.codex.domain.markup.MarkdownProcessor;
import de.paladinsinn.torg.codex.domain.markup.RawHtmlProcessor;
import de.paladinsinn.torg.codex.domain.markup.TorgMarkupService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring framework-binding adapter for the framework-free markup pipeline owned by
 * {@code de.paladinsinn.torg.codex.domain.markup} (ADR-017).
 *
 * <p>The domain markup classes are pure POJOs with no Spring annotations. This
 * configuration wires them into the application context as beans, following the same
 * framework-binding pattern used for {@code de.paladinsinn.drivethru.*} and
 * {@code de.paladinsinn.security.*} integrations.
 *
 * <p>{@code GameTokenRegistry} is a static utility class and therefore intentionally
 * has no bean definition.
 */
@Configuration
public class MarkupConfiguration {

    @Bean
    public ConditionalBlockProcessor conditionalBlockProcessor() {
        return new ConditionalBlockProcessor();
    }

    @Bean
    public EntityReferenceProcessor entityReferenceProcessor() {
        return new EntityReferenceProcessor();
    }

    @Bean
    public RawHtmlProcessor rawHtmlProcessor() {
        return new RawHtmlProcessor();
    }

    @Bean
    public GameTokenProcessor gameTokenProcessor() {
        return new GameTokenProcessor();
    }

    @Bean
    public MarkdownProcessor markdownProcessor() {
        return new MarkdownProcessor();
    }

    @Bean
    public TorgMarkupService torgMarkupService(
            ConditionalBlockProcessor conditionalBlockProcessor,
            EntityReferenceProcessor entityReferenceProcessor,
            RawHtmlProcessor rawHtmlProcessor,
            GameTokenProcessor gameTokenProcessor,
            MarkdownProcessor markdownProcessor
    ) {
        return new TorgMarkupService(
                conditionalBlockProcessor,
                entityReferenceProcessor,
                rawHtmlProcessor,
                gameTokenProcessor,
                markdownProcessor
        );
    }
}
