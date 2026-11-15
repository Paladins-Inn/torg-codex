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
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Ordered section descriptor driving generic single-element detail rendering across all 17 catalog
 * areas without per-category templates (FR-007, FR-016).
 *
 * @param <E> the catalog entry type
 */
public sealed interface DetailSectionDescriptor<E extends CatalogEntry>
        permits DetailSectionDescriptor.Attributes,
                DetailSectionDescriptor.Prose,
                DetailSectionDescriptor.ProseMap,
                DetailSectionDescriptor.EmbeddedReferences {

    String title();

    /**
     * Labeled scalar attribute inside an {@link Attributes} section.
     *
     * @param label           display label
     * @param valueExtractor  raw or formatted string extractor (empty/null values are skipped)
     * @param renderAsMarkup  when {@code true}, value passes through {@code Censor} before rendering
     * @param <E>             the catalog entry type
     */
    record AttributeField<E extends CatalogEntry>(
            String label,
            Function<E, String> valueExtractor,
            boolean renderAsMarkup) {

        public static <E extends CatalogEntry> AttributeField<E> plain(
                final String label, final Function<E, String> extractor) {
            return new AttributeField<>(label, extractor, false);
        }

        public static <E extends CatalogEntry> AttributeField<E> markup(
                final String label, final Function<E, String> extractor) {
            return new AttributeField<>(label, extractor, true);
        }
    }

    /**
     * Key-value attribute/statistics card section.
     *
     * @param title  section heading
     * @param fields ordered attribute extractors
     * @param <E>    the catalog entry type
     */
    record Attributes<E extends CatalogEntry>(
            String title,
            List<AttributeField<E>> fields) implements DetailSectionDescriptor<E> {
    }

    /**
     * Rich-text prose section whose raw Torg markup is censored and rendered via {@code Censor}.
     *
     * @param title              section heading (empty string for primary body prose)
     * @param rawMarkupExtractor raw Torg markup extractor
     * @param <E>                the catalog entry type
     */
    record Prose<E extends CatalogEntry>(
            String title,
            Function<E, String> rawMarkupExtractor) implements DetailSectionDescriptor<E> {
    }

    /**
     * Named map of rich-text prose entries (such as Threat special abilities).
     *
     * @param title                 section heading
     * @param rawMarkupMapExtractor ordered map extractor (ability name -> raw markup)
     * @param <E>                   the catalog entry type
     */
    record ProseMap<E extends CatalogEntry>(
            String title,
            Function<E, Map<String, String>> rawMarkupMapExtractor) implements DetailSectionDescriptor<E> {
    }

    /**
     * Navigable list of referenced catalog entries (such as spells in a spell list or weaponry items).
     *
     * @param title        section heading
     * @param targetArea   referenced catalog area
     * @param idsExtractor referenced UUID list extractor
     * @param <E>          the catalog entry type
     */
    record EmbeddedReferences<E extends CatalogEntry>(
            String title,
            CatalogArea targetArea,
            Function<E, List<UUID>> idsExtractor) implements DetailSectionDescriptor<E> {
    }
}
