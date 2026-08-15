package de.paladinsinn.torg.codex.api.configuration;

import de.paladinsinn.torg.codex.api.security.CensoringCatalogQuery;
import de.paladinsinn.torg.codex.api.security.CurrentUserCensorFactory;
import de.paladinsinn.torg.codex.data.adapter.out.persistence.JpaCatalogPersistenceAdapter;
import de.paladinsinn.torg.codex.data.adapter.out.persistence.JpaCatalogReferenceAdapter;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.application.port.in.CatalogReferenceQuery;
import de.paladinsinn.torg.codex.application.service.CatalogQueryService;
import de.paladinsinn.torg.codex.application.service.CatalogReferenceQueryService;
import de.paladinsinn.torg.codex.data.model.Article;
import de.paladinsinn.torg.codex.data.model.Cosm;
import de.paladinsinn.torg.codex.data.model.Item;
import de.paladinsinn.torg.codex.data.model.Miracle;
import de.paladinsinn.torg.codex.data.model.MiracleList;
import de.paladinsinn.torg.codex.data.model.Perk;
import de.paladinsinn.torg.codex.data.model.PerkGroup;
import de.paladinsinn.torg.codex.data.model.Power;
import de.paladinsinn.torg.codex.data.model.PowerList;
import de.paladinsinn.torg.codex.data.model.Publication;
import de.paladinsinn.torg.codex.data.model.Race;
import de.paladinsinn.torg.codex.data.model.Shard;
import de.paladinsinn.torg.codex.data.model.Spell;
import de.paladinsinn.torg.codex.data.model.SpellList;
import de.paladinsinn.torg.codex.data.model.Tag;
import de.paladinsinn.torg.codex.data.model.Threat;
import de.paladinsinn.torg.codex.data.model.TorgEntity;
import de.paladinsinn.torg.codex.data.model.Vehicle;
import de.paladinsinn.torg.codex.data.repository.ArticleRepository;
import de.paladinsinn.torg.codex.data.repository.CosmRepository;
import de.paladinsinn.torg.codex.data.repository.ItemRepository;
import de.paladinsinn.torg.codex.data.repository.MiracleListRepository;
import de.paladinsinn.torg.codex.data.repository.MiracleRepository;
import de.paladinsinn.torg.codex.data.repository.PerkGroupRepository;
import de.paladinsinn.torg.codex.data.repository.PerkRepository;
import de.paladinsinn.torg.codex.data.repository.PowerListRepository;
import de.paladinsinn.torg.codex.data.repository.PowerRepository;
import de.paladinsinn.torg.codex.data.repository.PublicationRepository;
import de.paladinsinn.torg.codex.data.repository.RaceRepository;
import de.paladinsinn.torg.codex.data.repository.ShardRepository;
import de.paladinsinn.torg.codex.data.repository.SpellListRepository;
import de.paladinsinn.torg.codex.data.repository.SpellRepository;
import de.paladinsinn.torg.codex.data.repository.TagRepository;
import de.paladinsinn.torg.codex.data.repository.ThreatRepository;
import de.paladinsinn.torg.codex.data.repository.VehicleRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Composition root that connects framework-specific persistence adapters to use cases.
 */
@Configuration
public class CatalogQueryConfiguration {

    @Bean
    CatalogQuery<Article> articleCatalogQuery(ArticleRepository repository, CurrentUserCensorFactory censorFactory) {
        return censoredQuery(repository, null, censorFactory);
    }

    @Bean
    CatalogQuery<Cosm> cosmCatalogQuery(CosmRepository repository, CurrentUserCensorFactory censorFactory) {
        return censoredQuery(repository, null, censorFactory);
    }

    @Bean
    CatalogQuery<Item> itemCatalogQuery(ItemRepository repository, CurrentUserCensorFactory censorFactory) {
        return censoredQuery(repository, repository::findByCosm, censorFactory);
    }

    @Bean
    CatalogQuery<Miracle> miracleCatalogQuery(MiracleRepository repository, CurrentUserCensorFactory censorFactory) {
        return censoredQuery(repository, null, censorFactory);
    }

    @Bean
    CatalogQuery<MiracleList> miracleListCatalogQuery(
            MiracleListRepository repository, CurrentUserCensorFactory censorFactory) {
        return censoredQuery(repository, repository::findByCosm, censorFactory);
    }

    @Bean
    CatalogQuery<Perk> perkCatalogQuery(PerkRepository repository, CurrentUserCensorFactory censorFactory) {
        return censoredQuery(repository, repository::findByCosm, censorFactory);
    }

    @Bean
    CatalogQuery<PerkGroup> perkGroupCatalogQuery(
            PerkGroupRepository repository, CurrentUserCensorFactory censorFactory) {
        return censoredQuery(repository, null, censorFactory);
    }

    @Bean
    CatalogQuery<Power> powerCatalogQuery(PowerRepository repository, CurrentUserCensorFactory censorFactory) {
        return censoredQuery(repository, null, censorFactory);
    }

    @Bean
    CatalogQuery<PowerList> powerListCatalogQuery(
            PowerListRepository repository, CurrentUserCensorFactory censorFactory) {
        return censoredQuery(repository, repository::findByCosm, censorFactory);
    }

    @Bean
    CatalogQuery<Publication> publicationCatalogQuery(
            PublicationRepository repository, CurrentUserCensorFactory censorFactory) {
        return query(repository, null);
    }

    @Bean
    CatalogQuery<Race> raceCatalogQuery(RaceRepository repository, CurrentUserCensorFactory censorFactory) {
        return censoredQuery(repository, null, censorFactory);
    }

    @Bean
    CatalogQuery<Shard> shardCatalogQuery(ShardRepository repository, CurrentUserCensorFactory censorFactory) {
        return censoredQuery(repository, repository::findByCosm, censorFactory);
    }

    @Bean
    CatalogQuery<Spell> spellCatalogQuery(SpellRepository repository, CurrentUserCensorFactory censorFactory) {
        return censoredQuery(repository, null, censorFactory);
    }

    @Bean
    CatalogQuery<SpellList> spellListCatalogQuery(
            SpellListRepository repository, CurrentUserCensorFactory censorFactory) {
        return censoredQuery(repository, repository::findByCosm, censorFactory);
    }

    @Bean
    CatalogQuery<Tag> tagCatalogQuery(TagRepository repository, CurrentUserCensorFactory censorFactory) {
        return censoredQuery(repository, null, censorFactory);
    }

    @Bean
    CatalogQuery<Threat> threatCatalogQuery(ThreatRepository repository, CurrentUserCensorFactory censorFactory) {
        return censoredQuery(repository, repository::findByCosm, censorFactory);
    }

    @Bean
    CatalogQuery<Vehicle> vehicleCatalogQuery(
            VehicleRepository repository, CurrentUserCensorFactory censorFactory) {
        return censoredQuery(repository, repository::findByCosm, censorFactory);
    }

    @Bean
    CatalogReferenceQuery catalogReferenceQuery(
            CosmRepository cosmRepository, PublicationRepository publicationRepository) {
        return new CatalogReferenceQueryService(
                new JpaCatalogReferenceAdapter(cosmRepository, publicationRepository));
    }

    private <T> CatalogQuery<T> query(
            JpaRepository<T, UUID> repository,
            Function<String, List<T>> cosmFinder) {
        return new CatalogQueryService<>(new JpaCatalogPersistenceAdapter<>(repository, cosmFinder));
    }

    private <T extends TorgEntity> CatalogQuery<T> censoredQuery(
            JpaRepository<T, UUID> repository,
            Function<String, List<T>> cosmFinder,
            CurrentUserCensorFactory censorFactory) {
        return new CensoringCatalogQuery<>(
                query(repository, cosmFinder),
                censorFactory);
    }
}
