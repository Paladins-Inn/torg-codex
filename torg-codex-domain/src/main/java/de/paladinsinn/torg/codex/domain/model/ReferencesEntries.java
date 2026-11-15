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

import java.util.List;
import java.util.UUID;

/**
 * A catalog entry embedding references to entries of another catalog area (ADR-019).
 *
 * <p>Restricted reads remove references to entries the caller may not read, keeping the order of
 * the remaining references.
 *
 * @param <SELF> the implementing type, returned by {@link #withReferencedEntries(List)}
 */
public interface ReferencesEntries<SELF extends ReferencesEntries<SELF>> {

    /**
     * Returns the catalog area of the referenced entries.
     *
     * @return the referenced area
     */
    EntityType referencedType();

    /**
     * Returns the ids of the referenced entries in their original order.
     *
     * @return the referenced ids; empty if there are none
     */
    List<UUID> referencedEntries();

    /**
     * Returns a copy of this entry with the given references and every other field unchanged.
     *
     * @param referencedEntries the references of the copy
     * @return the copy
     */
    SELF withReferencedEntries(List<UUID> referencedEntries);
}
