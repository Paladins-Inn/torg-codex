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
 * Full-database persisted-data snapshot comparison (T129a, G2 prerequisite completion gate for
 * T130). Extends the sampled {@link PersistenceEquivalenceHarness} approach used by
 * {@code PersistenceEquivalenceIT} (T080b) into an exhaustive check: every persisted row of
 * every one of the 17 catalog tables, seeded via the same pre-migration Liquibase/CSV load
 * used throughout this migration, is round-tripped (JPA entity &rarr; domain model via
 * MapStruct &rarr; JPA entity via the inverse mapper) and compared field-by-field against the
 * original raw-persisted entity snapshot from the very same Testcontainers-backed PostgreSQL
 * database. Any drift for any row of any area fails the build.
 *
 * <p>This is the concrete "persisted-data snapshot comparison" referenced by task T130's
 * completion gate for SC-001 through SC-006: it proves the fully-migrated
 * domain-model/adapter path is byte-for-byte equivalent to the pre-migration path for the
 * <em>entire</em> dataset, not merely a bounded sample.
 *
 * <p>Named {@code *Test} (not {@code *IT}) deliberately: unlike the sampled
 * {@code PersistenceEquivalenceIT}, which runs under Failsafe during {@code verify}, this
 * exhaustive full-table comparison is still fast enough (the fixture dataset is small) to run
 * under Surefire during the Docker-requiring {@code mvn test} phase already used by every
 * other Testcontainers-backed test in this module; keeping it as a {@code Test} means it is
 * covered by the same "cannot be skipped to pass a task" guarantee verified in T124.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
@DisplayName("Persisted-Data Snapshot Comparison (T129a, all rows, all 17 catalog areas)")
class PersistedDataSnapshotComparisonTest {

    // T129a: no row cap - compares every persisted row, not a sample, across all 17 tables.
    private static final int MAX_ROWS = Integer.MAX_VALUE;

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
