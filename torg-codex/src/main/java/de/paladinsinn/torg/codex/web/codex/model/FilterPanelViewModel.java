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

import de.paladinsinn.torg.codex.application.port.in.CatalogBrowseResult.FacetOption;
import de.paladinsinn.torg.codex.domain.catalog.GroupingDimension;

import java.util.List;

/**
 * View model for the htmx-driven structured filter panel on a category table page (FR-004).
 *
 * @param actionPath       canonical category path (e.g. {@code /perks})
 * @param searchQuery      current free-text search string
 * @param cosms            cosm facet options (empty when Cosm facet is not applicable)
 * @param groupingFacets   ordered grouping facet groups
 * @param sortKey              active sort column key
 * @param sortDir              active sort direction parameter ({@code asc} or {@code desc})
 * @param hasActiveFilters     whether any filter or search criterion is currently active
 * @param publicationParam     safe {@code publication} query token (lowercase UUID, {@code ""}, or {@code null})
 * @param clearPublicationUrl  full-page URL removing only {@code publication} (nullable when none)
 */
public record FilterPanelViewModel(
        String actionPath,
        String searchQuery,
        List<FacetOption> cosms,
        List<GroupingFacetGroup> groupingFacets,
        String sortKey,
        String sortDir,
        boolean hasActiveFilters,
        String publicationParam,
        String clearPublicationUrl) {

    public FilterPanelViewModel(
            final String actionPath,
            final String searchQuery,
            final List<FacetOption> cosms,
            final List<GroupingFacetGroup> groupingFacets,
            final String sortKey,
            final String sortDir,
            final boolean hasActiveFilters) {
        this(actionPath, searchQuery, cosms, groupingFacets, sortKey, sortDir, hasActiveFilters, null, null);
    }

    /**
     * A single structured grouping facet control group.
     *
     * @param dimension  grouping dimension
     * @param queryParam URL query parameter name
     * @param label      display heading
     * @param options    selectable facet options
     */
    public record GroupingFacetGroup(
            GroupingDimension dimension,
            String queryParam,
            String label,
            List<FacetOption> options) {
    }
}
