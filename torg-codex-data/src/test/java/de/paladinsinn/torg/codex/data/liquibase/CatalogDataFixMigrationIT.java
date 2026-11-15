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

package de.paladinsinn.torg.codex.data.liquibase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the domain-level DRM data correction (feature 005, FR-018, ADR-019) against a real
 * PostgreSQL schema without catalog data. The test seeds its own rows and executes
 * {@code 2026-11-14-001-migrate.sql} twice to prove exact results and idempotency.
 */
@SpringBootTest(properties = "spring.liquibase.change-log=classpath:liquibase/catalog-schema-only.yaml")
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Catalog data correction for domain-level DRM")
class CatalogDataFixMigrationIT {

    private static final String MIGRATION = "db/changelog/2026-11-14-001-migrate.sql";

    private static final List<String> CORE_SLUGS = List.of(
            "aysle", "core-earth", "cyberpapacy", "living-land", "nile-empire",
            "orrorsh", "pan-pacifica", "tharkold", "universal", "unknown");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void cleanCatalogTables() {
        jdbc.update("DELETE FROM torg_perk_group_products");
        jdbc.update("DELETE FROM torg_perk_products");
        jdbc.update("DELETE FROM torg_perk_cosms");
        jdbc.update("DELETE FROM torg_perk");
        jdbc.update("DELETE FROM torg_perk_group");
        jdbc.update("DELETE FROM torg_cosm_products");
        jdbc.update("DELETE FROM torg_cosm");
    }

    @Test
    @DisplayName("Assigns the free tier to unassigned core cosms and derives exact perk-group products")
    void correctsCatalogDataExactlyAndIdempotently() throws Exception {
        Map<String, UUID> cosms = new HashMap<>();
        for (String slug : CORE_SLUGS) {
            cosms.put(slug, insertCosm(slug));
        }
        cosms.put("kanawa", insertCosm("kanawa"));
        jdbc.update("INSERT INTO torg_cosm_products (cosm_id, product) VALUES (?, ?)",
                cosms.get("aysle"), "sourcebook-aysle");

        UUID combat = insertGroup("Test Combat");
        insertPerk("test-combat", "core-rulebook");
        insertPerk("test-combat", "sourcebook-aysle", "core-rulebook");
        insertPerk("other-group", "sourcebook-tharkold");
        insertGroupProduct(combat, "groups");
        insertGroupProduct(combat, "stale-product");
        insertGroupProduct(combat, "core-rulebook");

        UUID productless = insertGroup("Productless Perks");
        insertPerk("productless-perks");
        insertGroupProduct(productless, "groups");

        UUID withoutPerks = insertGroup("Lonely Group");
        insertGroupProduct(withoutPerks, "stale-product");

        for (int execution = 1; execution <= 2; execution++) {
            runMigration();

            for (String slug : CORE_SLUGS) {
                Set<String> expected = "aysle".equals(slug) ? Set.of("sourcebook-aysle") : Set.of("core-rulebook");
                assertThat(cosmProducts(cosms.get(slug)))
                        .as("products of core cosm %s after execution %d", slug, execution)
                        .isEqualTo(expected);
            }
            assertThat(cosmProducts(cosms.get("kanawa")))
                    .as("an unrelated cosm stays unassigned")
                    .isEmpty();

            assertThat(groupProducts(combat))
                    .as("group products equal the exact union of its perks' products")
                    .isEqualTo(Set.of("core-rulebook", "sourcebook-aysle"));
            assertThat(groupProducts(productless))
                    .as("a group without product-bearing perks ends unassigned (fail-closed)")
                    .isEmpty();
            assertThat(groupProducts(withoutPerks))
                    .as("a group without perks ends unassigned (fail-closed)")
                    .isEmpty();

            assertThat(count("SELECT count(*) FROM torg_perk_group_products WHERE product = 'groups'")).isZero();
            assertNoDuplicates();
        }
    }

    @Test
    @DisplayName("Is a no-op when no affected rows exist")
    void isNoOpWithoutAffectedRows() throws Exception {
        UUID kanawa = insertCosm("kanawa");
        jdbc.update("INSERT INTO torg_cosm_products (cosm_id, product) VALUES (?, ?)", kanawa, "sourcebook-kanawa");
        UUID group = insertGroup("Exact Group");
        insertPerk("exact-group", "core-rulebook");
        insertGroupProduct(group, "core-rulebook");

        List<Map<String, Object>> cosmProductsBefore = jdbc.queryForList(
                "SELECT cosm_id, product FROM torg_cosm_products ORDER BY cosm_id, product");
        List<Map<String, Object>> groupProductsBefore = jdbc.queryForList(
                "SELECT group_id, product FROM torg_perk_group_products ORDER BY group_id, product");

        runMigration();
        runMigration();

        assertThat(jdbc.queryForList("SELECT cosm_id, product FROM torg_cosm_products ORDER BY cosm_id, product"))
                .isEqualTo(cosmProductsBefore);
        assertThat(jdbc.queryForList(
                "SELECT group_id, product FROM torg_perk_group_products ORDER BY group_id, product"))
                .isEqualTo(groupProductsBefore);
    }

    @Test
    @DisplayName("Runs on empty catalog tables without changes")
    void runsOnEmptyTables() throws Exception {
        runMigration();
        runMigration();

        assertThat(count("SELECT count(*) FROM torg_cosm_products")).isZero();
        assertThat(count("SELECT count(*) FROM torg_perk_group_products")).isZero();
    }

    private void runMigration() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(MIGRATION));
        }
    }

    private UUID insertCosm(String slug) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO torg_cosm (id, name, slug) VALUES (?, ?, ?)", id, "Cosm " + slug, slug);
        return id;
    }

    private UUID insertGroup(String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO torg_perk_group (id, name) VALUES (?, ?)", id, name);
        return id;
    }

    private void insertPerk(String perkGroup, String... products) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO torg_perk (id, name, contradiction, perk_group) VALUES (?, ?, false, ?)",
                id, "Perk " + id, perkGroup);
        for (String product : products) {
            jdbc.update("INSERT INTO torg_perk_products (perk_id, product) VALUES (?, ?)", id, product);
        }
    }

    private void insertGroupProduct(UUID groupId, String product) {
        jdbc.update("INSERT INTO torg_perk_group_products (group_id, product) VALUES (?, ?)", groupId, product);
    }

    private Set<String> cosmProducts(UUID cosmId) {
        return new TreeSet<>(jdbc.queryForList(
                "SELECT product FROM torg_cosm_products WHERE cosm_id = ?", String.class, cosmId));
    }

    private Set<String> groupProducts(UUID groupId) {
        return new TreeSet<>(jdbc.queryForList(
                "SELECT product FROM torg_perk_group_products WHERE group_id = ?", String.class, groupId));
    }

    private void assertNoDuplicates() {
        assertThat(count("SELECT count(*) FROM (SELECT cosm_id, product FROM torg_cosm_products "
                + "GROUP BY cosm_id, product HAVING count(*) > 1) duplicates"))
                .as("no duplicate cosm product rows")
                .isZero();
        assertThat(count("SELECT count(*) FROM (SELECT group_id, product FROM torg_perk_group_products "
                + "GROUP BY group_id, product HAVING count(*) > 1) duplicates"))
                .as("no duplicate perk-group product rows")
                .isZero();
    }

    private int count(String sql) {
        Integer result = jdbc.queryForObject(sql, Integer.class);
        return result == null ? 0 : result;
    }
}
