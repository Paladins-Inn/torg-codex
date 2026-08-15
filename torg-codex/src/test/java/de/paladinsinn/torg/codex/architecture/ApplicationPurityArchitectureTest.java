package de.paladinsinn.torg.codex.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ApplicationPurityArchitectureTest {

    @Test
    void applicationContainsNoSpringOrJpaInfrastructureImports() {
        ArchitectureTestSupport.assertNoUnfrozenViolations(noClasses()
                .that().resideInAnyPackage("de.paladinsinn.torg.codex.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "org.hibernate..")
                .as("torg-codex-application must stay framework-free")
                .allowEmptyShould(true));
    }

    @Test
    void applicationContainsNoTransactionalAnnotations() {
        ArchitectureTestSupport.assertNoUnfrozenViolations(noClasses()
                .that().resideInAnyPackage("de.paladinsinn.torg.codex.application..")
                .should().beAnnotatedWith(Transactional.class)
                .as("torg-codex-application must not declare @Transactional")
                .allowEmptyShould(true));
    }
}
