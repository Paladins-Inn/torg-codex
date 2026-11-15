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

package de.paladinsinn.torg.codex.domain.model;

import de.paladinsinn.torg.codex.domain.markup.EntityType;

import java.util.Set;
import java.util.UUID;

/**
 * A catalog entry whose access is bound to the products (codex ids) it is assigned to (ADR-019).
 *
 * <p>Implemented by the records of the 14 product-bound catalog areas. Reference data (articles,
 * publications and tags) is not product-bound.
 */
public interface ProductBoundEntry {

    /**
     * Returns the stable identifier of the entry.
     *
     * @return the entry id
     */
    UUID id();

    /**
     * Returns the display name of the entry.
     *
     * @return the entry name
     */
    String name();

    /**
     * Returns the codex ids of the products the entry is assigned to.
     *
     * @return the assigned products; {@code null} or empty means the entry is unassigned
     */
    Set<String> products();

    /**
     * Returns the catalog area of the entry.
     *
     * @return the catalog area
     */
    EntityType entityType();
}
