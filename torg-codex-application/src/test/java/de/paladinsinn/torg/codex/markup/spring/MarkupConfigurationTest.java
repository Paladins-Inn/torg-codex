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
