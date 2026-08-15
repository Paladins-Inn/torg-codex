package de.paladinsinn.torg.codex.architecture;

import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class DomainPurityArchitectureTest {

    @Test
    void domainContainsNoSpringOrJpaImports() {
        ArchitectureTestSupport.assertNoUnfrozenViolations(noClasses()
                .that().resideInAnyPackage("de.paladinsinn.torg.codex.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "org.hibernate..")
                .as("torg-codex-domain must not import Spring, JPA, or Hibernate"));
    }
}
