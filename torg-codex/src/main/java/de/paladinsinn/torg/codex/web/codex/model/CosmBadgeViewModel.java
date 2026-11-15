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

import de.paladinsinn.torg.codex.domain.catalog.CatalogSlugs;
import de.paladinsinn.torg.codex.domain.markup.EntityType;

import java.util.Set;

/**
 * View model for a styled Torg Eternity Cosm badge.
 *
 * @param slug     normalized cosm slug
 * @param label    display name
 * @param cssClass Bootstrap / codex theme CSS class
 */
public record CosmBadgeViewModel(String slug, String label, String cssClass) {

    private static final Set<String> KNOWN_THEMED_COSMS = Set.of(
            "aysle", "core-earth", "cyberpapacy", "living-land", "nile-empire",
            "orrorsh", "pan-pacifica", "tharkold", "universal", "unknown");

    public static CosmBadgeViewModel of(final String rawSlug, final String explicitLabel) {
        String normalized = CatalogSlugs.normalize(rawSlug);
        String label = explicitLabel != null && !explicitLabel.isBlank()
                ? explicitLabel
                : EntityType.toDisplayName(normalized);
        String themeSuffix = KNOWN_THEMED_COSMS.contains(normalized) ? normalized : "unknown";
        return new CosmBadgeViewModel(normalized, label, "badge badge-cosm badge-cosm-" + themeSuffix);
    }
}
