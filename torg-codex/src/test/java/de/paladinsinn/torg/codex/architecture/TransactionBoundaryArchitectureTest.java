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
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces that transaction demarcation lives exclusively at the inbound-adapter/composition
 * boundary (e.g. {@code @RestController} classes in {@code torg-codex}), never inside the
 * framework-independent {@code torg-codex-application} or {@code torg-codex-domain} modules.
 *
 * <p>See Phase 4d, task T117 (split into T117a-T117q per catalog area) for the relocation of
 * {@code @Transactional} to each catalog area's controller.
 */
class TransactionBoundaryArchitectureTest {

    @Test
    void applicationContainsNoTransactionalAnnotations() {
        ArchitectureTestSupport.assertNoUnfrozenViolations(noClasses()
                .that().resideInAnyPackage("de.paladinsinn.torg.codex.application..")
                .should().beAnnotatedWith(Transactional.class)
                .as("torg-codex-application must not declare @Transactional")
                .allowEmptyShould(true));
    }

    @Test
    void domainContainsNoTransactionalAnnotations() {
        ArchitectureTestSupport.assertNoUnfrozenViolations(noClasses()
                .that().resideInAnyPackage("de.paladinsinn.torg.codex.domain..")
                .should().beAnnotatedWith(Transactional.class)
                .as("torg-codex-domain must not declare @Transactional")
                .allowEmptyShould(true));
    }

    @Test
    void noMethodInApplicationOrDomainIsAnnotatedTransactional() {
        ArchitectureTestSupport.assertNoUnfrozenViolations(
                com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods()
                        .that().areDeclaredInClassesThat().resideInAnyPackage(
                                "de.paladinsinn.torg.codex.application..",
                                "de.paladinsinn.torg.codex.domain..")
                        .should().beAnnotatedWith(Transactional.class)
                        .as("no method in torg-codex-application or torg-codex-domain may declare @Transactional")
                        .allowEmptyShould(true));
    }
}
