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

import de.paladinsinn.torg.codex.data.model.TorgEntity;
import de.paladinsinn.torg.codex.data.repository.MiracleListRepository;
import de.paladinsinn.torg.codex.data.repository.PerkRepository;
import de.paladinsinn.torg.codex.data.repository.PowerListRepository;
import de.paladinsinn.torg.codex.data.repository.SpellListRepository;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies multi-valued cosm and unlocking-perk references (feature 004, ADR-018, FR-001 to
 * FR-003, FR-009, FR-010, FR-016, FR-017, FR-020) with seeded rows on the real schema, and that
 * re-running the changelog changes nothing (FR-007, SC-012). Independent of proprietary data.
 */
@SpringBootTest(properties = "spring.liquibase.change-log=classpath:liquibase/catalog-schema-only.yaml")
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Multi-valued cosm and unlocking-perk references")
class MultiValueReferenceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PerkRepository perks;

    @Autowired
    private MiracleListRepository miracleLists;

    @Autowired
    private PowerListRepository powerLists;

    @Autowired
    private SpellListRepository spellLists;

    @Autowired
    private SpringLiquibase liquibase;

    private UUID multiCosmPerk;
    private UUID aysleOnlyPerk;
    private UUID cosmlessPerk;
    private UUID multiPerkMiracleList;
    private UUID unknownPerkMiracleList;
    private UUID powerList;
    private UUID spellList;

    @BeforeEach
    void seed() {
        for (String table : List.of("torg_perk_cosms", "torg_perk_products", "torg_perk",
                "torg_miracle_list_unlocking_perks", "torg_miracle_list", "torg_power_list_unlocking_perks",
                "torg_power_list", "torg_spell_list_unlocking_perks", "torg_spell_list")) {
            jdbc.update("DELETE FROM " + table);
        }
        multiCosmPerk = perk("core-earth", "pan-pacifica", "tharkold");
        jdbc.update("INSERT INTO torg_perk_products (perk_id, product) VALUES (?, 'core-rulebook')", multiCosmPerk);
        aysleOnlyPerk = perk("aysle");
        cosmlessPerk = perk();
        multiPerkMiracleList = list("torg_miracle_list", "miracles", "exemplar-of-light");
        unknownPerkMiracleList = list("torg_miracle_list", "no-such-perk");
        powerList = list("torg_power_list", "psionics");
        spellList = list("torg_spell_list", "magic", "arcane-knowledge");
    }

    @Test
    @DisplayName("A multi-cosm entry is found under each of its cosms and nowhere else")
    void filtersByEveryAssignedCosm() {
        for (String cosm : List.of("core-earth", "pan-pacifica", "tharkold")) {
            assertThat(ids(perks.findByCosm(cosm))).as("perks of %s", cosm).containsExactly(multiCosmPerk);
        }
        assertThat(ids(perks.findByCosm("aysle"))).containsExactly(aysleOnlyPerk);
        assertThat(ids(perks.findByCosm("unused-cosm"))).isEmpty();
        assertThat(ids(perks.findAll())).contains(cosmlessPerk);
        assertThat(perks.findById(cosmlessPerk).orElseThrow().getCosms()).isEmpty();

        assertThat(ids(perks.findByCosmAndProduct("tharkold", "core-rulebook"))).containsExactly(multiCosmPerk);
        assertThat(ids(perks.findByCosmAndProduct("tharkold", "sourcebook-aysle"))).isEmpty();
    }

    @Test
    @DisplayName("A list is found under each of its unlocking perks, including unknown slugs")
    void findsListsByEveryUnlockingPerk() {
        assertThat(ids(miracleLists.findByUnlockingPerk("miracles"))).containsExactly(multiPerkMiracleList);
        assertThat(ids(miracleLists.findByUnlockingPerk("exemplar-of-light"))).containsExactly(multiPerkMiracleList);
        assertThat(ids(miracleLists.findByUnlockingPerk("no-such-perk"))).containsExactly(unknownPerkMiracleList);
        assertThat(ids(powerLists.findByUnlockingPerk("psionics"))).containsExactly(powerList);
        assertThat(ids(spellLists.findByUnlockingPerk("magic"))).containsExactly(spellList);
        assertThat(ids(spellLists.findByUnlockingPerk("arcane-knowledge"))).containsExactly(spellList);
        assertThat(ids(spellLists.findByUnlockingPerk("psionics"))).isEmpty();
    }

    @Test
    @DisplayName("The same cosm or unlocking perk cannot be assigned twice")
    void rejectsDuplicateAssignments() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO torg_perk_cosms (perk_id, cosm) VALUES (?, 'tharkold')", multiCosmPerk))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO torg_miracle_list_unlocking_perks (list_id, perk_slug) VALUES (?, 'miracles')", multiPerkMiracleList))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Running the changelog again changes neither the changelog history nor the data")
    void rerunningTheChangelogChangesNothing() throws Exception {
        Integer changesets = jdbc.queryForObject("SELECT count(*) FROM databasechangelog", Integer.class);
        List<Map<String, Object>> cosms = jdbc.queryForList("SELECT perk_id, cosm FROM torg_perk_cosms ORDER BY perk_id, cosm");
        List<Map<String, Object>> unlocking = jdbc.queryForList(
                "SELECT list_id, perk_slug FROM torg_miracle_list_unlocking_perks ORDER BY list_id, perk_slug");

        liquibase.afterPropertiesSet();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM databasechangelog", Integer.class)).isEqualTo(changesets);
        assertThat(jdbc.queryForList("SELECT perk_id, cosm FROM torg_perk_cosms ORDER BY perk_id, cosm")).isEqualTo(cosms);
        assertThat(jdbc.queryForList(
                "SELECT list_id, perk_slug FROM torg_miracle_list_unlocking_perks ORDER BY list_id, perk_slug"))
                .isEqualTo(unlocking);
    }

    private UUID perk(String... cosms) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO torg_perk (id, name, contradiction) VALUES (?, ?, false)", id, "Perk " + id);
        for (String cosm : cosms) {
            jdbc.update("INSERT INTO torg_perk_cosms (perk_id, cosm) VALUES (?, ?)", id, cosm);
        }
        return id;
    }

    private UUID list(String table, String... unlockingPerks) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO " + table + " (id, name) VALUES (?, ?)", id, "List " + id);
        for (String perk : unlockingPerks) {
            jdbc.update("INSERT INTO " + table + "_unlocking_perks (list_id, perk_slug) VALUES (?, ?)", id, perk);
        }
        return id;
    }

    private static List<UUID> ids(List<? extends TorgEntity> entities) {
        return entities.stream().map(TorgEntity::getId).toList();
    }
}
