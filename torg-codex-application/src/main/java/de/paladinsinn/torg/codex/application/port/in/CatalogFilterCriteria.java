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

import de.paladinsinn.torg.codex.domain.catalog.CatalogSlugs;
import de.paladinsinn.torg.codex.domain.catalog.GroupingDimension;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable filter, search, and sort criteria for browsing a catalog category (FR-004, FR-016).
 *
 * @param query     optional free-text substring search on entry name
 * @param cosms     requested cosm filter values
 * @param groupings requested structured facet filter values by {@link GroupingDimension}
 * @param sortKey   column sort key (defaults to {@code "name"})
 * @param direction sort direction (defaults to {@link SortDirection#ASC})
 */
public record CatalogFilterCriteria(
        String query,
        Set<String> cosms,
        Map<GroupingDimension, Set<String>> groupings,
        String sortKey,
        SortDirection direction) {

    /**
     * Table sort direction.
     */
    public enum SortDirection {
        ASC,
        DESC;

        public static SortDirection fromParam(final String raw) {
            return "desc".equalsIgnoreCase(raw == null ? "" : raw.trim()) ? DESC : ASC;
        }

        public String paramValue() {
            return name().toLowerCase(Locale.ROOT);
        }

        public SortDirection toggled() {
            return this == ASC ? DESC : ASC;
        }
    }

    public CatalogFilterCriteria {
        query = query == null ? "" : query.trim();
        cosms = normalizeSet(cosms);
        Map<GroupingDimension, Set<String>> normalizedGroupings = new EnumMap<>(GroupingDimension.class);
        if (groupings != null) {
            groupings.forEach((dim, vals) -> {
                if (dim != null) {
                    Set<String> clean = normalizeSet(vals);
                    if (!clean.isEmpty()) {
                        normalizedGroupings.put(dim, clean);
                    }
                }
            });
        }
        groupings = Map.copyOf(normalizedGroupings);
        sortKey = sortKey == null || sortKey.isBlank() ? "name" : sortKey.trim();
        direction = direction == null ? SortDirection.ASC : direction;
    }

    public static CatalogFilterCriteria empty() {
        return new CatalogFilterCriteria("", Set.of(), Map.of(), "name", SortDirection.ASC);
    }

    public boolean hasActiveFilters() {
        return !query.isEmpty() || !cosms.isEmpty() || !groupings.isEmpty();
    }

    private static Set<String> normalizeSet(final Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(CatalogSlugs::normalize)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
