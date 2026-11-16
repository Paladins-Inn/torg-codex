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

import java.util.List;
import java.util.Objects;

/**
 * Single sequential page batch of owned DriveThruRPG product identifiers delivered during library streaming.
 *
 * @param pageNumber 1-based upstream page index
 * @param productIds immutable list of positive product IDs from this page
 * @param customerId first non-null upstream customer ID observed on this page, or {@code null}
 */
public record DriveThruLibraryBatch(
        int pageNumber,
        List<Integer> productIds,
        String customerId) {

    /**
     * Enforces page index and immutable positive product list invariants.
     *
     * @param pageNumber 1-based page number
     * @param productIds positive product IDs
     * @param customerId optional customer ID
     */
    public DriveThruLibraryBatch {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be >= 1.");
        }
        Objects.requireNonNull(productIds, "productIds");
        productIds = productIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
    }
}
