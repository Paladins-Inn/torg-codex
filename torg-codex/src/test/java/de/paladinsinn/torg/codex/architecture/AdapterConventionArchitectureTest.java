package de.paladinsinn.torg.codex.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdapterConventionArchitectureTest {

    /** The 17 catalog-area REST controllers rewired in Phase 4d (T099-T115). */
    private static final List<String> CATALOG_CONTROLLER_SIMPLE_NAMES = List.of(
            "ArticleController", "CosmController", "ItemController", "MiracleController",
            "MiracleListController", "PerkController", "PerkGroupController", "PowerController",
            "PowerListController", "PublicationController", "RaceController", "ShardController",
            "SpellController", "SpellListController", "TagController", "ThreatController",
            "VehicleController");

    @Test
    void controllersRemainInInboundAdapterPackage() {
        ArchitectureTestSupport.assertNoUnfrozenViolations(classes()
                .that().areAnnotatedWith(Controller.class)
                .or().areAnnotatedWith(RestController.class)
                .should().resideInAnyPackage("de.paladinsinn.torg.codex.api.controller..")
                .as("REST controllers must live under api.controller"));
    }

    @Test
    void outboundAdaptersRemainInDataAdapterOutPackages() {
        ArchitectureTestSupport.assertNoUnfrozenViolations(classes()
                .that().haveSimpleNameEndingWith("Adapter")
                .and().resideInAnyPackage("de.paladinsinn.torg.codex.data..")
                .should().resideInAnyPackage("de.paladinsinn.torg.codex.data.adapter.out..")
                .as("Persistence, HTTP, and event adapters must live under data.adapter.out"));
    }

    @Test
    void driveThruRpgOutboundAdapterLivesUnderDriveThruAdapterOutHttp() {
        JavaClasses imported = ArchitectureTestSupport.IMPORTED_CLASSES;
        boolean found = imported.stream().anyMatch(clazz ->
                clazz.getSimpleName().equals("DriveThruRpgProductAdapter")
                        && clazz.getPackageName().equals(
                                "de.paladinsinn.drivethru.adapter.out.http"));
        assertTrue(found,
                "DriveThruRpgProductAdapter must exist under drivethru.adapter.out.http "
                        + "as the outbound HTTP adapter for the DriveThruRPG product catalog");
    }

    @Test
    void domainEventBridgeOutboundAdapterLivesUnderDriveThruAdapterOutEvent() {
        JavaClasses imported = ArchitectureTestSupport.IMPORTED_CLASSES;
        boolean found = imported.stream().anyMatch(clazz ->
                clazz.getSimpleName().equals("SpringDomainEventPublisherAdapter")
                        && clazz.getPackageName().equals(
                                "de.paladinsinn.drivethru.adapter.out.event"));
        assertTrue(found,
                "SpringDomainEventPublisherAdapter must exist under drivethru.adapter.out.event "
                        + "as the outbound bridge from torg-codex-domain events to Spring's "
                        + "ApplicationEventPublisher");
    }

    @Test
    void everyCatalogAreaControllerIntroducedInPhase4dExistsUnderApiController() {
        JavaClasses imported = ArchitectureTestSupport.IMPORTED_CLASSES;
        List<String> missing = CATALOG_CONTROLLER_SIMPLE_NAMES.stream()
                .filter(name -> imported.stream().noneMatch(clazz ->
                        clazz.getSimpleName().equals(name)
                                && clazz.getPackageName().equals(
                                        "de.paladinsinn.torg.codex.api.controller")))
                .toList();
        assertTrue(missing.isEmpty(),
                "Missing (or misplaced) Phase 4d catalog-area controllers under "
                        + "api.controller: " + missing);
    }
}
