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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link MarkupConfiguration} wires the framework-free
 * {@code de.paladinsinn.torg.codex.domain.markup} pipeline into a Spring context,
 * exposing the five processors and the aggregating {@link TorgMarkupService} as
 * injectable beans (ADR-017 framework-binding adapter pattern).
 */
@SpringJUnitConfig(MarkupConfiguration.class)
class MarkupConfigurationTest {

    @Autowired
    private ConditionalBlockProcessor conditionalBlockProcessor;

    @Autowired
    private EntityReferenceProcessor entityReferenceProcessor;

    @Autowired
    private RawHtmlProcessor rawHtmlProcessor;

    @Autowired
    private GameTokenProcessor gameTokenProcessor;

    @Autowired
    private MarkdownProcessor markdownProcessor;

    @Autowired
    private TorgMarkupService torgMarkupService;

    @Test
    void allMarkupProcessorBeansAreInjectable() {
        assertThat(conditionalBlockProcessor).isNotNull();
        assertThat(entityReferenceProcessor).isNotNull();
        assertThat(rawHtmlProcessor).isNotNull();
        assertThat(gameTokenProcessor).isNotNull();
        assertThat(markdownProcessor).isNotNull();
    }

    @Test
    void torgMarkupServiceBeanIsInjectableAndRenders() {
        assertThat(torgMarkupService).isNotNull();
        assertThat(torgMarkupService.render("**bold**", java.util.Set.of()))
                .contains("<strong>bold</strong>");
    }
}
