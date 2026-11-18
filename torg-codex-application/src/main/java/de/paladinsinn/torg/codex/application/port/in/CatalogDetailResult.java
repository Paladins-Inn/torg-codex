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

import de.paladinsinn.torg.codex.domain.access.EntryAccess;
import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import de.paladinsinn.torg.codex.domain.catalog.CatalogCategoryDescriptor;
import de.paladinsinn.torg.codex.domain.model.CatalogEntry;
import de.paladinsinn.torg.codex.domain.model.ProductBoundEntry;
import de.paladinsinn.torg.codex.domain.model.Publication;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Result of looking up a single entry's detail view across any of the 17 catalog areas.
 */
public sealed interface CatalogDetailResult
        permits CatalogDetailResult.GrantedProductEntry,
                CatalogDetailResult.PublicReferenceEntry,
                CatalogDetailResult.RestrictedTeaser,
                CatalogDetailResult.Absent {

    CatalogArea area();

    /**
     * Resolved accessible reference to another catalog entry (for embedded lists).
     *
     * @param id            referenced entry UUID
     * @param name          display name
     * @param canonicalSlug canonical slug within {@code targetArea}
     * @param targetArea    referenced catalog area
     */
    record LinkedEntryRef(UUID id, String name, String canonicalSlug, CatalogArea targetArea) {
        public String detailPath() {
            return targetArea.detailPath(canonicalSlug);
        }
    }

    /**
     * Required product reference for a purchase teaser, paired with publication metadata if known.
     *
     * @param codexId     required product codex id
     * @param publication matching {@link Publication}, if registered in the catalog
     */
    record RequiredProductRef(String codexId, Optional<Publication> publication) {
    }

    /**
     * Accessible product-bound entry detail.
     *
     * @param area               catalog area
     * @param descriptor         category metadata descriptor
     * @param entry              granted entry
     * @param canonicalSlug      canonical slug
     * @param cosmLabels         resolved cosm slug -> display name map
     * @param embeddedReferences section title -> resolved accessible linked entries
     * @param publications       resolved source publications
     */
    record GrantedProductEntry(
            CatalogArea area,
            CatalogCategoryDescriptor<? extends CatalogEntry> descriptor,
            ProductBoundEntry entry,
            String canonicalSlug,
            Map<String, String> cosmLabels,
            Map<String, List<LinkedEntryRef>> embeddedReferences,
            List<Publication> publications) implements CatalogDetailResult {
    }

    /**
     * Distinct accessible entry count for one product-bound category on a publication detail page.
     *
     * @param area  product-bound catalog area
     * @param count non-negative distinct entry count
     */
    record PublicationContentCount(CatalogArea area, int count) {
        public PublicationContentCount {
            if (area == null || !area.isProductBound()) {
                throw new IllegalArgumentException("Publication content count area must be product-bound: " + area);
            }
            if (count < 0) {
                throw new IllegalArgumentException("Publication content count must be non-negative: " + count);
            }
        }
    }

    /**
     * Accessible public reference entry detail (Article, Publication, Tag).
     *
     * @param area               catalog area
     * @param descriptor         category metadata descriptor
     * @param entry              reference entry
     * @param canonicalSlug      canonical slug
     * @param embeddedReferences section title -> resolved linked entries (e.g., parent tag)
     * @param contentCounts      ordered product-bound category counts for publication details (empty otherwise)
     */
    record PublicReferenceEntry(
            CatalogArea area,
            CatalogCategoryDescriptor<? extends CatalogEntry> descriptor,
            CatalogEntry entry,
            String canonicalSlug,
            Map<String, List<LinkedEntryRef>> embeddedReferences,
            List<PublicationContentCount> contentCounts) implements CatalogDetailResult {

        public PublicReferenceEntry {
            contentCounts = contentCounts == null ? List.of() : List.copyOf(contentCounts);
        }

        public PublicReferenceEntry(
                final CatalogArea area,
                final CatalogCategoryDescriptor<? extends CatalogEntry> descriptor,
                final CatalogEntry entry,
                final String canonicalSlug,
                final Map<String, List<LinkedEntryRef>> embeddedReferences) {
            this(area, descriptor, entry, canonicalSlug, embeddedReferences, List.of());
        }
    }

    /**
     * Unowned product-bound entry purchase teaser.
     *
     * @param area             catalog area
     * @param teaser           domain teaser (id, name, type, requiredProducts)
     * @param requiredProducts resolved required products with optional publication metadata
     */
    record RestrictedTeaser(
            CatalogArea area,
            EntryAccess.Teaser<?> teaser,
            List<RequiredProductRef> requiredProducts) implements CatalogDetailResult {
    }

    /**
     * Entry does not exist or is unassigned (fail-closed).
     *
     * @param area catalog area
     */
    record Absent(CatalogArea area) implements CatalogDetailResult {
    }
}
