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

package de.paladinsinn.torg.codex.application.service;

import de.paladinsinn.torg.codex.application.port.in.CatalogReference;
import de.paladinsinn.torg.codex.application.port.in.CatalogPublicationReference;
import de.paladinsinn.torg.codex.application.port.in.CatalogReferenceQuery;
import de.paladinsinn.torg.codex.application.port.out.CatalogReferencePersistencePort;

import java.util.List;
import java.util.Optional;

/**
 * Framework-independent implementation of the catalog reference use case.
 */
public final class CatalogReferenceQueryService implements CatalogReferenceQuery {

    private final CatalogReferencePersistencePort persistence;

    public CatalogReferenceQueryService(CatalogReferencePersistencePort persistence) {
        this.persistence = persistence;
    }

    @Override
    public Optional<CatalogReference> findCosmByName(String name) {
        return persistence.findCosmByName(name);
    }

    @Override
    public Optional<CatalogReference> findPublicationByCodexId(String codexId) {
        return persistence.findPublicationByCodexId(codexId);
    }

    @Override
    public List<CatalogPublicationReference> findPublicationsByProductId(int productId) {
        return persistence.findPublicationsByProductId(productId);
    }
}
