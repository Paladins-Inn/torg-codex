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
import java.util.Map;

/**
 * Generic view model for rendering the complete detailed view of any accessible catalog entry
 * across all 17 categories (FR-007, FR-016).
 *
 * @param nav             shared global navigation model
 * @param area            catalog area
 * @param header          entry title, canonical URL, clearance, and cosm badges
 * @param attributeGroups structured statistics and attribute cards
 * @param proseSections   censored and sanitized rich-text prose blocks
 * @param embeddedGroups  titled lists of accessible referenced entries
 * @param publications    source publication attribution links
 */
public record DetailViewModel(
        NavContextViewModel nav,
        CatalogArea area,
        DetailHeader header,
        List<AttributeGroupViewModel> attributeGroups,
        List<RenderedProseSection> proseSections,
        Map<String, List<LinkedEntryViewModel>> embeddedGroups,
        List<PublicationLinkViewModel> publications) {

    /**
     * Header metadata of a detail page.
     *
     * @param name            entry display name
     * @param canonicalPath   canonical slug-based detail URL
     * @param clearanceSymbol Greek clearance symbol (nullable)
     * @param clearanceName   clearance full name (nullable)
     * @param cosms           cosm badges
     */
    public record DetailHeader(
            String name,
            String canonicalPath,
            String clearanceSymbol,
            String clearanceName,
            List<CosmBadgeViewModel> cosms) {
    }
}
