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
import de.paladinsinn.torg.codex.domain.model.ClearanceLevel;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Metadata descriptor for one summary column in a generic catalog category table (FR-003, FR-016).
 *
 * @param key           stable sort/column identifier
 * @param header        human-readable column header
 * @param kind          rendering presentation kind
 * @param sortable      whether the table can be sorted by this column
 * @param textExtractor primary scalar value extractor
 * @param listExtractor multi-value badge extractor (used for cosm badge lists)
 * @param <E>           the catalog entry type
 */
public record ColumnDescriptor<E extends CatalogEntry>(
        String key,
        String header,
        CellKind kind,
        boolean sortable,
        Function<E, String> textExtractor,
        Function<E, List<String>> listExtractor) {

    /**
     * Presentation kind for generic table cells.
     */
    public enum CellKind {
        PRIMARY_LINK,
        TEXT,
        MARKUP,
        CLEARANCE_BADGE,
        COSM_BADGES,
        GROUP_BADGE,
        BOOLEAN_FLAG,
        EXTERNAL_LINK
    }

    public static <E extends CatalogEntry> ColumnDescriptor<E> primaryName() {
        return new ColumnDescriptor<>("name", "Name", CellKind.PRIMARY_LINK, true,
                CatalogEntry::name, e -> List.of());
    }

    public static <E extends CatalogEntry> ColumnDescriptor<E> clearance() {
        return new ColumnDescriptor<>("clearance", "Clearance", CellKind.CLEARANCE_BADGE, true,
                e -> e.clearance().map(ClearanceLevel::getSymbol).orElse(""),
                e -> e.clearance().map(c -> List.of(c.getFullName())).orElse(List.of()));
    }

    public static <E extends CatalogEntry> ColumnDescriptor<E> cosms() {
        return new ColumnDescriptor<>("cosms", "Cosm", CellKind.COSM_BADGES, true,
                e -> String.join(", ", e.cosmSlugs().stream().sorted().toList()),
                e -> e.cosmSlugs().stream().sorted().toList());
    }

    public static <E extends CatalogEntry> ColumnDescriptor<E> text(
            final String key,
            final String header,
            final boolean sortable,
            final Function<E, String> extractor) {
        return new ColumnDescriptor<>(key, header, CellKind.TEXT, sortable,
                e -> nullSafe(extractor.apply(e)), e -> List.of());
    }

    public static <E extends CatalogEntry> ColumnDescriptor<E> markup(
            final String key,
            final String header,
            final Function<E, String> extractor) {
        return new ColumnDescriptor<>(key, header, CellKind.MARKUP, false,
                e -> nullSafe(extractor.apply(e)), e -> List.of());
    }

    public static <E extends CatalogEntry> ColumnDescriptor<E> groupBadge(
            final String key,
            final String header,
            final boolean sortable,
            final Function<E, String> extractor) {
        return new ColumnDescriptor<>(key, header, CellKind.GROUP_BADGE, sortable,
                e -> nullSafe(extractor.apply(e)), e -> List.of());
    }

    public static <E extends CatalogEntry> ColumnDescriptor<E> flag(
            final String key,
            final String header,
            final Predicate<E> predicate) {
        return new ColumnDescriptor<>(key, header, CellKind.BOOLEAN_FLAG, true,
                e -> predicate.test(e) ? "Yes" : "", e -> List.of());
    }

    public static <E extends CatalogEntry> ColumnDescriptor<E> externalLink(
            final String key,
            final String header,
            final Function<E, String> urlExtractor) {
        return new ColumnDescriptor<>(key, header, CellKind.EXTERNAL_LINK, false,
                e -> nullSafe(urlExtractor.apply(e)), e -> List.of());
    }

    private static String nullSafe(final String value) {
        return value == null ? "" : value;
    }
}
