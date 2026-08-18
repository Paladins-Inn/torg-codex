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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the freeze-list enforcement mechanism itself (T120): the loader must parse the
 * current {@code specs/architecture-migration/freeze-list.md} into well-formed entries, and
 * an entry's {@link FreezeListEntry#matches(String)} predicate must tolerate only violations
 * naming its exact registered violating class/dependency, rejecting every unlisted violation.
 *
 * <p>This is the safety net behind {@link ArchitectureTestSupport#assertNoUnfrozenViolations}:
 * every other architecture test in this package delegates unlisted-violation detection to that
 * method, so if the freeze-list matching logic itself were broken (e.g. matching too broadly),
 * every other architecture rule in this suite could silently stop enforcing anything.
 */
class FreezeListEnforcementArchitectureTest {

    @Test
    void loaderParsesTheCurrentFreezeListWithoutError() {
        List<FreezeListEntry> entries = FreezeListLoader.load();
        // The freeze list currently holds exactly the FL-007 accepted-deviation row; if new
        // rows are added, this assertion documents the expectation that they must still parse.
        assertTrue(entries.size() >= 1, "freeze-list.md must parse to at least the FL-007 entry");
        entries.forEach(entry -> {
            assertFalse(entry.id().isBlank(), "id must not be blank: " + entry);
            assertFalse(entry.violatedRule().isBlank(), "violatedRule must not be blank: " + entry);
            assertFalse(entry.status().isBlank(), "status must not be blank: " + entry);
        });
    }

    @Test
    void entryMatchesOnlyViolationsNamingItsRegisteredDependency() {
        FreezeListEntry entry = new FreezeListEntry(
                "FL-TEST", "test-module", "com.example.SomeSpecificClass",
                "some rule", "test rationale", "T000", "N/A", "accepted deviation");

        assertTrue(entry.matches(
                "Method <de.paladinsinn.torg.codex.Foo.bar()> calls method "
                        + "<com.example.SomeSpecificClass.baz()>"),
                "an entry must match a violation report that names its registered class");
    }

    @Test
    void entryDoesNotMatchAnUnlistedViolation() {
        FreezeListEntry entry = new FreezeListEntry(
                "FL-TEST", "test-module", "com.example.SomeSpecificClass",
                "some rule", "test rationale", "T000", "N/A", "accepted deviation");

        assertFalse(entry.matches(
                "Method <de.paladinsinn.torg.codex.Foo.bar()> calls method "
                        + "<org.springframework.completely.Unrelated.method()>"),
                "an entry must NOT match a violation naming a completely different dependency");
    }

    @Test
    void blankViolatingDependencyNeverMatchesAnything() {
        FreezeListEntry entry = new FreezeListEntry(
                "FL-TEST", "test-module", "",
                "some rule", "test rationale", "T000", "N/A", "accepted deviation");

        assertFalse(entry.matches("anything at all"),
                "a blank violating-class/dependency column must never match (fail closed)");
    }
}
