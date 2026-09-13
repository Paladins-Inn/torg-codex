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

package de.paladinsinn.torg.codex.application.port.in;

import java.util.List;
import java.util.Optional;

/**
 * Driving port for resolving lightweight references used by codex use cases.
 */
public interface CatalogReferenceQuery {

    /**
     * Resolves a cosm reference by its stable slug.
     *
     * @param slug the cosm slug used by entity references and API filters
     * @return the reference carrying the cosm id and its display name, if found
     */
    Optional<CatalogReference> findCosmBySlug(String slug);

    Optional<CatalogReference> findPublicationByCodexId(String codexId);

    List<CatalogPublicationReference> findPublicationsByProductId(int productId);
}
