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

import de.paladinsinn.torg.codex.domain.catalog.ColumnDescriptor.CellKind;

import java.util.List;

/**
 * View model for a single cell inside a generic catalog table row (FR-003, FR-016).
 *
 * @param kind       cell presentation kind
 * @param text       display text (symbol for clearance, label for primary link)
 * @param href       link target (detail URL for {@code PRIMARY_LINK}, external URL for {@code EXTERNAL_LINK})
 * @param cosmBadges styled cosm badges when {@code kind == COSM_BADGES}
 * @param markupValue censored HTML when {@code kind == MARKUP}
 */
public record TableCellViewModel(
        CellKind kind,
        String text,
        String href,
        List<CosmBadgeViewModel> cosmBadges,
        SanitizedHtml markupValue) {
}
