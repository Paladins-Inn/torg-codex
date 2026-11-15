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

package de.paladinsinn.torg.codex.characterization;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Detects the separately delivered (proprietary, git-ignored) catalog data. Tests that replay
 * fixtures captured from it are skipped when it is absent, e.g. in public CI (constitution:
 * proprietary fixtures). Behaviour that must also be proven in public CI uses seeded data instead.
 */
final class ProprietaryCatalogData {

    /** The Liquibase changelog that loads the proprietary catalog CSV files. */
    static final String LOAD_CHANGELOG = "db/load/torg-data-load.yaml";

    private ProprietaryCatalogData() {
    }

    static boolean present() {
        return ProprietaryCatalogData.class.getClassLoader().getResource(LOAD_CHANGELOG) != null;
    }

    static void assumePresent() {
        assumeTrue(present(), "proprietary catalog data (" + LOAD_CHANGELOG + ") is not available");
    }
}
