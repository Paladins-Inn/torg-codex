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

import de.paladinsinn.torg.codex.data.adapter.out.persistence.JpaCatalogReferenceAdapter;
import de.paladinsinn.torg.codex.data.adapter.out.persistence.JpaArticlePersistenceAdapter;
import de.paladinsinn.torg.codex.data.adapter.out.persistence.JpaCosmPersistenceAdapter;
import de.paladinsinn.torg.codex.data.adapter.out.persistence.JpaItemPersistenceAdapter;
import de.paladinsinn.torg.codex.data.adapter.out.persistence.JpaMiraclePersistenceAdapter;
import de.paladinsinn.torg.codex.data.adapter.out.persistence.JpaMiracleListPersistenceAdapter;
import de.paladinsinn.torg.codex.data.adapter.out.persistence.JpaPerkPersistenceAdapter;
import de.paladinsinn.torg.codex.data.adapter.out.persistence.JpaPerkGroupPersistenceAdapter;
import de.paladinsinn.torg.codex.data.adapter.out.persistence.JpaPowerPersistenceAdapter;
import de.paladinsinn.torg.codex.data.adapter.out.persistence.JpaPowerListPersistenceAdapter;
import de.paladinsinn.torg.codex.data.adapter.out.persistence.JpaPublicationPersistenceAdapter;
import de.paladinsinn.torg.codex.data.adapter.out.persistence.JpaRacePersistenceAdapter;
import de.paladinsinn.torg.codex.data.adapter.out.persistence.JpaShardPersistenceAdapter;
import de.paladinsinn.torg.codex.data.adapter.out.persistence.JpaSpellPersistenceAdapter;
import de.paladinsinn.torg.codex.data.adapter.out.persistence.JpaSpellListPersistenceAdapter;
import de.paladinsinn.torg.codex.data.adapter.out.persistence.JpaTagPersistenceAdapter;
import de.paladinsinn.torg.codex.data.adapter.out.persistence.JpaThreatPersistenceAdapter;
import de.paladinsinn.torg.codex.data.adapter.out.persistence.JpaVehiclePersistenceAdapter;
import de.paladinsinn.torg.codex.data.mapper.ArticleEntityMapper;
import de.paladinsinn.torg.codex.data.mapper.CatalogReferenceEntityMapper;
import de.paladinsinn.torg.codex.data.mapper.CosmEntityMapper;
import de.paladinsinn.torg.codex.data.mapper.ItemEntityMapper;
import de.paladinsinn.torg.codex.data.mapper.MiracleEntityMapper;
import de.paladinsinn.torg.codex.data.mapper.MiracleListEntityMapper;
import de.paladinsinn.torg.codex.data.mapper.PerkEntityMapper;
import de.paladinsinn.torg.codex.data.mapper.PerkGroupEntityMapper;
import de.paladinsinn.torg.codex.data.mapper.PowerEntityMapper;
import de.paladinsinn.torg.codex.data.mapper.PowerListEntityMapper;
import de.paladinsinn.torg.codex.data.mapper.PublicationEntityMapper;
import de.paladinsinn.torg.codex.data.mapper.RaceEntityMapper;
import de.paladinsinn.torg.codex.data.mapper.ShardEntityMapper;
import de.paladinsinn.torg.codex.data.mapper.SpellEntityMapper;
import de.paladinsinn.torg.codex.data.mapper.SpellListEntityMapper;
import de.paladinsinn.torg.codex.data.mapper.TagEntityMapper;
import de.paladinsinn.torg.codex.data.mapper.ThreatEntityMapper;
import de.paladinsinn.torg.codex.data.mapper.VehicleEntityMapper;
import de.paladinsinn.torg.codex.data.repository.ArticleRepository;
import de.paladinsinn.torg.codex.data.repository.CosmRepository;
import de.paladinsinn.torg.codex.data.repository.ItemRepository;
import de.paladinsinn.torg.codex.data.repository.MiracleRepository;
import de.paladinsinn.torg.codex.data.repository.MiracleListRepository;
import de.paladinsinn.torg.codex.data.repository.PerkRepository;
import de.paladinsinn.torg.codex.data.repository.PerkGroupRepository;
import de.paladinsinn.torg.codex.data.repository.PowerRepository;
import de.paladinsinn.torg.codex.data.repository.PowerListRepository;
import de.paladinsinn.torg.codex.data.repository.PublicationRepository;
import de.paladinsinn.torg.codex.data.repository.RaceRepository;
import de.paladinsinn.torg.codex.data.repository.ShardRepository;
import de.paladinsinn.torg.codex.data.repository.SpellRepository;
import de.paladinsinn.torg.codex.data.repository.SpellListRepository;
import de.paladinsinn.torg.codex.data.repository.TagRepository;
import de.paladinsinn.torg.codex.data.repository.ThreatRepository;
import de.paladinsinn.torg.codex.data.repository.VehicleRepository;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.application.port.in.CatalogReferenceQuery;
import de.paladinsinn.torg.codex.application.port.in.RestrictedCatalogQuery;
import de.paladinsinn.torg.codex.application.port.out.CallerProductsPort;
import de.paladinsinn.torg.codex.application.port.out.CatalogAccessObserver;
import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.application.port.out.CatalogUsageObserver;
import de.paladinsinn.torg.codex.application.service.CatalogQueryService;
import de.paladinsinn.torg.codex.application.service.CatalogReferenceQueryService;
import de.paladinsinn.torg.codex.application.service.ReferencedEntryResolver;
import de.paladinsinn.torg.codex.application.service.RestrictedCatalogQueryService;
import de.paladinsinn.torg.codex.domain.access.CatalogAccessPolicy;
import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import de.paladinsinn.torg.codex.domain.markup.EntityType;
import de.paladinsinn.torg.codex.domain.model.ProductBoundEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root that connects framework-specific persistence adapters to use cases.
 *
 * <p>Each catalog area's driving-port bean is typed against its framework-independent domain
 * model and wired to the corresponding outbound {@code Jpa<Area>PersistenceAdapter}, which maps
 * JPA entities to domain models via MapStruct. Product-gate rendering (censoring) is applied at
 * the inbound (web) boundary in the controllers/DTO mappers, not here.
 *
 * <p>The 14 product-bound areas are exposed only as {@link RestrictedCatalogQuery} beans that
 * apply the domain-level product-ownership DRM (ADR-019). Unrestricted {@link CatalogQuery} beans
 * exist only for the public reference data: articles, publications and tags. Caller ownership
 * is wired through the {@link CallerProductsPort} interface type only. Spell, miracle and power
 * lists filter their embedded references through a {@link ReferencedEntryResolver} backed by the
 * batched persistence adapter of the referenced area.
 */
@Configuration
@RequiredArgsConstructor
public class CatalogQueryConfiguration {

    private final CallerProductsPort callerProducts;
    private final CatalogAccessObserver observer;
    private final CatalogUsageObserver usageObserver;
    private final CatalogAccessPolicy policy = new CatalogAccessPolicy();

    @Bean
    CatalogQuery<de.paladinsinn.torg.codex.domain.model.Article> articleCatalogQuery(
            ArticleRepository repository, ArticleEntityMapper mapper) {
        return new CatalogQueryService<>(CatalogArea.ARTICLE, new JpaArticlePersistenceAdapter(repository, mapper), usageObserver);
    }

    @Bean
    RestrictedCatalogQuery<de.paladinsinn.torg.codex.domain.model.Cosm> cosmCatalogQuery(
            CosmRepository repository, CosmEntityMapper mapper) {
        return restricted(EntityType.COSM, new JpaCosmPersistenceAdapter(repository, mapper));
    }

    @Bean
    RestrictedCatalogQuery<de.paladinsinn.torg.codex.domain.model.Item> itemCatalogQuery(
            ItemRepository repository, ItemEntityMapper mapper) {
        return restricted(EntityType.ITEM, new JpaItemPersistenceAdapter(repository, mapper));
    }

    @Bean
    RestrictedCatalogQuery<de.paladinsinn.torg.codex.domain.model.Miracle> miracleCatalogQuery(
            MiracleRepository repository, MiracleEntityMapper mapper) {
        return restricted(EntityType.MIRACLE, new JpaMiraclePersistenceAdapter(repository, mapper));
    }

    @Bean
    RestrictedCatalogQuery<de.paladinsinn.torg.codex.domain.model.MiracleList> miracleListCatalogQuery(
            MiracleListRepository repository, MiracleListEntityMapper mapper,
            MiracleRepository miracleRepository, MiracleEntityMapper miracleMapper) {
        return RestrictedCatalogQueryService.withReferences(EntityType.MIRACLE_LIST,
                new JpaMiracleListPersistenceAdapter(repository, mapper), callerProducts, observer, policy,
                new ReferencedEntryResolver<>(new JpaMiraclePersistenceAdapter(miracleRepository, miracleMapper), observer, policy),
                usageObserver);
    }

    @Bean
    RestrictedCatalogQuery<de.paladinsinn.torg.codex.domain.model.Perk> perkCatalogQuery(
            PerkRepository repository, PerkEntityMapper mapper) {
        return restricted(EntityType.PERK, new JpaPerkPersistenceAdapter(repository, mapper));
    }

    @Bean
    RestrictedCatalogQuery<de.paladinsinn.torg.codex.domain.model.PerkGroup> perkGroupCatalogQuery(
            PerkGroupRepository repository, PerkGroupEntityMapper mapper) {
        return restricted(EntityType.GROUP, new JpaPerkGroupPersistenceAdapter(repository, mapper));
    }

    @Bean
    RestrictedCatalogQuery<de.paladinsinn.torg.codex.domain.model.Power> powerCatalogQuery(
            PowerRepository repository, PowerEntityMapper mapper) {
        return restricted(EntityType.POWER, new JpaPowerPersistenceAdapter(repository, mapper));
    }

    @Bean
    RestrictedCatalogQuery<de.paladinsinn.torg.codex.domain.model.PowerList> powerListCatalogQuery(
            PowerListRepository repository, PowerListEntityMapper mapper,
            PowerRepository powerRepository, PowerEntityMapper powerMapper) {
        return RestrictedCatalogQueryService.withReferences(EntityType.POWER_LIST,
                new JpaPowerListPersistenceAdapter(repository, mapper), callerProducts, observer, policy,
                new ReferencedEntryResolver<>(new JpaPowerPersistenceAdapter(powerRepository, powerMapper), observer, policy),
                usageObserver);
    }

    @Bean
    CatalogQuery<de.paladinsinn.torg.codex.domain.model.Publication> publicationCatalogQuery(
            PublicationRepository repository, PublicationEntityMapper mapper) {
        return new CatalogQueryService<>(CatalogArea.PUBLICATION, new JpaPublicationPersistenceAdapter(repository, mapper), usageObserver);
    }

    @Bean
    RestrictedCatalogQuery<de.paladinsinn.torg.codex.domain.model.Race> raceCatalogQuery(
            RaceRepository repository, RaceEntityMapper mapper) {
        return restricted(EntityType.RACE, new JpaRacePersistenceAdapter(repository, mapper));
    }

    @Bean
    RestrictedCatalogQuery<de.paladinsinn.torg.codex.domain.model.Shard> shardCatalogQuery(
            ShardRepository repository, ShardEntityMapper mapper) {
        return restricted(EntityType.SHARD, new JpaShardPersistenceAdapter(repository, mapper));
    }

    @Bean
    RestrictedCatalogQuery<de.paladinsinn.torg.codex.domain.model.Spell> spellCatalogQuery(
            SpellRepository repository, SpellEntityMapper mapper) {
        return restricted(EntityType.SPELL, new JpaSpellPersistenceAdapter(repository, mapper));
    }

    @Bean
    RestrictedCatalogQuery<de.paladinsinn.torg.codex.domain.model.SpellList> spellListCatalogQuery(
            SpellListRepository repository, SpellListEntityMapper mapper,
            SpellRepository spellRepository, SpellEntityMapper spellMapper) {
        return RestrictedCatalogQueryService.withReferences(EntityType.SPELL_LIST,
                new JpaSpellListPersistenceAdapter(repository, mapper), callerProducts, observer, policy,
                new ReferencedEntryResolver<>(new JpaSpellPersistenceAdapter(spellRepository, spellMapper), observer, policy),
                usageObserver);
    }

    @Bean
    CatalogQuery<de.paladinsinn.torg.codex.domain.model.Tag> tagCatalogQuery(
            TagRepository repository, TagEntityMapper mapper) {
        return new CatalogQueryService<>(CatalogArea.TAG, new JpaTagPersistenceAdapter(repository, mapper), usageObserver);
    }

    @Bean
    RestrictedCatalogQuery<de.paladinsinn.torg.codex.domain.model.Threat> threatCatalogQuery(
            ThreatRepository repository, ThreatEntityMapper mapper) {
        return restricted(EntityType.THREAT, new JpaThreatPersistenceAdapter(repository, mapper));
    }

    @Bean
    RestrictedCatalogQuery<de.paladinsinn.torg.codex.domain.model.Vehicle> vehicleCatalogQuery(
            VehicleRepository repository, VehicleEntityMapper mapper) {
        return restricted(EntityType.VEHICLE, new JpaVehiclePersistenceAdapter(repository, mapper));
    }

    @Bean
    CatalogReferenceQuery catalogReferenceQuery(
            CosmRepository cosmRepository, PublicationRepository publicationRepository,
            CatalogReferenceEntityMapper referenceMapper) {
        return new CatalogReferenceQueryService(
                new JpaCatalogReferenceAdapter(cosmRepository, publicationRepository, referenceMapper),
                observer, policy);
    }

    private <T extends ProductBoundEntry> RestrictedCatalogQuery<T> restricted(
            EntityType area, CatalogPersistencePort<T> persistence) {
        return new RestrictedCatalogQueryService<>(area, persistence, callerProducts, observer, policy, usageObserver);
    }
}
