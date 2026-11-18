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

import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import de.paladinsinn.torg.codex.domain.catalog.CatalogCategoryDescriptor;
import de.paladinsinn.torg.codex.domain.catalog.GroupingDimension;
import de.paladinsinn.torg.codex.domain.model.CatalogEntry;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Result of browsing a {@link CatalogArea} with {@link CatalogFilterCriteria}.
 *
 * @param area                      browsed catalog area
 * @param descriptor                metadata descriptor for the area
 * @param entries                   matching accessible entries with their resolved canonical slugs
 * @param totalAccessibleInCategory total accessible entries in this category before filtering
 * @param availableCosms            ordered cosm facet options (when Cosm facet is enabled)
 * @param availableFacets           ordered structured grouping facet options by dimension
 * @param appliedCriteria           normalized criteria applied to produce this result
 * @param publicationSelection      resolved publication selection state for this browse result
 */
public record CatalogBrowseResult(
        CatalogArea area,
        CatalogCategoryDescriptor<? extends CatalogEntry> descriptor,
        List<ResolvedEntry> entries,
        int totalAccessibleInCategory,
        List<FacetOption> availableCosms,
        Map<GroupingDimension, List<FacetOption>> availableFacets,
        CatalogFilterCriteria appliedCriteria,
        PublicationSelection publicationSelection) {

    public CatalogBrowseResult {
        publicationSelection = publicationSelection == null
                ? new PublicationSelection.None()
                : publicationSelection;
    }

    public CatalogBrowseResult(
            final CatalogArea area,
            final CatalogCategoryDescriptor<? extends CatalogEntry> descriptor,
            final List<ResolvedEntry> entries,
            final int totalAccessibleInCategory,
            final List<FacetOption> availableCosms,
            final Map<GroupingDimension, List<FacetOption>> availableFacets,
            final CatalogFilterCriteria appliedCriteria) {
        this(area, descriptor, entries, totalAccessibleInCategory, availableCosms, availableFacets,
                appliedCriteria, new PublicationSelection.None());
    }

    /**
     * Resolved publication filter selection for a browsed category.
     */
    public sealed interface PublicationSelection
            permits PublicationSelection.None, PublicationSelection.Selected, PublicationSelection.Unavailable {

        record None() implements PublicationSelection {
        }

        record Selected(UUID publicationId, String displayName) implements PublicationSelection {
            public Selected {
                Objects.requireNonNull(publicationId, "publicationId must not be null");
                displayName = displayName == null ? "" : displayName;
            }
        }

        record Unavailable() implements PublicationSelection {
        }
    }

    /**
     * Accessible entry paired with its collision-safe canonical slug within the area.
     *
     * @param entry         domain entry
     * @param canonicalSlug unique human-readable slug within the area
     */
    public record ResolvedEntry(CatalogEntry entry, String canonicalSlug) {
    }

    /**
     * Selectable structured filter option.
     *
     * @param key      normalized filter key
     * @param label    human-readable option label
     * @param selected whether currently active in {@link #appliedCriteria}
     */
    public record FacetOption(String key, String label, boolean selected) {
    }
}
