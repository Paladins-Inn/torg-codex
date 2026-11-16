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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
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

package de.paladinsinn.drivethru;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Complete, immutable, token-free snapshot of a DriveThruRPG library lookup suitable for caching.
 *
 * @param productIds deterministically ordered, deduplicated positive DriveThruRPG product IDs
 * @param customerId upstream customer ID, or {@code null} when the library is empty
 * @param loadedAt   completion timestamp
 */
public record DriveThruLibrarySnapshot(
        List<Integer> productIds,
        String customerId,
        Instant loadedAt) implements Serializable {

    /**
     * Enforces an immutable, deduplicated positive product list and non-null timestamp.
     *
     * @param productIds positive product IDs
     * @param customerId optional customer ID
     * @param loadedAt   completion timestamp
     */
    public DriveThruLibrarySnapshot {
        Objects.requireNonNull(productIds, "productIds");
        Objects.requireNonNull(loadedAt, "loadedAt");
        LinkedHashSet<Integer> deduplicated = new LinkedHashSet<>();
        for (Integer id : productIds) {
            if (id != null && id > 0) {
                deduplicated.add(id);
            }
        }
        productIds = List.copyOf(deduplicated);
    }

    /**
     * Builds an immutable deduplicated snapshot from collected product IDs.
     *
     * @param productIds collected product IDs
     * @param customerId resolved customer ID, or {@code null}
     * @param loadedAt   completion instant
     * @return immutable snapshot
     */
    public static DriveThruLibrarySnapshot of(
            final Collection<Integer> productIds,
            final String customerId,
            final Instant loadedAt) {
        return new DriveThruLibrarySnapshot(
                productIds == null ? List.of() : List.copyOf(productIds),
                customerId,
                loadedAt);
    }
}
