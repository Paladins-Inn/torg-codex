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
import de.paladinsinn.torg.codex.application.port.out.CatalogAccessObserver;
import de.paladinsinn.torg.codex.application.port.out.CatalogEntryReference;
import de.paladinsinn.torg.codex.application.port.out.CatalogReferencePersistencePort;
import de.paladinsinn.torg.codex.domain.access.AccessDecision;
import de.paladinsinn.torg.codex.domain.access.CatalogAccessPolicy;
import de.paladinsinn.torg.codex.domain.access.OwnedProducts;
import de.paladinsinn.torg.codex.domain.markup.EntityType;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Framework-independent implementation of the catalog reference use case.
 *
 * <p>Cosm references expose only teaser-level data (id and name), so the caller's ownership is
 * irrelevant; cosms without products are hidden like unknown slugs and reported to the
 * {@link CatalogAccessObserver} (ADR-019, FR-008/FR-016).
 */
@RequiredArgsConstructor
public final class CatalogReferenceQueryService implements CatalogReferenceQuery {

    private final CatalogReferencePersistencePort persistence;
    private final CatalogAccessObserver observer;
    private final CatalogAccessPolicy policy;

    @Override
    public Optional<CatalogReference> findCosmBySlug(String slug) {
        return persistence.findCosmBySlug(slug).flatMap(this::visibleReference);
    }

    private Optional<CatalogReference> visibleReference(CatalogEntryReference cosm) {
        if (policy.decide(cosm, OwnedProducts.anonymous()) == AccessDecision.UNASSIGNED) {
            observer.entryWithoutProducts(EntityType.COSM, cosm.id(), cosm.name());
            return Optional.empty();
        }
        return Optional.of(new CatalogReference(cosm.id(), cosm.name()));
    }

    @Override
    public Optional<CatalogReference> findPublicationByCodexId(String codexId) {
        return persistence.findPublicationByCodexId(codexId);
    }

    @Override
    public List<CatalogPublicationReference> findPublicationsByProductId(int productId) {
        return persistence.findPublicationsByProductId(productId);
    }

    @Override
    public List<CatalogPublicationReference> findPublicationsByProductIds(Set<Integer> productIds) {
        return productIds.isEmpty() ? List.of() : persistence.findPublicationsByProductIds(productIds);
    }
}
