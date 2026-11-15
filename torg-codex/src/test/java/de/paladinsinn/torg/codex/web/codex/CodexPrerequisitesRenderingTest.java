package de.paladinsinn.torg.codex.web.codex;

import de.paladinsinn.torg.codex.api.security.CurrentUserCensorFactory;
import de.paladinsinn.torg.codex.application.port.in.CatalogBrowseResult;
import de.paladinsinn.torg.codex.application.port.in.CatalogBrowseResult.ResolvedEntry;
import de.paladinsinn.torg.codex.application.port.in.CatalogFilterCriteria;
import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import de.paladinsinn.torg.codex.domain.catalog.CatalogCategoryRegistry;
import de.paladinsinn.torg.codex.domain.markup.Censor;
import de.paladinsinn.torg.codex.domain.markup.ConditionalBlockProcessor;
import de.paladinsinn.torg.codex.domain.markup.EntityReferenceProcessor;
import de.paladinsinn.torg.codex.domain.markup.GameTokenProcessor;
import de.paladinsinn.torg.codex.domain.markup.MarkdownProcessor;
import de.paladinsinn.torg.codex.domain.markup.RawHtmlProcessor;
import de.paladinsinn.torg.codex.domain.markup.TorgMarkupService;
import de.paladinsinn.torg.codex.domain.model.ClearanceLevel;
import de.paladinsinn.torg.codex.domain.model.Perk;
import de.paladinsinn.torg.codex.web.codex.mapper.CodexViewMapper;
import de.paladinsinn.torg.codex.web.codex.model.TableViewModel;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Exercises prerequisite markup through the real mapper, rendering pipeline and JTE templates.
 */
class CodexPrerequisitesRenderingTest {

    @ParameterizedTest
    @ValueSource(strings = {"codex/pages/category.jte", "codex/fragments/categoryResults.jte"})
    void rendersPrerequisiteReferencesAsLinks(final String template) {
        TableViewModel model = table("Dexterity 8, <perk:realm-runner> and <race:human>", Set.of("core-rulebook"));

        String html = render(template, model);

        assertThat(html).contains("Dexterity 8", "href=\"/perks/realm-runner\"", ">Realm Runner</a>",
                        "href=\"/races/human\"", ">Human</a>")
                .doesNotContain("&lt;perk:", "<perk:", "&lt;race:", "<race:", "&lt;a ");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void censorsPrerequisitesBeforeRenderingLinks(final boolean ownsSourcebook) {
        Set<String> products = ownsSourcebook
                ? Set.of("core-rulebook", "sourcebook-aysle") : Set.of("core-rulebook");
        TableViewModel model = table(
                "Dexterity 8 <IF:sourcebook-aysle><perk:realm-runner></IF>", products);

        String html = render("codex/fragments/categoryResults.jte", model);

        assertThat(html).contains("Dexterity 8").doesNotContain("&lt;IF:", "<IF:");
        if (ownsSourcebook) {
            assertThat(html).contains("href=\"/perks/realm-runner\"");
        } else {
            assertThat(html).doesNotContain("realm-runner", "Realm Runner");
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "Dexterity 8"})
    void handlesPlainAndEmptyPrerequisites(final String prerequisites) {
        String html = render("codex/fragments/categoryResults.jte",
                table(prerequisites, Set.of("core-rulebook")));

        assertThat(html).contains("<table", "Test Perk").doesNotContain(">null<");
        if (prerequisites != null && !prerequisites.isBlank()) {
            assertThat(html).contains(prerequisites);
        }
    }

    private TableViewModel table(final String prerequisites, final Set<String> products) {
        TorgMarkupService markup = new TorgMarkupService(new ConditionalBlockProcessor(),
                new EntityReferenceProcessor(), new RawHtmlProcessor(),
                new GameTokenProcessor(), new MarkdownProcessor());
        CurrentUserCensorFactory factory = mock(CurrentUserCensorFactory.class);
        when(factory.create()).thenReturn(Censor.of(markup, products));
        Perk perk = new Perk(UUID.randomUUID(), "Test Perk", ClearanceLevel.ALPHA,
                Set.of("core-rulebook"), false, Set.of(), "prowess", prerequisites, "");
        CatalogBrowseResult result = new CatalogBrowseResult(CatalogArea.PERK,
                CatalogCategoryRegistry.forArea(CatalogArea.PERK),
                List.of(new ResolvedEntry(perk, "test-perk")), 1,
                List.of(), Map.of(), CatalogFilterCriteria.empty());
        return new CodexViewMapper(factory).toTableViewModel(result, null);
    }

    private String render(final String template, final TableViewModel model) {
        StringOutput output = new StringOutput();
        TemplateEngine.createPrecompiled(ContentType.Html).render(template, Map.of("model", model), output);
        return output.toString();
    }
}
