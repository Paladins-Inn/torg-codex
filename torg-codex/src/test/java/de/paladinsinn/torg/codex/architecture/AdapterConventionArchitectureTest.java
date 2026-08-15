package de.paladinsinn.torg.codex.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

class AdapterConventionArchitectureTest {

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
}
