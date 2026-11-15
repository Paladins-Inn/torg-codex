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

package de.paladinsinn.torg.codex.api.configuration;

import de.paladinsinn.torg.codex.application.port.in.CatalogBrowseQuery;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.application.port.in.CodexOverviewQuery;
import de.paladinsinn.torg.codex.application.port.in.RestrictedCatalogQuery;
import de.paladinsinn.torg.codex.application.port.out.CallerProductsPort;
import de.paladinsinn.torg.codex.application.port.out.CatalogUsageObserver;
import de.paladinsinn.torg.codex.application.service.CatalogBrowseService;
import de.paladinsinn.torg.codex.application.service.CatalogBrowseService.AreaSource;
import de.paladinsinn.torg.codex.application.service.CodexOverviewService;
import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import de.paladinsinn.torg.codex.domain.model.Article;
import de.paladinsinn.torg.codex.domain.model.Cosm;
import de.paladinsinn.torg.codex.domain.model.Item;
import de.paladinsinn.torg.codex.domain.model.Miracle;
import de.paladinsinn.torg.codex.domain.model.MiracleList;
import de.paladinsinn.torg.codex.domain.model.Perk;
import de.paladinsinn.torg.codex.domain.model.PerkGroup;
import de.paladinsinn.torg.codex.domain.model.Power;
import de.paladinsinn.torg.codex.domain.model.PowerList;
import de.paladinsinn.torg.codex.domain.model.Publication;
import de.paladinsinn.torg.codex.domain.model.Race;
import de.paladinsinn.torg.codex.domain.model.Shard;
import de.paladinsinn.torg.codex.domain.model.Spell;
import de.paladinsinn.torg.codex.domain.model.SpellList;
import de.paladinsinn.torg.codex.domain.model.Tag;
import de.paladinsinn.torg.codex.domain.model.Threat;
import de.paladinsinn.torg.codex.domain.model.Vehicle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Composition root wiring the generic 17-area {@link CatalogBrowseQuery} and
 * {@link CodexOverviewQuery} strictly over the driving query ports (ADR-019, D5).
 *
 * <p>Depends exclusively on {@link RestrictedCatalogQuery} for the 14 product-bound areas and
 * concrete {@link CatalogQuery} types for {@link Article}, {@link Publication}, and {@link Tag},
 * satisfying ArchUnit invariants A1, A2, and A6 without touching persistence classes.
 */
@Configuration
public class CatalogBrowseConfiguration {

    @Bean
    AreaSource cosmAreaSource(final RestrictedCatalogQuery<Cosm> query) {
        return AreaSource.restricted(CatalogArea.COSM, query);
    }

    @Bean
    AreaSource raceAreaSource(final RestrictedCatalogQuery<Race> query) {
        return AreaSource.restricted(CatalogArea.RACE, query);
    }

    @Bean
    AreaSource perkAreaSource(final RestrictedCatalogQuery<Perk> query) {
        return AreaSource.restricted(CatalogArea.PERK, query);
    }

    @Bean
    AreaSource perkGroupAreaSource(final RestrictedCatalogQuery<PerkGroup> query) {
        return AreaSource.restricted(CatalogArea.PERK_GROUP, query);
    }

    @Bean
    AreaSource spellAreaSource(final RestrictedCatalogQuery<Spell> query) {
        return AreaSource.restricted(CatalogArea.SPELL, query);
    }

    @Bean
    AreaSource spellListAreaSource(final RestrictedCatalogQuery<SpellList> query) {
        return AreaSource.restricted(CatalogArea.SPELL_LIST, query);
    }

    @Bean
    AreaSource miracleAreaSource(final RestrictedCatalogQuery<Miracle> query) {
        return AreaSource.restricted(CatalogArea.MIRACLE, query);
    }

    @Bean
    AreaSource miracleListAreaSource(final RestrictedCatalogQuery<MiracleList> query) {
        return AreaSource.restricted(CatalogArea.MIRACLE_LIST, query);
    }

    @Bean
    AreaSource powerAreaSource(final RestrictedCatalogQuery<Power> query) {
        return AreaSource.restricted(CatalogArea.POWER, query);
    }

    @Bean
    AreaSource powerListAreaSource(final RestrictedCatalogQuery<PowerList> query) {
        return AreaSource.restricted(CatalogArea.POWER_LIST, query);
    }

    @Bean
    AreaSource itemAreaSource(final RestrictedCatalogQuery<Item> query) {
        return AreaSource.restricted(CatalogArea.ITEM, query);
    }

    @Bean
    AreaSource vehicleAreaSource(final RestrictedCatalogQuery<Vehicle> query) {
        return AreaSource.restricted(CatalogArea.VEHICLE, query);
    }

    @Bean
    AreaSource shardAreaSource(final RestrictedCatalogQuery<Shard> query) {
        return AreaSource.restricted(CatalogArea.SHARD, query);
    }

    @Bean
    AreaSource threatAreaSource(final RestrictedCatalogQuery<Threat> query) {
        return AreaSource.restricted(CatalogArea.THREAT, query);
    }

    @Bean
    AreaSource publicationAreaSource(final CatalogQuery<Publication> query) {
        return AreaSource.publications(query);
    }

    @Bean
    AreaSource articleAreaSource(final CatalogQuery<Article> query) {
        return AreaSource.articles(query);
    }

    @Bean
    AreaSource tagAreaSource(final CatalogQuery<Tag> query) {
        return AreaSource.tags(query);
    }

    @Bean
    CatalogBrowseQuery catalogBrowseQuery(
            final List<AreaSource> sources,
            final CatalogUsageObserver usageObserver) {
        return new CatalogBrowseService(sources, usageObserver);
    }

    @Bean
    CodexOverviewQuery codexOverviewQuery(
            final List<AreaSource> sources,
            final CallerProductsPort callerProducts) {
        return new CodexOverviewService(sources, callerProducts);
    }
}
