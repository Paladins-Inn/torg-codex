package de.paladinsinn.torg.codex.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces that transaction demarcation lives exclusively at the inbound-adapter/composition
 * boundary (e.g. {@code @RestController} classes in {@code torg-codex}), never inside the
 * framework-independent {@code torg-codex-application} or {@code torg-codex-domain} modules.
 *
 * <p>See Phase 4d, task T117 (split into T117a-T117q per catalog area) for the relocation of
 * {@code @Transactional} to each catalog area's controller.
 */
class TransactionBoundaryArchitectureTest {

    @Test
    void applicationContainsNoTransactionalAnnotations() {
        ArchitectureTestSupport.assertNoUnfrozenViolations(noClasses()
                .that().resideInAnyPackage("de.paladinsinn.torg.codex.application..")
                .should().beAnnotatedWith(Transactional.class)
                .as("torg-codex-application must not declare @Transactional")
                .allowEmptyShould(true));
    }

    @Test
    void domainContainsNoTransactionalAnnotations() {
        ArchitectureTestSupport.assertNoUnfrozenViolations(noClasses()
                .that().resideInAnyPackage("de.paladinsinn.torg.codex.domain..")
                .should().beAnnotatedWith(Transactional.class)
                .as("torg-codex-domain must not declare @Transactional")
                .allowEmptyShould(true));
    }

    @Test
    void noMethodInApplicationOrDomainIsAnnotatedTransactional() {
        ArchitectureTestSupport.assertNoUnfrozenViolations(
                com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods()
                        .that().areDeclaredInClassesThat().resideInAnyPackage(
                                "de.paladinsinn.torg.codex.application..",
                                "de.paladinsinn.torg.codex.domain..")
                        .should().beAnnotatedWith(Transactional.class)
                        .as("no method in torg-codex-application or torg-codex-domain may declare @Transactional")
                        .allowEmptyShould(true));
    }
}
