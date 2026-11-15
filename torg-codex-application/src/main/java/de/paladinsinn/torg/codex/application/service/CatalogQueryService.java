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

import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.application.port.out.CatalogUsageObserver;
import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import de.paladinsinn.torg.codex.domain.catalog.CatalogSlugs;
import de.paladinsinn.torg.codex.domain.catalog.DetailAccessOutcome;
import de.paladinsinn.torg.codex.domain.model.CatalogEntry;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Framework-independent implementation of the unrestricted public reference catalog reading use case
 * (limited to Article, Publication, and Tag per ArchUnit A2/A6).
 *
 * @param <T> the entry type
 */
public final class CatalogQueryService<T> implements CatalogQuery<T> {

    /** Field name {@code persistence} is inspected by {@code CatalogDrmArchitectureTest} (A6). */
    private final CatalogPersistencePort<T> persistence;
    private final CatalogArea area;
    private final CatalogUsageObserver usageObserver;

    public CatalogQueryService(final CatalogPersistencePort<T> persistence) {
        this(null, persistence, CatalogUsageObserver.NO_OP);
    }

    public CatalogQueryService(
            final CatalogArea area,
            final CatalogPersistencePort<T> persistence,
            final CatalogUsageObserver usageObserver) {
        this.area = area;
        this.persistence = Objects.requireNonNull(persistence, "persistence must not be null");
        this.usageObserver = Objects.requireNonNull(usageObserver, "usageObserver must not be null");
    }

    @Override
    public List<T> findAll() {
        if (area != null) {
            usageObserver.categoryAccessed(area);
        }
        return peekAll();
    }

    @Override
    public List<T> peekAll() {
        return persistence.findAll();
    }

    @Override
    public Optional<T> findById(final UUID id) {
        Optional<T> found = persistence.findById(id);
        if (found.isEmpty()) {
            if (area != null) {
                usageObserver.detailNotFound(area);
            }
            return Optional.empty();
        }
        T value = found.get();
        recordPublicDetailAccess(value, extractSlug(value));
        return found;
    }

    @Override
    public Optional<T> findByIdentifier(final String slugOrId) {
        Optional<UUID> parsedId = CatalogSlugs.tryParseUuid(slugOrId);
        if (parsedId.isPresent()) {
            return findById(parsedId.get());
        }
        String targetSlug = CatalogSlugs.normalize(slugOrId);
        if (targetSlug.isEmpty()) {
            if (area != null) {
                usageObserver.detailNotFound(area);
            }
            return Optional.empty();
        }
        List<T> all = persistence.findAll();
        List<CatalogEntry> catalogEntries = all.stream()
                .filter(CatalogEntry.class::isInstance)
                .map(CatalogEntry.class::cast)
                .toList();
        if (catalogEntries.size() == all.size()) {
            Map<UUID, String> canonicalSlugs = CatalogSlugs.resolveCanonicalSlugs(
                    catalogEntries, CatalogEntry::id, CatalogEntry::slug);
            for (T candidate : all) {
                CatalogEntry entry = (CatalogEntry) candidate;
                String canonical = canonicalSlugs.get(entry.id());
                if (targetSlug.equals(canonical)) {
                    recordPublicDetailAccess(candidate, canonical);
                    return Optional.of(candidate);
                }
            }
        }
        if (area != null) {
            usageObserver.detailNotFound(area);
        }
        return Optional.empty();
    }

    @Override
    public List<T> findByCosm(final String cosm) {
        if (area != null) {
            usageObserver.categoryAccessed(area);
        }
        List<T> matches = persistence.findByCosm(cosm);
        String normalizedCosm = CatalogSlugs.normalize(cosm);
        if (!normalizedCosm.isEmpty() && !matches.isEmpty()) {
            usageObserver.cosmAccessed(normalizedCosm);
        }
        return matches;
    }

    private void recordPublicDetailAccess(final T entry, final String slug) {
        if (area == null) {
            return;
        }
        String effectiveSlug = slug == null || slug.isEmpty() ? "unknown" : slug;
        usageObserver.detailAccessed(area, effectiveSlug, DetailAccessOutcome.PUBLIC_REFERENCE);
        area.selfGroupingDimension().ifPresent(dim -> usageObserver.groupingAccessed(dim, effectiveSlug));
    }

    private String extractSlug(final T entry) {
        if (entry instanceof CatalogEntry ce) {
            String slug = ce.slug();
            return slug.isEmpty() ? ce.id().toString() : slug;
        }
        return "entry";
    }
}
