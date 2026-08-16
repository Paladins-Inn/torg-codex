package de.paladinsinn.torg.codex.architecture;

import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the data-persistence-only module boundary (feature 003, FR-001, FR-002).
 *
 * <p>The {@code torg-codex-data} module is a pure persistence adapter. It must never
 * reference the DriveThruRPG integration classes ({@code de.paladinsinn.drivethru..}),
 * the security integration classes ({@code de.paladinsinn.security..}), or the Spring
 * Security framework ({@code org.springframework.security..}). Any such reference added
 * in the future causes an immediate build failure naming the offending class and rule.
 *
 * <p>The {@code de.paladinsinn.torg.codex.data} package is non-empty, so no
 * {@code allowEmptyShould(true)} is used, and there are no FreezeList entries for these
 * rules (FR-011): violations must be resolved at the source, never suppressed.
 */
class DataPersistenceBoundaryArchitectureTest {

    @Test
    void dataMustNotReferenceSecurityIntegrationClasses() {
        ArchitectureTestSupport.assertNoUnfrozenViolations(noClasses()
                .that().resideInAnyPackage("de.paladinsinn.torg.codex.data..")
                .should().dependOnClassesThat().resideInAnyPackage("de.paladinsinn.security..")
                .as("torg-codex-data must not reference Security integration classes"));
    }

    @Test
    void dataMustNotReferenceDriveThruRpgIntegrationClasses() {
        ArchitectureTestSupport.assertNoUnfrozenViolations(noClasses()
                .that().resideInAnyPackage("de.paladinsinn.torg.codex.data..")
                .should().dependOnClassesThat().resideInAnyPackage("de.paladinsinn.drivethru..")
                .as("torg-codex-data must not reference DriveThruRPG integration classes"));
    }

    @Test
    void dataMustNotImportSpringSecurityFrameworkClasses() {
        ArchitectureTestSupport.assertNoUnfrozenViolations(noClasses()
                .that().resideInAnyPackage("de.paladinsinn.torg.codex.data..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework.security..")
                .as("torg-codex-data must not import Spring Security framework classes"));
    }
}
