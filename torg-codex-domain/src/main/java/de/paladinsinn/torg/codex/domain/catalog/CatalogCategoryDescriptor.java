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

package de.paladinsinn.torg.codex.domain.catalog;

import de.paladinsinn.torg.codex.domain.model.CatalogEntry;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Complete metadata descriptor of a single {@link CatalogArea}, driving generic tabular columns,
 * structured filter facets, and detail sections (FR-016).
 *
 * @param area                    the catalog area
 * @param entryClass              concrete domain record class
 * @param columns                 ordered tabular summary columns
 * @param facets                  structured filter facets enabled on the category table
 * @param detailSections          ordered detail view sections
 * @param directGroupingExtractor extracts normalized grouping keys directly present on an entry
 * @param <E>                     the catalog entry type
 */
public record CatalogCategoryDescriptor<E extends CatalogEntry>(
        CatalogArea area,
        Class<E> entryClass,
        List<ColumnDescriptor<E>> columns,
        List<FilterFacetDescriptor> facets,
        List<DetailSectionDescriptor<E>> detailSections,
        BiFunction<E, GroupingDimension, Set<String>> directGroupingExtractor) {

    public CatalogCategoryDescriptor {
        columns = List.copyOf(columns);
        facets = List.copyOf(facets);
        detailSections = List.copyOf(detailSections);
    }

    public Set<String> extractDirectGroupingKeys(final E entry, final GroupingDimension dimension) {
        if (dimension == GroupingDimension.CLEARANCE) {
            return entry.clearance().map(c -> Set.of(c.getFullName())).orElse(Set.of());
        }
        return directGroupingExtractor == null ? Set.of() : directGroupingExtractor.apply(entry, dimension);
    }
}
