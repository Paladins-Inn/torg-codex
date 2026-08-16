package de.paladinsinn.torg.codex.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

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
 *   <li>Only {@code CurrentUserCensorFactory} (in {@code api.security}) may depend on the
 *       {@code ProductOwnershipResolver}; controllers reach censoring exclusively through the
 *       factory, and exactly the 15 gated controllers depend on it (FR-002/FR-003/SC-002).</li>
 * </ul>
 */
class CensoringSingleMechanismArchitectureTest {

    private static final String CONTROLLER_PACKAGE = "de.paladinsinn.torg.codex.api.controller";
    private static final int EXPECTED_GATED_CONTROLLER_COUNT = 15;

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

    @Test
    void onlyCensorFactoryDependsOnProductOwnershipResolver() {
        ArchitectureTestSupport.assertNoUnfrozenViolations(noClasses()
                .that().doNotHaveSimpleName("CurrentUserCensorFactory")
                .and().doNotHaveSimpleName("ProductOwnershipResolver")
                .should().dependOnClassesThat().haveSimpleName("ProductOwnershipResolver")
                .as("Only CurrentUserCensorFactory may depend on ProductOwnershipResolver; "
                        + "controllers reach censoring through the factory only (FR-002/FR-003)")
                .allowEmptyShould(true));
    }

    @Test
    void gatedControllersDependOnCensorFactoryOnly() {
        long controllersUsingFactory = ArchitectureTestSupport.IMPORTED_CLASSES.stream()
                .filter(clazz -> clazz.getPackageName().equals(CONTROLLER_PACKAGE))
                .filter(clazz -> clazz.getDirectDependenciesFromSelf().stream()
                        .anyMatch(dependency ->
                                dependency.getTargetClass().getSimpleName().equals("CurrentUserCensorFactory")))
                .count();

        assertThat(controllersUsingFactory)
                .as("exactly the 15 gated controllers depend on CurrentUserCensorFactory (FR-003)")
                .isEqualTo(EXPECTED_GATED_CONTROLLER_COUNT);

        // And no controller reaches the resolver directly.
        boolean anyControllerTouchesResolver = ArchitectureTestSupport.IMPORTED_CLASSES.stream()
                .filter(clazz -> clazz.getPackageName().equals(CONTROLLER_PACKAGE))
                .flatMap(clazz -> clazz.getDirectDependenciesFromSelf().stream())
                .map(dependency -> dependency.getTargetClass())
                .map(JavaClass::getSimpleName)
                .anyMatch(name -> name.equals("ProductOwnershipResolver"));

        assertThat(anyControllerTouchesResolver)
                .as("no controller may depend on ProductOwnershipResolver directly")
                .isFalse();
    }
}
