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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package de.paladinsinn.torg.codex.data.liquibase;

import de.kaiserpfalz.liquibase.DatabaseChangeLog;
import de.kaiserpfalz.liquibase.DatabaseChangeLogLock;
import de.kaiserpfalz.liquibase.DatabaseChangeLogLockRepository;
import de.kaiserpfalz.liquibase.DatabaseChangeLogRepository;
import de.paladinsinn.torg.codex.data.repository.ThreatRepository;
import de.paladinsinn.torg.codex.data.repository.SpellRepository;
import de.paladinsinn.torg.codex.data.repository.PerkRepository;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test that starts the full Spring Boot context against a real
 * PostgreSQL database (via Testcontainers) and verifies that both Liquibase
 * changesets were applied successfully and that the CSV data was imported.
 *
 * <ul>
 *   <li>Changeset id:1 – schema creation ({@code torg-data-entity.yml})</li>
 *   <li>Changeset id:2 – CSV data load ({@code torg-data-load.yml})</li>
 * </ul>
 *
 * <p>Checks that need the separately delivered (proprietary, git-ignored) catalog data are skipped
 * when it is absent, so the test also runs against the public free-tier data set.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Liquibase Import Integration Test")
class LiquibaseImportIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine");

    // -----------------------------------------------------------------------
    // Liquibase tracking repositories
    // -----------------------------------------------------------------------

    @Autowired
    private DatabaseChangeLogRepository changeLogRepository;

    @Autowired
    private DatabaseChangeLogLockRepository changeLogLockRepository;

    // -----------------------------------------------------------------------
    // Domain repositories used to verify data counts after import
    // -----------------------------------------------------------------------

    @Autowired
    private ThreatRepository threatRepository;

    @Autowired
    private SpellRepository spellRepository;

    @Autowired
    private PerkRepository perkRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SpringLiquibase liquibase;

    /** The proprietary data load; only present in fully checked-out local working copies. */
    private static final Path PROPRIETARY_LOAD = Path.of("src", "main", "resources", "db", "load", "torg-data-load.yaml");

    private static void assumeProprietaryData() {
        assumeTrue(Files.exists(PROPRIETARY_LOAD), "proprietary catalog data is not available");
    }

    // -----------------------------------------------------------------------
    // Tests: Liquibase metadata
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Changeset 1 (schema) was executed successfully")
    void changeset1_schemaWasApplied() {
        List<DatabaseChangeLog> logs = changeLogRepository.findByChangesetId("1");

        assertThat(logs)
                .as("Changeset id:1 must exist in DATABASECHANGELOG")
                .isNotEmpty();

        assertThat(logs)
                .allSatisfy(log -> {
                    assertThat(log.getAuthor()).isEqualTo("codex");
                    assertThat(log.getExecType())
                            .as("Changeset 1 must have been executed, not skipped or failed")
                            .isIn("EXECUTED", "RERAN", "MARK_RAN");
                });
    }

    @Test
    @DisplayName("Changeset 2 (data load) was executed successfully")
    void changeset2_dataLoadWasApplied() {
        assumeProprietaryData();
        List<DatabaseChangeLog> logs = changeLogRepository.findByChangesetId("2");

        assertThat(logs)
                .as("Changeset id:2 must exist in DATABASECHANGELOG")
                .isNotEmpty();

        assertThat(logs)
                .allSatisfy(log -> {
                    assertThat(log.getAuthor()).isEqualTo("codex");
                    assertThat(log.getExecType())
                            .as("Changeset 2 must have been executed, not skipped or failed")
                            .isIn("EXECUTED", "RERAN", "MARK_RAN");
                });
    }

    @Test
    @DisplayName("All changesets executed by author 'codex' have status EXECUTED")
    void allCodexChangesetsAreExecuted() {
        List<DatabaseChangeLog> logs =
                changeLogRepository.findByChangeLogIdAuthorOrderByOrderExecuted("codex");

        assertThat(logs)
                .as("There must be at least 2 changesets from author 'codex'")
                .hasSizeGreaterThanOrEqualTo(2);

        assertThat(logs)
                .extracting(DatabaseChangeLog::getExecType)
                .as("All changesets from 'codex' must have been executed successfully")
                .allMatch(execType -> !"FAILED".equals(execType));
    }

    @Test
    @DisplayName("No changeset failed (EXECTYPE != FAILED)")
    void noChangesetFailed() {
        List<DatabaseChangeLog> failedLogs =
                changeLogRepository.findByExecType("FAILED");

        assertThat(failedLogs)
                .as("No changeset must have EXECTYPE = 'FAILED'")
                .isEmpty();
    }

    @Test
    @DisplayName("Liquibase lock is released after migration (locked = false)")
    void liquibaseLockIsReleased() {
        List<DatabaseChangeLogLock> locks = changeLogLockRepository.findAll();

        assertThat(locks)
                .as("DATABASECHANGELOGLOCK must contain exactly one row")
                .hasSize(1);

        assertThat(locks.getFirst().isLocked())
                .as("The Liquibase lock must be released after migration")
                .isFalse();
    }

    // -----------------------------------------------------------------------
    // Tests: Data counts (smoke tests that CSV data was actually loaded)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Threat data was imported (at least 100 threats expected)")
    void threats_dataWasImported() {
        assumeProprietaryData();
        long count = threatRepository.count();
        assertThat(count)
                .as("At least 100 threats must have been imported via CSV")
                .isGreaterThan(100L);
    }

    @Test
    @DisplayName("Spell data was imported (at least 50 spells expected)")
    void spells_dataWasImported() {
        assumeProprietaryData();
        long count = spellRepository.count();
        assertThat(count)
                .as("At least 50 spells must have been imported via CSV")
                .isGreaterThan(50L);
    }

    @Test
    @DisplayName("Perk data was imported (at least 100 perks expected)")
    void perks_dataWasImported() {
        assumeProprietaryData();
        long count = perkRepository.count();
        assertThat(count)
                .as("At least 100 perks must have been imported via CSV")
                .isGreaterThan(100L);
    }

    @Test
    @DisplayName("Reference collections are normalized and legacy columns are removed")
    void multiValueReferencesWereNormalized() {
        Integer encodedCosms = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM torg_perk_cosms WHERE cosm LIKE '[%' OR cosm LIKE '%]'",
                Integer.class);
        Integer encodedPerks = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM torg_miracle_list_unlocking_perks "
                        + "WHERE perk_slug LIKE '[%' OR perk_slug LIKE '%]'",
                Integer.class);
        Integer legacyColumns = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.columns "
                        + "WHERE column_name IN ('cosm', 'unlocking_perk') "
                        + "AND table_name IN ('torg_item', 'torg_miracle_list', 'torg_perk', "
                        + "'torg_power_list', 'torg_shard', 'torg_spell_list', 'torg_threat', 'torg_vehicle')",
                Integer.class);

        assertThat(encodedCosms).isZero();
        assertThat(encodedPerks).isZero();
        assertThat(legacyColumns).isZero();
    }

    @Test
    @DisplayName("Every cosm assignment is found by the cosm filter (SC-002)")
    void everyCosmAssignmentIsFoundByTheFilter() {
        for (Map<String, Object> assignment : jdbcTemplate.queryForList("SELECT perk_id, cosm FROM torg_perk_cosms")) {
            assertThat(perkRepository.findByCosm((String) assignment.get("cosm")))
                    .as("perks of %s", assignment.get("cosm"))
                    .anySatisfy(perk -> assertThat(perk.getId()).isEqualTo(assignment.get("perk_id")));
        }
    }

    @Test
    @DisplayName("The previously encoded references of the full data set are expanded (SC-001, SC-002, SC-009, SC-010)")
    void fullDataSetReferencesAreExpanded() {
        assumeProprietaryData();

        assertThat(multiValued("torg_perk_cosms", "perk_id"))
                .as("54 of the 58 encoded perk rows named several cosms; 4 named a single cosm in list form")
                .isEqualTo(54);
        assertThat(multiValued("torg_miracle_list_cosms", "list_id")).isEqualTo(3);
        assertThat(multiValued("torg_power_list_cosms", "list_id")).isEqualTo(1);
        assertThat(multiValued("torg_miracle_list_unlocking_perks", "list_id")).isEqualTo(5);
        assertThat(jdbcTemplate.queryForList(
                "SELECT cosm FROM torg_perk_cosms WHERE perk_id = '959f00d3-1692-4d2f-8682-407dd5214b1a' ORDER BY cosm",
                String.class))
                .containsExactly("aysle", "core-earth", "cyberpapacy", "living-land", "nile-empire", "orrorsh");
        assertThat(perkRepository.findByCosm("core-earth")).isNotEmpty();
    }

    @Test
    @DisplayName("Running the full changelog again changes neither the history nor the data (FR-007, SC-012)")
    void rerunningTheChangelogChangesNothing() throws Exception {
        String snapshot = "SELECT (SELECT count(*) FROM databasechangelog) || ':' || "
                + "(SELECT count(*) FROM torg_perk_cosms) || ':' || "
                + "(SELECT count(*) FROM torg_miracle_list_unlocking_perks) || ':' || "
                + "(SELECT coalesce(md5(string_agg(coalesce(additional_features, ''), '|' ORDER BY id)), '') FROM torg_item)";
        String before = jdbcTemplate.queryForObject(snapshot, String.class);

        liquibase.afterPropertiesSet();

        assertThat(jdbcTemplate.queryForObject(snapshot, String.class)).isEqualTo(before);
    }

    private int multiValued(String table, String owner) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM (SELECT " + owner + " FROM " + table + " GROUP BY " + owner + " HAVING count(*) > 1) multi",
                Integer.class);
        return count == null ? 0 : count;
    }

    @Test
    @DisplayName("Encoded prose lists were repaired without leaving list literals")
    void proseListsWereRepaired() {
        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT (SELECT count(*) FROM torg_item WHERE additional_features LIKE '[%]') "
                        + "+ (SELECT count(*) FROM torg_threat WHERE quote LIKE '[%]') "
                        + "+ (SELECT count(*) FROM torg_spell_list WHERE notes LIKE '[%]')",
                Integer.class);

        assertThat(remaining).isZero();
    }
}
