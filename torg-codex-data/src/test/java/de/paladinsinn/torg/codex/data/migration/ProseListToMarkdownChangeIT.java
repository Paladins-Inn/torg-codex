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

package de.paladinsinn.torg.codex.data.migration;

import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import org.junit.jupiter.api.BeforeEach;
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

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the prose repair custom change (feature 004, ADR-018, FR-021 to FR-026) on a real
 * PostgreSQL schema with seeded rows, independent of the proprietary catalog data.
 */
@SpringBootTest(properties = "spring.liquibase.change-log=classpath:liquibase/catalog-schema-only.yaml")
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Prose list repair (Liquibase custom change)")
class ProseListToMarkdownChangeIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM torg_item");
        jdbc.update("DELETE FROM torg_threat");
        jdbc.update("DELETE FROM torg_spell_list");
    }

    @Test
    @DisplayName("Converts complete encoded lists in all three fields and leaves everything else byte-identical")
    void repairsEncodedProseExactlyAndIdempotently() throws Exception {
        UUID multiple = item("['puts out fires', 'might be used for tricks']");
        UUID single = item("['only one statement']");
        UUID empty = item("[]");
        UUID markup = item("['[Perk:foo] unlocks it', \"it's **bold**\"]");
        UUID bracketed = item("see [table] for details");
        UUID partial = item("prefix ['a', 'b']");
        UUID malformed = item("['unterminated");
        UUID absent = item(null);
        UUID quote = threat("['To the last breath!']");
        UUID notes = spellList("['first note', 'second note']");

        assertThat(execute()).isEqualTo("Repaired 6 encoded prose values");

        assertThat(itemText(multiple)).isEqualTo("- puts out fires\n- might be used for tricks");
        assertThat(itemText(single)).isEqualTo("only one statement");
        assertThat(itemText(empty)).isEmpty();
        assertThat(itemText(markup)).isEqualTo("- [Perk:foo] unlocks it\n- it's **bold**");
        assertThat(itemText(bracketed)).isEqualTo("see [table] for details");
        assertThat(itemText(partial)).isEqualTo("prefix ['a', 'b']");
        assertThat(itemText(malformed)).isEqualTo("['unterminated");
        assertThat(itemText(absent)).isNull();
        assertThat(jdbc.queryForObject("SELECT quote FROM torg_threat WHERE id = ?", String.class, quote))
                .isEqualTo("To the last breath!");
        assertThat(jdbc.queryForObject("SELECT notes FROM torg_spell_list WHERE id = ?", String.class, notes))
                .isEqualTo("- first note\n- second note");

        var before = jdbc.queryForList("SELECT id, additional_features FROM torg_item ORDER BY id");
        assertThat(execute()).as("second run changes nothing").isEqualTo("Repaired 0 encoded prose values");
        assertThat(jdbc.queryForList("SELECT id, additional_features FROM torg_item ORDER BY id")).isEqualTo(before);
    }

    /** Executes the change like Liquibase does: on a manual-commit connection, committed afterwards. */
    private String execute() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            ProseListToMarkdownChange change = new ProseListToMarkdownChange();
            change.execute(database);
            database.commit();
            return change.getConfirmationMessage();
        }
    }

    private UUID item(String additionalFeatures) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO torg_item (id, name, additional_features) VALUES (?, ?, ?)", id, "Item " + id, additionalFeatures);
        return id;
    }

    private UUID threat(String quote) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO torg_threat (id, name, \"unique\", quote) VALUES (?, ?, false, ?)", id, "Threat " + id, quote);
        return id;
    }

    private UUID spellList(String notes) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO torg_spell_list (id, name, notes) VALUES (?, ?, ?)", id, "List " + id, notes);
        return id;
    }

    private String itemText(UUID id) {
        return jdbc.queryForObject("SELECT additional_features FROM torg_item WHERE id = ?", String.class, id);
    }
}
