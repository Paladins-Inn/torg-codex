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

import de.paladinsinn.torg.codex.domain.model.ProductBoundEntry;

import java.util.Objects;
import java.util.Set;

/**
 * The pure, stateless product-ownership access rule for catalog entries (ADR-019).
 *
 * <p>Evaluated in this order: an entry without (non-blank) products is
 * {@link AccessDecision#UNASSIGNED}; an entry with at least one owned product is
 * {@link AccessDecision#GRANTED}; otherwise the decision is {@link AccessDecision#TEASER}.
 */
public final class CatalogAccessPolicy {

    /**
     * Decides whether the owner of the given products may read the given entry.
     *
     * @param entry the catalog entry
     * @param owned the caller's effective owned products
     * @return the access decision
     */
    public AccessDecision decide(final ProductBoundEntry entry, final OwnedProducts owned) {
        Objects.requireNonNull(entry, "entry");
        Objects.requireNonNull(owned, "owned");

        Set<String> assigned = OwnedProducts.normalize(entry.products());
        if (assigned.isEmpty()) {
            return AccessDecision.UNASSIGNED;
        }
        return owned.ownsAnyOf(assigned) ? AccessDecision.GRANTED : AccessDecision.TEASER;
    }
}
