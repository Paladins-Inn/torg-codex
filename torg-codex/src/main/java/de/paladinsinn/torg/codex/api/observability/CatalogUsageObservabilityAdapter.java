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

package de.paladinsinn.torg.codex.api.observability;

import de.paladinsinn.torg.codex.application.port.out.CatalogUsageObserver;
import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import de.paladinsinn.torg.codex.domain.catalog.CatalogSlugs;
import de.paladinsinn.torg.codex.domain.catalog.DetailAccessOutcome;
import de.paladinsinn.torg.codex.domain.catalog.GroupingDimension;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Driven Micrometer adapter recording the 4 granular catalog usage counters and both sets of 4
 * aggregate roll-up counters (FR-017, SC-008, {@code observability-and-logging.md}).
 *
 * <p>Never records caller identity, credentials, or {@code ROLE_<codex-id>} tags, and uses
 * {@value #NOT_FOUND_ENTRY_SENTINEL} for missing/unassigned detail lookups to prevent unbounded
 * metric cardinality.
 */
@Component
public class CatalogUsageObservabilityAdapter implements CatalogUsageObserver {

    public static final String CATEGORY_COUNTER = "torg.codex.catalog.usage.category";
    public static final String COSM_COUNTER = "torg.codex.catalog.usage.cosm";
    public static final String GROUPING_COUNTER = "torg.codex.catalog.usage.grouping";
    public static final String DETAIL_COUNTER = "torg.codex.catalog.usage.detail";

    public static final String OUTCOME_DRM_GRANTED_COUNTER = "torg.codex.catalog.usage.outcome.drm.granted";
    public static final String OUTCOME_PUBLIC_REF_COUNTER = "torg.codex.catalog.usage.outcome.public.reference";
    public static final String OUTCOME_TEASER_COUNTER = "torg.codex.catalog.usage.outcome.teaser";
    public static final String OUTCOME_NOT_FOUND_COUNTER = "torg.codex.catalog.usage.outcome.not.found";

    public static final String FAMILY_CATEGORY_COUNTER = "torg.codex.catalog.usage.family.category";
    public static final String FAMILY_COSM_COUNTER = "torg.codex.catalog.usage.family.cosm";
    public static final String FAMILY_GROUPING_COUNTER = "torg.codex.catalog.usage.family.grouping";
    public static final String FAMILY_DETAIL_COUNTER = "torg.codex.catalog.usage.family.detail";

    public static final String NOT_FOUND_ENTRY_SENTINEL = "_not_found_";

    private final MeterRegistry registry;
    private final Map<CatalogArea, Counter> categoryCounters = new EnumMap<>(CatalogArea.class);
    private final Map<CatalogArea, Counter> notFoundDetailCounters = new EnumMap<>(CatalogArea.class);
    private final Map<DetailAccessOutcome, Counter> outcomeRollups = new EnumMap<>(DetailAccessOutcome.class);

    private final Counter familyCategoryTotal;
    private final Counter familyCosmTotal;
    private final Counter familyGroupingTotal;
    private final Counter familyDetailTotal;

    public CatalogUsageObservabilityAdapter(final MeterRegistry registry) {
        this.registry = registry;
        for (CatalogArea area : CatalogArea.values()) {
            categoryCounters.put(area, Counter.builder(CATEGORY_COUNTER)
                    .description("Catalog category read accesses")
                    .tag("category", area.slug())
                    .register(registry));
            notFoundDetailCounters.put(area, Counter.builder(DETAIL_COUNTER)
                    .description("Single-entry catalog detail lookups")
                    .tag("category", area.slug())
                    .tag("entry", NOT_FOUND_ENTRY_SENTINEL)
                    .tag("outcome", DetailAccessOutcome.NOT_FOUND.getTag())
                    .register(registry));
        }

        outcomeRollups.put(DetailAccessOutcome.DRM_GRANTED, Counter.builder(OUTCOME_DRM_GRANTED_COUNTER)
                .description("Total DRM-granted single-entry detail accesses across all categories")
                .register(registry));
        outcomeRollups.put(DetailAccessOutcome.PUBLIC_REFERENCE, Counter.builder(OUTCOME_PUBLIC_REF_COUNTER)
                .description("Total public-reference single-entry detail accesses across all categories")
                .register(registry));
        outcomeRollups.put(DetailAccessOutcome.TEASER, Counter.builder(OUTCOME_TEASER_COUNTER)
                .description("Total DRM purchase-teaser detail accesses across all categories")
                .register(registry));
        outcomeRollups.put(DetailAccessOutcome.NOT_FOUND, Counter.builder(OUTCOME_NOT_FOUND_COUNTER)
                .description("Total not-found single-entry detail lookups across all categories")
                .register(registry));

        this.familyCategoryTotal = Counter.builder(FAMILY_CATEGORY_COUNTER)
                .description("Total category accesses across all catalog areas")
                .register(registry);
        this.familyCosmTotal = Counter.builder(FAMILY_COSM_COUNTER)
                .description("Total cosm accesses and filters across all cosms")
                .register(registry);
        this.familyGroupingTotal = Counter.builder(FAMILY_GROUPING_COUNTER)
                .description("Total grouping accesses and filters across all grouping dimensions")
                .register(registry);
        this.familyDetailTotal = Counter.builder(FAMILY_DETAIL_COUNTER)
                .description("Total single-entry detail lookups across all outcomes")
                .register(registry);
    }

    @Override
    public void categoryAccessed(final CatalogArea area) {
        if (area == null) {
            return;
        }
        categoryCounters.get(area).increment();
        familyCategoryTotal.increment();
    }

    @Override
    public void cosmAccessed(final String cosmSlug) {
        String normalized = CatalogSlugs.normalize(cosmSlug);
        if (normalized.isEmpty()) {
            return;
        }
        Counter.builder(COSM_COUNTER)
                .description("Catalog cosm accesses and filters")
                .tag("cosm", normalized)
                .register(registry)
                .increment();
        familyCosmTotal.increment();
    }

    @Override
    public void groupingAccessed(final GroupingDimension dimension, final String groupKey) {
        String normalized = CatalogSlugs.normalize(groupKey);
        if (dimension == null || normalized.isEmpty()) {
            return;
        }
        Counter.builder(GROUPING_COUNTER)
                .description("Catalog grouping accesses and structured facet filters")
                .tag("dimension", dimension.getTag())
                .tag("group", normalized)
                .register(registry)
                .increment();
        familyGroupingTotal.increment();
    }

    @Override
    public void detailAccessed(
            final CatalogArea area,
            final String entrySlug,
            final DetailAccessOutcome outcome) {
        if (area == null || outcome == null) {
            return;
        }
        if (outcome == DetailAccessOutcome.NOT_FOUND) {
            detailNotFound(area);
            return;
        }
        String normalizedEntry = CatalogSlugs.normalize(entrySlug);
        if (normalizedEntry.isEmpty()) {
            normalizedEntry = NOT_FOUND_ENTRY_SENTINEL;
        }
        Counter.builder(DETAIL_COUNTER)
                .description("Single-entry catalog detail lookups")
                .tag("category", area.slug())
                .tag("entry", normalizedEntry)
                .tag("outcome", outcome.getTag())
                .register(registry)
                .increment();
        outcomeRollups.get(outcome).increment();
        familyDetailTotal.increment();
    }

    @Override
    public void detailNotFound(final CatalogArea area) {
        if (area == null) {
            return;
        }
        notFoundDetailCounters.get(area).increment();
        outcomeRollups.get(DetailAccessOutcome.NOT_FOUND).increment();
        familyDetailTotal.increment();
    }
}
