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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that starts the full Spring Boot context against a real
 * PostgreSQL database (via Testcontainers) and verifies that both Liquibase
 * changesets were applied successfully and that the CSV data was imported.
 *
 * <ul>
 *   <li>Changeset id:1 – schema creation ({@code torg-data-entity.yml})</li>
 *   <li>Changeset id:2 – CSV data load ({@code torg-data-load.yml})</li>
 * </ul>
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

        assertThat(locks.get(0).isLocked())
                .as("The Liquibase lock must be released after migration")
                .isFalse();
    }

    // -----------------------------------------------------------------------
    // Tests: Data counts (smoke tests that CSV data was actually loaded)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Threat data was imported (at least 100 threats expected)")
    void threats_dataWasImported() {
        long count = threatRepository.count();
        assertThat(count)
                .as("At least 100 threats must have been imported via CSV")
                .isGreaterThan(100L);
    }

    @Test
    @DisplayName("Spell data was imported (at least 50 spells expected)")
    void spells_dataWasImported() {
        long count = spellRepository.count();
        assertThat(count)
                .as("At least 50 spells must have been imported via CSV")
                .isGreaterThan(50L);
    }

    @Test
    @DisplayName("Perk data was imported (at least 100 perks expected)")
    void perks_dataWasImported() {
        long count = perkRepository.count();
        assertThat(count)
                .as("At least 100 perks must have been imported via CSV")
                .isGreaterThan(100L);
    }
}

