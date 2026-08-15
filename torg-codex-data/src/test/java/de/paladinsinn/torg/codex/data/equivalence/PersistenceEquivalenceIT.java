package de.paladinsinn.torg.codex.data.equivalence;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Persistence read/write equivalence test (T080b, G2 prerequisite for T081) covering all 17
 * catalog areas. Runs the {@link PersistenceEquivalenceHarness} against a real PostgreSQL
 * database (Testcontainers) seeded through the pre-migration Liquibase/CSV load, confirming the
 * new domain-model + MapStruct mapper path round-trips each persisted entity without any
 * field-level drift.
 *
 * <p>Named {@code *IT} so it runs under Failsafe during {@code verify} (consistent with the
 * repository's Testcontainers convention, e.g. {@code LiquibaseImportIT}), keeping the
 * Docker-less {@code mvn test} phase green.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
@DisplayName("Persistence Equivalence Integration Test (all 17 catalog areas)")
class PersistenceEquivalenceIT {

    private static final int MAX_ROWS = 5;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine");

    private final PersistenceEquivalenceHarness harness = new PersistenceEquivalenceHarness();

    @Autowired private ArticleRepository articleRepository;
    @Autowired private ArticleEntityMapper articleMapper;
    @Autowired private CosmRepository cosmRepository;
    @Autowired private CosmEntityMapper cosmMapper;
    @Autowired private ItemRepository itemRepository;
    @Autowired private ItemEntityMapper itemMapper;
    @Autowired private MiracleRepository miracleRepository;
    @Autowired private MiracleEntityMapper miracleMapper;
    @Autowired private MiracleListRepository miracleListRepository;
    @Autowired private MiracleListEntityMapper miracleListMapper;
    @Autowired private PerkRepository perkRepository;
    @Autowired private PerkEntityMapper perkMapper;
    @Autowired private PerkGroupRepository perkGroupRepository;
    @Autowired private PerkGroupEntityMapper perkGroupMapper;
    @Autowired private PowerRepository powerRepository;
    @Autowired private PowerEntityMapper powerMapper;
    @Autowired private PowerListRepository powerListRepository;
    @Autowired private PowerListEntityMapper powerListMapper;
    @Autowired private PublicationRepository publicationRepository;
    @Autowired private PublicationEntityMapper publicationMapper;
    @Autowired private RaceRepository raceRepository;
    @Autowired private RaceEntityMapper raceMapper;
    @Autowired private ShardRepository shardRepository;
    @Autowired private ShardEntityMapper shardMapper;
    @Autowired private SpellRepository spellRepository;
    @Autowired private SpellEntityMapper spellMapper;
    @Autowired private SpellListRepository spellListRepository;
    @Autowired private SpellListEntityMapper spellListMapper;
    @Autowired private TagRepository tagRepository;
    @Autowired private TagEntityMapper tagMapper;
    @Autowired private ThreatRepository threatRepository;
    @Autowired private ThreatEntityMapper threatMapper;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private VehicleEntityMapper vehicleMapper;

    @Test
    @DisplayName("Article: domain-model MapStruct round-trip preserves persisted state")
    void article_roundTripIsEquivalent() {
        harness.assertRoundTripEquivalent(
                "Article", articleRepository, articleMapper::toDomain, articleMapper::toEntity, MAX_ROWS);
    }

    @Test
    @DisplayName("Cosm: domain-model MapStruct round-trip preserves persisted state")
    void cosm_roundTripIsEquivalent() {
        harness.assertRoundTripEquivalent(
                "Cosm", cosmRepository, cosmMapper::toDomain, cosmMapper::toEntity, MAX_ROWS);
    }

    @Test
    @DisplayName("Item: domain-model MapStruct round-trip preserves persisted state")
    void item_roundTripIsEquivalent() {
        harness.assertRoundTripEquivalent(
                "Item", itemRepository, itemMapper::toDomain, itemMapper::toEntity, MAX_ROWS);
    }

    @Test
    @DisplayName("Miracle: domain-model MapStruct round-trip preserves persisted state")
    void miracle_roundTripIsEquivalent() {
        harness.assertRoundTripEquivalent(
                "Miracle", miracleRepository, miracleMapper::toDomain, miracleMapper::toEntity, MAX_ROWS);
    }

    @Test
    @DisplayName("MiracleList: domain-model MapStruct round-trip preserves persisted state")
    void miracleList_roundTripIsEquivalent() {
        harness.assertRoundTripEquivalent(
                "MiracleList", miracleListRepository, miracleListMapper::toDomain, miracleListMapper::toEntity, MAX_ROWS);
    }

    @Test
    @DisplayName("Perk: domain-model MapStruct round-trip preserves persisted state")
    void perk_roundTripIsEquivalent() {
        harness.assertRoundTripEquivalent(
                "Perk", perkRepository, perkMapper::toDomain, perkMapper::toEntity, MAX_ROWS);
    }

    @Test
    @DisplayName("PerkGroup: domain-model MapStruct round-trip preserves persisted state")
    void perkGroup_roundTripIsEquivalent() {
        harness.assertRoundTripEquivalent(
                "PerkGroup", perkGroupRepository, perkGroupMapper::toDomain, perkGroupMapper::toEntity, MAX_ROWS);
    }

    @Test
    @DisplayName("Power: domain-model MapStruct round-trip preserves persisted state")
    void power_roundTripIsEquivalent() {
        harness.assertRoundTripEquivalent(
                "Power", powerRepository, powerMapper::toDomain, powerMapper::toEntity, MAX_ROWS);
    }

    @Test
    @DisplayName("PowerList: domain-model MapStruct round-trip preserves persisted state")
    void powerList_roundTripIsEquivalent() {
        harness.assertRoundTripEquivalent(
                "PowerList", powerListRepository, powerListMapper::toDomain, powerListMapper::toEntity, MAX_ROWS);
    }

    @Test
    @DisplayName("Publication: domain-model MapStruct round-trip preserves persisted state")
    void publication_roundTripIsEquivalent() {
        harness.assertRoundTripEquivalent(
                "Publication", publicationRepository, publicationMapper::toDomain, publicationMapper::toEntity, MAX_ROWS);
    }

    @Test
    @DisplayName("Race: domain-model MapStruct round-trip preserves persisted state")
    void race_roundTripIsEquivalent() {
        harness.assertRoundTripEquivalent(
                "Race", raceRepository, raceMapper::toDomain, raceMapper::toEntity, MAX_ROWS);
    }

    @Test
    @DisplayName("Shard: domain-model MapStruct round-trip preserves persisted state")
    void shard_roundTripIsEquivalent() {
        harness.assertRoundTripEquivalent(
                "Shard", shardRepository, shardMapper::toDomain, shardMapper::toEntity, MAX_ROWS);
    }

    @Test
    @DisplayName("Spell: domain-model MapStruct round-trip preserves persisted state")
    void spell_roundTripIsEquivalent() {
        harness.assertRoundTripEquivalent(
                "Spell", spellRepository, spellMapper::toDomain, spellMapper::toEntity, MAX_ROWS);
    }

    @Test
    @DisplayName("SpellList: domain-model MapStruct round-trip preserves persisted state")
    void spellList_roundTripIsEquivalent() {
        harness.assertRoundTripEquivalent(
                "SpellList", spellListRepository, spellListMapper::toDomain, spellListMapper::toEntity, MAX_ROWS);
    }

    @Test
    @DisplayName("Tag: domain-model MapStruct round-trip preserves persisted state")
    void tag_roundTripIsEquivalent() {
        harness.assertRoundTripEquivalent(
                "Tag", tagRepository, tagMapper::toDomain, tagMapper::toEntity, MAX_ROWS);
    }

    @Test
    @DisplayName("Threat: domain-model MapStruct round-trip preserves persisted state")
    void threat_roundTripIsEquivalent() {
        harness.assertRoundTripEquivalent(
                "Threat", threatRepository, threatMapper::toDomain, threatMapper::toEntity, MAX_ROWS);
    }

    @Test
    @DisplayName("Vehicle: domain-model MapStruct round-trip preserves persisted state")
    void vehicle_roundTripIsEquivalent() {
        harness.assertRoundTripEquivalent(
                "Vehicle", vehicleRepository, vehicleMapper::toDomain, vehicleMapper::toEntity, MAX_ROWS);
    }
}
