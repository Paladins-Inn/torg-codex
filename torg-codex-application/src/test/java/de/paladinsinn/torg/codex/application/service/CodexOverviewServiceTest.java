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

import de.paladinsinn.torg.codex.application.port.in.CodexHomeOverview;
import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.application.port.out.CatalogUsageObserver;
import de.paladinsinn.torg.codex.application.service.CatalogBrowseService.AreaSource;
import de.paladinsinn.torg.codex.domain.access.CatalogAccessPolicy;
import de.paladinsinn.torg.codex.domain.access.OwnedProducts;
import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import de.paladinsinn.torg.codex.domain.catalog.DetailAccessOutcome;
import de.paladinsinn.torg.codex.domain.catalog.GroupingDimension;
import de.paladinsinn.torg.codex.domain.markup.EntityType;
import de.paladinsinn.torg.codex.domain.model.CatalogEntry;
import de.paladinsinn.torg.codex.domain.model.ClearanceLevel;
import de.paladinsinn.torg.codex.domain.model.Perk;
import de.paladinsinn.torg.codex.domain.model.Publication;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CodexOverviewServiceTest {

    @Test
    void computesAccessibleCountsAndPublicationPartitionsWithoutIncrementingCategoryUsageCounters() {
        AtomicInteger usageEvents = new AtomicInteger();
        CatalogUsageObserver failIfCalled = new CatalogUsageObserver() {
            @Override
            public void categoryAccessed(final CatalogArea area) {
                usageEvents.incrementAndGet();
            }

            @Override
            public void cosmAccessed(final String cosmSlug) {
                usageEvents.incrementAndGet();
            }

            @Override
            public void groupingAccessed(final GroupingDimension dimension, final String groupKey) {
                usageEvents.incrementAndGet();
            }

            @Override
            public void detailAccessed(final CatalogArea area, final String entrySlug, final DetailAccessOutcome outcome) {
                usageEvents.incrementAndGet();
            }

            @Override
            public void detailNotFound(final CatalogArea area) {
                usageEvents.incrementAndGet();
            }
        };

        Perk freePerk = new Perk(UUID.randomUUID(), "Ace Pilot", ClearanceLevel.ALPHA, Set.of("core-rulebook"), false, Set.of(), "prowess", "", "");
        Perk ayslePerk = new Perk(UUID.randomUUID(), "Spellcraft", ClearanceLevel.ALPHA, Set.of("sourcebook-aysle"), false, Set.of(), "spellcraft", "", "");

        Publication corePub = new Publication(UUID.randomUUID(), "core-rulebook", "Core Rules", 216248, null, Set.of(216248));
        Publication ayslePub = new Publication(UUID.randomUUID(), "sourcebook-aysle", "Aysle Sourcebook", 295843, null, Set.of(295843));

        var overviewService = new CodexOverviewService(List.of(
                AreaSource.restricted(CatalogArea.PERK, new RestrictedCatalogQueryService<>(
                        EntityType.PERK, port(List.of(freePerk, ayslePerk)), OwnedProducts::anonymous, (a, i, n) -> { }, new CatalogAccessPolicy(), failIfCalled)),
                AreaSource.publications(new CatalogQueryService<>(CatalogArea.PUBLICATION, port(List.of(corePub, ayslePub)), failIfCalled))
        ), OwnedProducts::anonymous);

        CodexHomeOverview overview = overviewService.loadOverview();

        assertThat(usageEvents.get()).isZero();
        assertThat(overview.categories()).hasSize(17);
        assertThat(overview.categories())
                .filteredOn(c -> c.area() == CatalogArea.PERK)
                .singleElement()
                .extracting(CodexHomeOverview.CategorySummary::accessibleCount)
                .isEqualTo(1);
        assertThat(overview.ownedPublications()).containsExactly(corePub);
        assertThat(overview.missingPublications()).containsExactly(ayslePub);
        assertThat(overview.unlockedPercentage()).isEqualTo(50.0);
    }

    private static <T extends CatalogEntry> CatalogPersistencePort<T> port(final List<T> items) {
        return new CatalogPersistencePort<>() {
            @Override
            public List<T> findAll() {
                return items;
            }

            @Override
            public Optional<T> findById(final UUID id) {
                return items.stream().filter(i -> i.id().equals(id)).findFirst();
            }

            @Override
            public List<T> findByCosm(final String cosm) {
                return List.of();
            }
        };
    }
}
