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
 * Closed purchase-teaser view model for unowned single entries (FR-011, ADR-019).
 *
 * <p>Carries exclusively the entry's name, its category, and the unlocking publications—never
 * protected statistics, clearance, cosms, or prose.
 *
 * @param nav              shared global navigation model
 * @param area             entry catalog area
 * @param entryName        entry display name
 * @param requiredProducts sorted required products that unlock this entry
 */
public record TeaserViewModel(
        NavContextViewModel nav,
        CatalogArea area,
        String entryName,
        List<RequiredProductViewModel> requiredProducts) {
}
