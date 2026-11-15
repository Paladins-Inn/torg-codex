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

package de.paladinsinn.torg.codex.application.port.in;

import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import de.paladinsinn.torg.codex.domain.model.Publication;

import java.util.List;

/**
 * Landing-page overview summary containing accessible category counts and publication unlock
 * progress (FR-015).
 *
 * @param categories           all 17 categories with accessible entry counts
 * @param ownedPublications    publications currently owned by the caller (including free tier)
 * @param missingPublications  known publications not currently owned by the caller
 * @param unlockedPercentage   percentage (0..100) of known publications owned
 */
public record CodexHomeOverview(
        List<CategorySummary> categories,
        List<Publication> ownedPublications,
        List<Publication> missingPublications,
        double unlockedPercentage) {

    /**
     * Accessible entry count for a single {@link CatalogArea}.
     *
     * @param area            catalog area
     * @param accessibleCount number of entries visible to the current caller
     */
    public record CategorySummary(CatalogArea area, int accessibleCount) {
    }
}
