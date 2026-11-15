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

import com.tngtech.archunit.library.GeneralCodingRules;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces Web UI surface isolation (reader {@code web.codex} vs. reserved {@code web.admin}),
 * single-mechanism censoring in web adapters, and SLF4J-only logging (invariants W2–W4,
 * FR-018, FR-019, ADR-020, ADR-022).
 */
class WebUiSurfaceSeparationArchitectureTest {

    @Test
    void w2_readerSurfaceDoesNotDependOnAdminSurface() {
        noClasses()
                .that().resideInAnyPackage("de.paladinsinn.torg.codex.web.codex..")
                .should().dependOnClassesThat().resideInAnyPackage("de.paladinsinn.torg.codex.web.admin..")
                .as("W2: reader UI (web.codex) must not depend on the admin UI surface (web.admin)")
                .allowEmptyShould(true)
                .check(ArchitectureTestSupport.IMPORTED_CLASSES);
    }

    @Test
    void w2_adminSurfaceDoesNotDependOnReaderSurface() {
        noClasses()
                .that().resideInAnyPackage("de.paladinsinn.torg.codex.web.admin..")
                .should().dependOnClassesThat().resideInAnyPackage("de.paladinsinn.torg.codex.web.codex..")
                .as("W2: admin UI surface (web.admin) must not depend on the reader UI (web.codex)")
                .allowEmptyShould(true)
                .check(ArchitectureTestSupport.IMPORTED_CLASSES);
    }

    @Test
    void w3_webAdaptersDoNotTouchOwnershipResolverOrGrantedAuthorityDirectly() {
        noClasses()
                .that().resideInAnyPackage("de.paladinsinn.torg.codex.web..")
                .should().dependOnClassesThat().haveSimpleName("ProductOwnershipResolver")
                .orShould().dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.security.core.GrantedAuthority")
                .as("W3: web adapters must use CallerProductsPort / CurrentUserCensorFactory rather than "
                        + "ProductOwnershipResolver or GrantedAuthority directly")
                .allowEmptyShould(true)
                .check(ArchitectureTestSupport.IMPORTED_CLASSES);
    }

    @Test
    void w4_productionCodeUsesSlf4jOnlyWithoutStandardStreamsOrConcreteLogback() {
        noClasses()
                .that().resideInAnyPackage("de.paladinsinn..")
                .should().dependOnClassesThat().resideInAnyPackage("java.util.logging..", "ch.qos.logback..")
                .as("W4: production code must log exclusively via SLF4J (ADR-022)")
                .check(ArchitectureTestSupport.IMPORTED_CLASSES);

        noClasses()
                .that().resideInAnyPackage("de.paladinsinn..")
                .should(GeneralCodingRules.ACCESS_STANDARD_STREAMS)
                .as("W4: production code must not write to System.out or System.err (ADR-022)")
                .check(ArchitectureTestSupport.IMPORTED_CLASSES);
    }
}
