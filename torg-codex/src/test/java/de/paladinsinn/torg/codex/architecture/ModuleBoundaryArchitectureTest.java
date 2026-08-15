package de.paladinsinn.torg.codex.architecture;

import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ModuleBoundaryArchitectureTest {

    @Test
    void domainDependsOnNoOtherReactorModule() {
        ArchitectureTestSupport.assertNoUnfrozenViolations(noClasses()
                .that().resideInAnyPackage("de.paladinsinn.torg.codex.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "de.paladinsinn.torg.codex.application..",
                        "de.paladinsinn.torg.codex.data..",
                        "de.paladinsinn.torg.codex.api..",
                        "de.paladinsinn.torg.codex.configuration..",
                        "de.paladinsinn.drivethru..",
                        "de.paladinsinn.security..",
                        "de.kaiserpfalz..")
                .as("torg-codex-domain must not depend on other reactor modules"));
    }

    @Test
    void applicationDependsOnlyOnDomainAndJava() {
        ArchitectureTestSupport.assertNoUnfrozenViolations(noClasses()
                .that().resideInAnyPackage("de.paladinsinn.torg.codex.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "de.paladinsinn.torg.codex.data..",
                        "de.paladinsinn.torg.codex.api..",
                        "de.paladinsinn.torg.codex.configuration..",
                        "de.paladinsinn.drivethru..",
                        "de.paladinsinn.security..",
                        "de.kaiserpfalz..")
                .as("torg-codex-application must depend inward only")
                .allowEmptyShould(true));
    }

    @Test
    void drivingPortsLiveInApplicationModule() {
        ArchitectureTestSupport.assertNoUnfrozenViolations(classes()
                .that().haveSimpleName("CatalogQuery")
                .or().haveSimpleName("CatalogReferenceQuery")
                .should().resideInAnyPackage("de.paladinsinn.torg.codex.application.port.in..")
                .as("Driving ports must live in torg-codex-application"));
    }

    @Test
    void drivenPortsLiveInApplicationModule() {
        ArchitectureTestSupport.assertNoUnfrozenViolations(classes()
                .that().haveSimpleName("CatalogPersistencePort")
                .or().haveSimpleName("CatalogReferencePersistencePort")
                .should().resideInAnyPackage("de.paladinsinn.torg.codex.application.port.out..")
                .as("Driven ports must live in torg-codex-application"));
    }

    @Test
    void frameworkFreeServicesLiveInApplicationModule() {
        ArchitectureTestSupport.assertNoUnfrozenViolations(classes()
                .that().haveSimpleName("CatalogQueryService")
                .or().haveSimpleName("CatalogReferenceQueryService")
                .should().resideInAnyPackage("de.paladinsinn.torg.codex.application.service..")
                .as("Framework-free use-case services must live in torg-codex-application"));
    }
}
