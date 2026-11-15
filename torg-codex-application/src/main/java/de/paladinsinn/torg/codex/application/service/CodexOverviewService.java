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
import de.paladinsinn.torg.codex.application.port.in.CodexHomeOverview.CategorySummary;
import de.paladinsinn.torg.codex.application.port.in.CodexOverviewQuery;
import de.paladinsinn.torg.codex.application.port.out.CallerProductsPort;
import de.paladinsinn.torg.codex.application.service.CatalogBrowseService.AreaSource;
import de.paladinsinn.torg.codex.domain.access.OwnedProducts;
import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import de.paladinsinn.torg.codex.domain.model.Publication;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Framework-free use case computing landing-page category counts and owned/missing publications
 * without triggering {@code usage.category} browse counter spikes (FR-015, Guarantee O1).
 */
public final class CodexOverviewService implements CodexOverviewQuery {

    private static final double FULL_PERCENTAGE = 100.0;

    private final Map<CatalogArea, AreaSource> sources;
    private final CallerProductsPort callerProducts;

    public CodexOverviewService(
            final List<AreaSource> areaSources,
            final CallerProductsPort callerProducts) {
        Objects.requireNonNull(areaSources, "areaSources must not be null");
        this.callerProducts = Objects.requireNonNull(callerProducts, "callerProducts must not be null");
        Map<CatalogArea, AreaSource> map = new EnumMap<>(CatalogArea.class);
        for (AreaSource source : areaSources) {
            map.put(source.area(), source);
        }
        this.sources = Map.copyOf(map);
    }

    @Override
    public CodexHomeOverview loadOverview() {
        OwnedProducts owned = callerProducts.current();
        List<CategorySummary> summaries = new ArrayList<>(CatalogArea.values().length);
        for (CatalogArea area : CatalogArea.values()) {
            AreaSource source = sources.get(area);
            int count = source == null ? 0 : source.peekAll().size();
            summaries.add(new CategorySummary(area, count));
        }

        List<Publication> ownedPubs = new ArrayList<>();
        List<Publication> missingPubs = new ArrayList<>();
        AreaSource pubSource = sources.get(CatalogArea.PUBLICATION);
        if (pubSource != null) {
            List<Publication> sortedPubs = pubSource.peekAll().stream()
                    .filter(Publication.class::isInstance)
                    .map(Publication.class::cast)
                    .sorted(Comparator.comparing(Publication::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            for (Publication pub : sortedPubs) {
                if (pub.codexId() != null && owned.codexIds().contains(pub.codexId())) {
                    ownedPubs.add(pub);
                } else {
                    missingPubs.add(pub);
                }
            }
        }

        int totalPubs = ownedPubs.size() + missingPubs.size();
        double percentage = totalPubs == 0
                ? FULL_PERCENTAGE
                : (ownedPubs.size() * FULL_PERCENTAGE) / totalPubs;

        return new CodexHomeOverview(
                List.copyOf(summaries),
                List.copyOf(ownedPubs),
                List.copyOf(missingPubs),
                percentage);
    }
}
