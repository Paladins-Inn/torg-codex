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

import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import de.paladinsinn.torg.codex.domain.catalog.DetailAccessOutcome;
import de.paladinsinn.torg.codex.domain.catalog.GroupingDimension;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogUsageObservabilityAdapterTest {

    @Test
    void incrementsGranularAndBothRollupCounterSetsAcrossAllFourOutcomesWithBoundedSentinel() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CatalogUsageObservabilityAdapter adapter = new CatalogUsageObservabilityAdapter(registry);

        adapter.categoryAccessed(CatalogArea.PERK);
        adapter.categoryAccessed(CatalogArea.SPELL);
        adapter.cosmAccessed("aysle");
        adapter.groupingAccessed(GroupingDimension.PERK_GROUP, "prowess");
        adapter.groupingAccessed(GroupingDimension.CLEARANCE, "beta");

        adapter.detailAccessed(CatalogArea.PERK, "ace-pilot", DetailAccessOutcome.DRM_GRANTED);
        adapter.detailAccessed(CatalogArea.ARTICLE, "changelog", DetailAccessOutcome.PUBLIC_REFERENCE);
        adapter.detailAccessed(CatalogArea.SPELL, "aysle-bolt", DetailAccessOutcome.TEASER);
        adapter.detailNotFound(CatalogArea.SPELL);

        assertThat(registry.get(CatalogUsageObservabilityAdapter.CATEGORY_COUNTER)
                .tag("category", "perks").counter().count()).isEqualTo(1.0);
        assertThat(registry.get(CatalogUsageObservabilityAdapter.COSM_COUNTER)
                .tag("cosm", "aysle").counter().count()).isEqualTo(1.0);
        assertThat(registry.get(CatalogUsageObservabilityAdapter.GROUPING_COUNTER)
                .tag("dimension", "perk-group").tag("group", "prowess").counter().count()).isEqualTo(1.0);
        assertThat(registry.get(CatalogUsageObservabilityAdapter.DETAIL_COUNTER)
                .tag("category", "perks").tag("entry", "ace-pilot").tag("outcome", "drm-granted")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get(CatalogUsageObservabilityAdapter.DETAIL_COUNTER)
                .tag("category", "spells")
                .tag("entry", CatalogUsageObservabilityAdapter.NOT_FOUND_ENTRY_SENTINEL)
                .tag("outcome", "not-found")
                .counter().count()).isEqualTo(1.0);

        // Outcome roll-up counters (Set 1)
        assertThat(registry.get(CatalogUsageObservabilityAdapter.OUTCOME_DRM_GRANTED_COUNTER).counter().count()).isEqualTo(1.0);
        assertThat(registry.get(CatalogUsageObservabilityAdapter.OUTCOME_PUBLIC_REF_COUNTER).counter().count()).isEqualTo(1.0);
        assertThat(registry.get(CatalogUsageObservabilityAdapter.OUTCOME_TEASER_COUNTER).counter().count()).isEqualTo(1.0);
        assertThat(registry.get(CatalogUsageObservabilityAdapter.OUTCOME_NOT_FOUND_COUNTER).counter().count()).isEqualTo(1.0);

        // Metric-family roll-up counters (Set 2)
        assertThat(registry.get(CatalogUsageObservabilityAdapter.FAMILY_CATEGORY_COUNTER).counter().count()).isEqualTo(2.0);
        assertThat(registry.get(CatalogUsageObservabilityAdapter.FAMILY_COSM_COUNTER).counter().count()).isEqualTo(1.0);
        assertThat(registry.get(CatalogUsageObservabilityAdapter.FAMILY_GROUPING_COUNTER).counter().count()).isEqualTo(2.0);
        assertThat(registry.get(CatalogUsageObservabilityAdapter.FAMILY_DETAIL_COUNTER).counter().count()).isEqualTo(4.0);

        // Privacy guarantee: no meter carries caller, role, or credential tags
        for (Meter meter : registry.getMeters()) {
            assertThat(meter.getId().getTags())
                    .extracting(io.micrometer.core.instrument.Tag::getKey)
                    .doesNotContain("user", "principal", "role", "apiKey", "ownedProducts");
        }
    }
}
