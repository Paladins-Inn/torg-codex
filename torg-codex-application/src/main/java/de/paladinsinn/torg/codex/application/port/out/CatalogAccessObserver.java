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

package de.paladinsinn.torg.codex.application.port.out;

import de.paladinsinn.torg.codex.domain.markup.EntityType;

import java.util.UUID;

/**
 * Driven port reporting catalog entries hidden because they have no product assignment
 * (ADR-019, FR-016/FR-017).
 *
 * <p>Called once per encounter, without deduplication across requests. Implementations must
 * not record caller identity, authorities, owned products or credentials.
 */
public interface CatalogAccessObserver {

    /**
     * Reports an entry without product assignment.
     *
     * @param area the catalog area of the entry
     * @param id   the entry id
     * @param name the entry display name
     */
    void entryWithoutProducts(EntityType area, UUID id, String name);
}
