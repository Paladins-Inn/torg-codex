package de.paladinsinn.torg.codex.architecture;

import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces constitution Principle V's single-mechanism rule (feature
 * {@code 001-unify-censoring-authorization}): product-ownership derivation for content
 * censoring MUST be confined to a single production component and its narrowly-scoped
 * collaborator, both living in {@code de.paladinsinn.torg.codex.api.security}.
 *
 * <p>Concretely:
 * <ul>
 *   <li>No production class named {@code SecuredMarkupService} may exist any longer
 *       (the removed duplicate, FR-007 / User Story 2, Acceptance Scenario 3).</li>
 *   <li>Within the {@code torg-codex} application module, only classes in
 *       {@code de.paladinsinn.torg.codex.api.security} may depend on Spring Security's
 *       {@code GrantedAuthority} to derive the owned product-id set; no controller, mapper,
 *       repository, or entity may re-implement that decision. Authority <em>producers</em> such
 *       as {@code DriveThruUserDetails} / {@code NotLoggedInUserDetails} live in the
 *       {@code torg-codex-data} module ({@code de.paladinsinn.security}) and are unaffected.</li>
 * </ul>
 */
class CensoringSingleMechanismArchitectureTest {

    @Test
    void noSecuredMarkupServiceRemainsInProductionCode() {
        ArchitectureTestSupport.assertNoUnfrozenViolations(noClasses()
                .that().resideInAnyPackage("de.paladinsinn..")
                .should().haveSimpleName("SecuredMarkupService")
                .as("The duplicate SecuredMarkupService ownership-resolution mechanism must be removed (FR-007)"));
    }

    @Test
    void grantedAuthorityDerivationIsConfinedToApiSecurityPackage() {
        ArchitectureTestSupport.assertNoUnfrozenViolations(noClasses()
                .that().resideInAPackage("de.paladinsinn.torg.codex..")
                .and().resideOutsideOfPackage("de.paladinsinn.torg.codex.api.security..")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.security.core.GrantedAuthority")
                .as("Only de.paladinsinn.torg.codex.api.security may derive product ownership from "
                        + "GrantedAuthority (constitution Principle V single-mechanism rule)"));
    }
}
