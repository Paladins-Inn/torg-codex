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

import de.paladinsinn.torg.codex.domain.access.EntryAccess;
import de.paladinsinn.torg.codex.domain.model.ProductBoundEntry;

import java.util.List;
import java.util.UUID;

/**
 * Driving port for reading product-bound catalog entries with product-ownership DRM applied
 * (ADR-019). It is the only way inbound adapters read the 14 product-bound catalog areas.
 *
 * <p>Guarantees of every implementation:
 * <ul>
 *   <li>G1: the caller's effective ownership is resolved at most once per invocation;</li>
 *   <li>G2: every entry without products encountered during an invocation, top-level or
 *       referenced, is reported exactly once to the {@code CatalogAccessObserver};</li>
 *   <li>G3: {@link #findAll()} and {@link #findByCosm(String)} return only granted entries;</li>
 *   <li>G4: {@link #findById(UUID)} returns a teaser only for entries the caller does not own,
 *       and the absent result for both missing and unassigned entries;</li>
 *   <li>G5: embedded references of returned entries contain only granted ids, in their original
 *       order, resolved with one batched lookup per invocation;</li>
 *   <li>G6: implementations carry no Spring, JPA, Hibernate or Micrometer dependency.</li>
 * </ul>
 *
 * @param <T> the product-bound entry type
 */
public interface RestrictedCatalogQuery<T extends ProductBoundEntry> {

    /**
     * Lists the entries the caller may read.
     *
     * @return the accessible entries; never contains teasers
     */
    List<T> findAll();

    /**
     * Lists the entries of a cosm the caller may read.
     *
     * @param cosm the cosm slug
     * @return the accessible entries of the cosm; never contains teasers
     */
    List<T> findByCosm(String cosm);

    /**
     * Reads a single entry.
     *
     * @param id the entry id
     * @return the entry, its purchase teaser, or the absent result
     */
    EntryAccess<T> findById(UUID id);
}
