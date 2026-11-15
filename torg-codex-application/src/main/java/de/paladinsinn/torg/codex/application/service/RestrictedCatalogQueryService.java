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

import de.paladinsinn.torg.codex.application.port.in.RestrictedCatalogQuery;
import de.paladinsinn.torg.codex.application.port.out.CallerProductsPort;
import de.paladinsinn.torg.codex.application.port.out.CatalogAccessObserver;
import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.domain.access.AccessDecision;
import de.paladinsinn.torg.codex.domain.access.CatalogAccessPolicy;
import de.paladinsinn.torg.codex.domain.access.EntryAccess;
import de.paladinsinn.torg.codex.domain.access.OwnedProducts;
import de.paladinsinn.torg.codex.domain.markup.EntityType;
import de.paladinsinn.torg.codex.domain.model.ProductBoundEntry;
import de.paladinsinn.torg.codex.domain.model.ReferencesEntries;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Framework-independent implementation of the restricted catalog reading use case (ADR-019).
 *
 * <p>Applies the {@link CatalogAccessPolicy} to every entry loaded from persistence with the
 * caller's effective ownership, resolved once per invocation. Entries without products are
 * reported to the {@link CatalogAccessObserver} and treated as absent. Areas embedding references
 * to other entries filter them with a {@link ReferencedEntryResolver} using the same ownership.
 *
 * @param <T> the product-bound entry type
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class RestrictedCatalogQueryService<T extends ProductBoundEntry> implements RestrictedCatalogQuery<T> {

    private final EntityType area;
    private final CatalogPersistencePort<T> persistence;
    private final CallerProductsPort callerProducts;
    private final CatalogAccessObserver observer;
    private final CatalogAccessPolicy policy;
    private final ReferenceFilter<T> references;

    /**
     * Creates the use case for an area without embedded references.
     *
     * @param area           the catalog area
     * @param persistence    the persistence port of the area
     * @param callerProducts the caller ownership port
     * @param observer       the observer of entries without products
     * @param policy         the access policy
     */
    public RestrictedCatalogQueryService(
            final EntityType area,
            final CatalogPersistencePort<T> persistence,
            final CallerProductsPort callerProducts,
            final CatalogAccessObserver observer,
            final CatalogAccessPolicy policy) {
        this(area, persistence, callerProducts, observer, policy, (entries, owned) -> entries);
    }

    /**
     * Creates the use case for an area whose entries embed references to other entries.
     *
     * @param area           the catalog area
     * @param persistence    the persistence port of the area
     * @param callerProducts the caller ownership port
     * @param observer       the observer of entries without products
     * @param policy         the access policy
     * @param resolver       the resolver of the referenced area
     * @param <L>            the referencing entry type
     * @return the use case
     */
    public static <L extends ProductBoundEntry & ReferencesEntries<L>> RestrictedCatalogQueryService<L> withReferences(
            final EntityType area,
            final CatalogPersistencePort<L> persistence,
            final CallerProductsPort callerProducts,
            final CatalogAccessObserver observer,
            final CatalogAccessPolicy policy,
            final ReferencedEntryResolver<L> resolver) {
        return new RestrictedCatalogQueryService<>(area, persistence, callerProducts, observer, policy, resolver::filter);
    }

    @Override
    public List<T> findAll() {
        return granted(persistence.findAll(), callerProducts.current());
    }

    @Override
    public List<T> findByCosm(final String cosm) {
        return granted(persistence.findByCosm(cosm), callerProducts.current());
    }

    @Override
    public EntryAccess<T> findById(final UUID id) {
        final OwnedProducts owned = callerProducts.current();
        return persistence.findById(id)
                .map(entry -> access(entry, owned))
                .orElseGet(EntryAccess::absent);
    }

    private List<T> granted(final List<T> candidates, final OwnedProducts owned) {
        final List<T> granted = candidates.stream()
                .filter(entry -> decide(entry, owned) == AccessDecision.GRANTED)
                .toList();
        return granted.isEmpty() ? granted : references.apply(granted, owned);
    }

    private EntryAccess<T> access(final T entry, final OwnedProducts owned) {
        return switch (decide(entry, owned)) {
            case GRANTED -> EntryAccess.granted(references.apply(List.of(entry), owned).getFirst());
            case TEASER -> EntryAccess.teaserOf(entry);
            case UNASSIGNED -> EntryAccess.absent();
        };
    }

    private AccessDecision decide(final T entry, final OwnedProducts owned) {
        final AccessDecision decision = policy.decide(entry, owned);
        if (decision == AccessDecision.UNASSIGNED) {
            observer.entryWithoutProducts(area, entry.id(), entry.name());
        }
        return decision;
    }

    /**
     * Filters embedded references of granted entries with the caller's ownership.
     *
     * @param <T> the entry type
     */
    @FunctionalInterface
    private interface ReferenceFilter<T> {

        List<T> apply(List<T> entries, OwnedProducts owned);
    }
}
