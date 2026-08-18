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
import de.paladinsinn.torg.codex.application.service.CatalogQueryService;
import de.paladinsinn.torg.codex.application.service.CatalogReferenceQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root that connects framework-specific persistence adapters to use cases.
 *
 * <p>Each catalog area's driving-port bean is typed against its framework-independent domain
 * model and wired to the corresponding outbound {@code Jpa<Area>PersistenceAdapter}, which maps
 * JPA entities to domain models via MapStruct. Product-gate rendering (censoring) is applied at
 * the inbound (web) boundary in the controllers/DTO mappers, not here.
 */
@Configuration
public class CatalogQueryConfiguration {

    @Bean
    CatalogQuery<de.paladinsinn.torg.codex.domain.model.Article> articleCatalogQuery(
            ArticleRepository repository, ArticleEntityMapper mapper) {
        return new CatalogQueryService<>(new JpaArticlePersistenceAdapter(repository, mapper));
    }

    @Bean
    CatalogQuery<de.paladinsinn.torg.codex.domain.model.Cosm> cosmCatalogQuery(
            CosmRepository repository, CosmEntityMapper mapper) {
        return new CatalogQueryService<>(new JpaCosmPersistenceAdapter(repository, mapper));
    }

    @Bean
    CatalogQuery<de.paladinsinn.torg.codex.domain.model.Item> itemCatalogQuery(
            ItemRepository repository, ItemEntityMapper mapper) {
        return new CatalogQueryService<>(new JpaItemPersistenceAdapter(repository, mapper));
    }

    @Bean
    CatalogQuery<de.paladinsinn.torg.codex.domain.model.Miracle> miracleCatalogQuery(
            MiracleRepository repository, MiracleEntityMapper mapper) {
        return new CatalogQueryService<>(new JpaMiraclePersistenceAdapter(repository, mapper));
    }

    @Bean
    CatalogQuery<de.paladinsinn.torg.codex.domain.model.MiracleList> miracleListCatalogQuery(
            MiracleListRepository repository, MiracleListEntityMapper mapper) {
        return new CatalogQueryService<>(new JpaMiracleListPersistenceAdapter(repository, mapper));
    }

    @Bean
    CatalogQuery<de.paladinsinn.torg.codex.domain.model.Perk> perkCatalogQuery(
            PerkRepository repository, PerkEntityMapper mapper) {
        return new CatalogQueryService<>(new JpaPerkPersistenceAdapter(repository, mapper));
    }

    @Bean
    CatalogQuery<de.paladinsinn.torg.codex.domain.model.PerkGroup> perkGroupCatalogQuery(
            PerkGroupRepository repository, PerkGroupEntityMapper mapper) {
        return new CatalogQueryService<>(new JpaPerkGroupPersistenceAdapter(repository, mapper));
    }

    @Bean
    CatalogQuery<de.paladinsinn.torg.codex.domain.model.Power> powerCatalogQuery(
            PowerRepository repository, PowerEntityMapper mapper) {
        return new CatalogQueryService<>(new JpaPowerPersistenceAdapter(repository, mapper));
    }

    @Bean
    CatalogQuery<de.paladinsinn.torg.codex.domain.model.PowerList> powerListCatalogQuery(
            PowerListRepository repository, PowerListEntityMapper mapper) {
        return new CatalogQueryService<>(new JpaPowerListPersistenceAdapter(repository, mapper));
    }

    @Bean
    CatalogQuery<de.paladinsinn.torg.codex.domain.model.Publication> publicationCatalogQuery(
            PublicationRepository repository, PublicationEntityMapper mapper) {
        return new CatalogQueryService<>(new JpaPublicationPersistenceAdapter(repository, mapper));
    }

    @Bean
    CatalogQuery<de.paladinsinn.torg.codex.domain.model.Race> raceCatalogQuery(
            RaceRepository repository, RaceEntityMapper mapper) {
        return new CatalogQueryService<>(new JpaRacePersistenceAdapter(repository, mapper));
    }

    @Bean
    CatalogQuery<de.paladinsinn.torg.codex.domain.model.Shard> shardCatalogQuery(
            ShardRepository repository, ShardEntityMapper mapper) {
        return new CatalogQueryService<>(new JpaShardPersistenceAdapter(repository, mapper));
    }

    @Bean
    CatalogQuery<de.paladinsinn.torg.codex.domain.model.Spell> spellCatalogQuery(
            SpellRepository repository, SpellEntityMapper mapper) {
        return new CatalogQueryService<>(new JpaSpellPersistenceAdapter(repository, mapper));
    }

    @Bean
    CatalogQuery<de.paladinsinn.torg.codex.domain.model.SpellList> spellListCatalogQuery(
            SpellListRepository repository, SpellListEntityMapper mapper) {
        return new CatalogQueryService<>(new JpaSpellListPersistenceAdapter(repository, mapper));
    }

    @Bean
    CatalogQuery<de.paladinsinn.torg.codex.domain.model.Tag> tagCatalogQuery(
            TagRepository repository, TagEntityMapper mapper) {
        return new CatalogQueryService<>(new JpaTagPersistenceAdapter(repository, mapper));
    }

    @Bean
    CatalogQuery<de.paladinsinn.torg.codex.domain.model.Threat> threatCatalogQuery(
            ThreatRepository repository, ThreatEntityMapper mapper) {
        return new CatalogQueryService<>(new JpaThreatPersistenceAdapter(repository, mapper));
    }

    @Bean
    CatalogQuery<de.paladinsinn.torg.codex.domain.model.Vehicle> vehicleCatalogQuery(
            VehicleRepository repository, VehicleEntityMapper mapper) {
        return new CatalogQueryService<>(new JpaVehiclePersistenceAdapter(repository, mapper));
    }

    @Bean
    CatalogReferenceQuery catalogReferenceQuery(
            CosmRepository cosmRepository, PublicationRepository publicationRepository) {
        return new CatalogReferenceQueryService(
                new JpaCatalogReferenceAdapter(cosmRepository, publicationRepository));
    }
}
