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

package de.paladinsinn.torg.codex.web.codex.model;

import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;

import java.util.List;

/**
 * View model for the landing overview page {@code GET /} (FR-001, FR-015).
 *
 * @param nav                 shared global navigation model
 * @param categories          all 17 catalog areas with accessible entry counts
 * @param ownedPublications   publications owned by the current caller
 * @param missingPublications unowned publications available for discovery
 * @param unlockedPercent     rounded percentage (0..100) of known publications owned
 */
public record HomeViewModel(
        NavContextViewModel nav,
        List<CategoryCard> categories,
        List<PublicationCard> ownedPublications,
        List<PublicationCard> missingPublications,
        int unlockedPercent) {

    /**
     * Summary card for one catalog area on the landing page.
     *
     * @param area            catalog area
     * @param accessibleCount count of entries accessible to the current caller
     */
    public record CategoryCard(CatalogArea area, int accessibleCount) {
    }

    /**
     * Summary card for an owned or missing publication on the landing page.
     *
     * @param codexId    publication codex id
     * @param name       publication title
     * @param detailPath codex publication detail URL
     * @param publisher  publisher or Infiniverse author label
     * @param coverUrl   DriveThruRPG cover image URL (nullable)
     * @param storeUrl   DriveThruRPG product page URL (nullable)
     */
    public record PublicationCard(
            String codexId,
            String name,
            String detailPath,
            String publisher,
            String coverUrl,
            String storeUrl) {
    }
}
