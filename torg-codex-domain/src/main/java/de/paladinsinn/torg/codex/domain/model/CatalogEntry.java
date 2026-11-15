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

package de.paladinsinn.torg.codex.domain.model;

import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import de.paladinsinn.torg.codex.domain.catalog.CatalogSlugs;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Common core concept implemented by all 17 catalog entry types (FR-016).
 *
 * <p>Provides uniform identity ({@link #id()} and {@link #slug()}), display name,
 * {@link CatalogArea}, clearance, cosm memberships, and source product codex IDs
 * so generic browse, filter, detail, teaser, and metric components operate without
 * per-category duplication.
 */
public interface CatalogEntry {

    UUID id();

    String name();

    CatalogArea catalogArea();

    default String slug() {
        return CatalogSlugs.normalize(name());
    }

    default Optional<ClearanceLevel> clearance() {
        return Optional.empty();
    }

    default Set<String> cosmSlugs() {
        return Set.of();
    }

    default Set<String> productCodexIds() {
        return Set.of();
    }

    static Set<String> normalizeSlugs(final Collection<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Set.of();
        }
        return raw.stream()
                .filter(Objects::nonNull)
                .map(CatalogSlugs::normalize)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
