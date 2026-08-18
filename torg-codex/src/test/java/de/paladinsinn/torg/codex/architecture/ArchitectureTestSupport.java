/*
 * Copyright (c) 2026.  Roland T. Lichti <rlichti@kaiserpfalz-edv.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * ERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * You may contact me via email rlichti@kaiserpfalz-edv.de or via mail
 *
 * Kaiserpfalz EDV-Service
 * Roland T. Lichti
 * Darmstädter Str. 12
 * 64625 Bensheim
 * GERMANY
 */

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
