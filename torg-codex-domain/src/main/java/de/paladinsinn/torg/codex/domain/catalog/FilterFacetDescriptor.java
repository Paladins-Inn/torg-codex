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

/**
 * Declares a structured filter facet enabled for a {@link CatalogArea} table view (FR-004, FR-016).
 *
 * @param cosmFacet {@code true} when this facet filters by Cosm slug; {@code false} for a {@link GroupingDimension}
 * @param dimension the grouping dimension when {@code cosmFacet} is {@code false}; {@code null} for Cosm
 */
public record FilterFacetDescriptor(boolean cosmFacet, GroupingDimension dimension) {

    public static FilterFacetDescriptor cosm() {
        return new FilterFacetDescriptor(true, null);
    }

    public static FilterFacetDescriptor grouping(final GroupingDimension dimension) {
        return new FilterFacetDescriptor(false, dimension);
    }

    public String queryParam() {
        return cosmFacet ? "cosm" : dimension.getQueryParam();
    }

    public String label() {
        return cosmFacet ? "Cosm" : dimension.getDisplayName();
    }
}
