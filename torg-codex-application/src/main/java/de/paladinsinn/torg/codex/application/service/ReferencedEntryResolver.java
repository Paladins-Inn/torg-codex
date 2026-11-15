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

import de.paladinsinn.torg.codex.application.port.out.CatalogAccessObserver;
import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.domain.access.AccessDecision;
import de.paladinsinn.torg.codex.domain.access.CatalogAccessPolicy;
import de.paladinsinn.torg.codex.domain.access.OwnedProducts;
import de.paladinsinn.torg.codex.domain.model.ProductBoundEntry;
import de.paladinsinn.torg.codex.domain.model.ReferencesEntries;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Removes embedded references to entries the caller may not read (ADR-019, FR-009).
 *
 * <p>Collects the referenced ids of all given entries, reads the referenced entries with one
 * batched {@link CatalogPersistencePort#findAllById} call, applies the {@link CatalogAccessPolicy}
 * once per referenced entry (reporting entries without products), and rebuilds each entry with
 * only the granted references in their original order.
 *
 * @param <L> the referencing entry type
 */
@RequiredArgsConstructor
public final class ReferencedEntryResolver<L extends ProductBoundEntry & ReferencesEntries<L>> {

    private final CatalogPersistencePort<? extends ProductBoundEntry> referenced;
    private final CatalogAccessObserver observer;
    private final CatalogAccessPolicy policy;

    /**
     * Filters the references of the given entries.
     *
     * @param entries the referencing entries
     * @param owned   the caller's effective owned products
     * @return copies of the entries containing only accessible references
     */
    public List<L> filter(final List<L> entries, final OwnedProducts owned) {
        final Set<UUID> requested = entries.stream()
                .flatMap(entry -> entry.referencedEntries().stream())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        final Set<UUID> accessible = accessibleIds(requested, owned);
        return entries.stream()
                .map(entry -> entry.withReferencedEntries(entry.referencedEntries().stream()
                        .filter(accessible::contains)
                        .toList()))
                .toList();
    }

    private Set<UUID> accessibleIds(final Set<UUID> requested, final OwnedProducts owned) {
        if (requested.isEmpty()) {
            return Set.of();
        }
        final Set<UUID> accessible = new HashSet<>();
        for (ProductBoundEntry entry : referenced.findAllById(requested)) {
            final AccessDecision decision = policy.decide(entry, owned);
            if (decision == AccessDecision.UNASSIGNED) {
                observer.entryWithoutProducts(entry.entityType(), entry.id(), entry.name());
            } else if (decision == AccessDecision.GRANTED) {
                accessible.add(entry.id());
            }
        }
        return accessible;
    }
}
