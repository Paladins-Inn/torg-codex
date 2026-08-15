package de.paladinsinn.torg.codex.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import org.junit.jupiter.api.Assertions;

import java.util.List;

final class ArchitectureTestSupport {
    static final JavaClasses IMPORTED_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(
                    "de.paladinsinn.torg.codex",
                    "de.paladinsinn.drivethru",
                    "de.paladinsinn.security",
                    "de.kaiserpfalz.liquibase");

    private ArchitectureTestSupport() {
    }

    static void assertNoUnfrozenViolations(ArchRule rule) {
        EvaluationResult result = rule.evaluate(IMPORTED_CLASSES);
        List<FreezeListEntry> freezeList = FreezeListLoader.load();
        List<String> unexpectedViolations = result.getFailureReport().getDetails().stream()
                .filter(violation -> freezeList.stream().noneMatch(entry -> entry.matches(violation)))
                .toList();

        if (!unexpectedViolations.isEmpty()) {
            Assertions.fail(rule.getDescription() + System.lineSeparator() + String.join(System.lineSeparator(), unexpectedViolations));
        }
    }
}
