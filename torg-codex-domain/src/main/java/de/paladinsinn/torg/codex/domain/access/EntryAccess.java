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

package de.paladinsinn.torg.codex.domain.access;

import de.paladinsinn.torg.codex.domain.markup.EntityType;
import de.paladinsinn.torg.codex.domain.model.ProductBoundEntry;

import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

/**
 * The result of reading a single product-bound catalog entry (ADR-019).
 *
 * @param <T> the entry type
 */
public sealed interface EntryAccess<T extends ProductBoundEntry> {

    /**
     * Creates the result for an accessible entry.
     *
     * @param entry the accessible entry
     * @param <T>   the entry type
     * @return the granted result
     */
    static <T extends ProductBoundEntry> EntryAccess<T> granted(final T entry) {
        return new Granted<>(entry);
    }

    /**
     * Creates the purchase teaser for an entry the caller does not own.
     *
     * @param entry the entry; its products must not be empty
     * @param <T>   the entry type
     * @return the teaser carrying only id, name, area and the sorted required products
     */
    static <T extends ProductBoundEntry> EntryAccess<T> teaserOf(final T entry) {
        Objects.requireNonNull(entry, "entry");
        return new Teaser<>(entry.id(), entry.name(), entry.entityType(), new TreeSet<>(OwnedProducts.normalize(entry.products())));
    }

    /**
     * Creates the result for a missing or unassigned entry; both are indistinguishable.
     *
     * @param <T> the entry type
     * @return the absent result
     */
    static <T extends ProductBoundEntry> EntryAccess<T> absent() {
        return new Absent<>();
    }

    /**
     * The caller may read the entry.
     *
     * @param entry the accessible entry
     * @param <T>   the entry type
     */
    record Granted<T extends ProductBoundEntry>(T entry) implements EntryAccess<T> {

        /**
         * Validates the entry.
         */
        public Granted {
            Objects.requireNonNull(entry, "entry");
        }
    }

    /**
     * The entry exists but the caller owns none of its products.
     *
     * @param id               the entry id
     * @param name             the entry display name
     * @param type             the catalog area of the entry
     * @param requiredProducts the codex ids that grant access, sorted ascending; never empty
     * @param <T>              the entry type
     */
    record Teaser<T extends ProductBoundEntry>(UUID id, String name, EntityType type, SortedSet<String> requiredProducts)
            implements EntryAccess<T> {

        /**
         * Validates the teaser and makes the required products immutable.
         */
        public Teaser {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(type, "type");
            if (requiredProducts == null || requiredProducts.isEmpty()) {
                throw new IllegalArgumentException("A teaser requires at least one granting product");
            }
            requiredProducts = Collections.unmodifiableSortedSet(new TreeSet<>(requiredProducts));
        }
    }

    /**
     * The entry does not exist or has no product assignment.
     *
     * @param <T> the entry type
     */
    record Absent<T extends ProductBoundEntry>() implements EntryAccess<T> {
    }
}
