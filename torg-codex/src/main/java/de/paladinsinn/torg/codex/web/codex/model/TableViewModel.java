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
 * Complete view model for both full-page category tables ({@code codex/pages/category.jte}) and
 * htmx partial table updates ({@code codex/fragments/categoryResults.jte}).
 *
 * @param nav                  shared global navigation model
 * @param area                 browsed catalog area
 * @param filterPanel          filter form state and facet options
 * @param headers              table column headers with sort URLs
 * @param rows                 matching accessible table rows
 * @param matchingCount         count of rows matching current criteria
 * @param totalAccessibleCount  count of accessible entries in this category before filtering
 * @param publicationFilter     active publication filter display state (nullable when none)
 */
public record TableViewModel(
        NavContextViewModel nav,
        CatalogArea area,
        FilterPanelViewModel filterPanel,
        List<ColumnHeaderViewModel> headers,
        List<TableRowViewModel> rows,
        int matchingCount,
        int totalAccessibleCount,
        PublicationFilterStatus publicationFilter) {

    public TableViewModel(
            final NavContextViewModel nav,
            final CatalogArea area,
            final FilterPanelViewModel filterPanel,
            final List<ColumnHeaderViewModel> headers,
            final List<TableRowViewModel> rows,
            final int matchingCount,
            final int totalAccessibleCount) {
        this(nav, area, filterPanel, headers, rows, matchingCount, totalAccessibleCount, null);
    }

    /**
     * Active publication filter status shown in the category results header.
     *
     * @param displayName resolved publication name when available (nullable when unavailable)
     * @param unavailable {@code true} when the requested publication is unknown or invalid
     */
    public record PublicationFilterStatus(String displayName, boolean unavailable) {
    }

    /**
     * Column header view model supporting htmx/bookmarkable column sorting.
     *
     * @param key        column key
     * @param label      display header
     * @param sortable   whether clicking sorts by this column
     * @param activeSort whether currently sorted by this column
     * @param ascending  whether current sort is ascending
     * @param sortUrl    bookmarkable URL toggling/activating sort on this column
     */
    public record ColumnHeaderViewModel(
            String key,
            String label,
            boolean sortable,
            boolean activeSort,
            boolean ascending,
            String sortUrl) {
    }
}
