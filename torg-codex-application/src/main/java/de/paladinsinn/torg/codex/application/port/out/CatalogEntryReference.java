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
import de.paladinsinn.torg.codex.domain.model.ProductBoundEntry;

import java.util.Set;
import java.util.UUID;

/**
 * Lightweight projection of a referenced cosm including its product assignment, so reference
 * resolution can hide cosms without products (ADR-019, FR-008/FR-009).
 *
 * @param id       the cosm id
 * @param name     the cosm display name
 * @param products the codex ids of the products the cosm is assigned to
 */
public record CatalogEntryReference(UUID id, String name, Set<String> products) implements ProductBoundEntry {

    @Override
    public EntityType entityType() {
        return EntityType.COSM;
    }
}
