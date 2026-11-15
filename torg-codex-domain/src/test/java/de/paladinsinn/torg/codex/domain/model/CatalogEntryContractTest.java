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

package de.paladinsinn.torg.codex.domain.model;

import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogEntryContractTest {

    @Test
    void allSeventeenDomainRecordsImplementCatalogEntryWithConsistentAreaAndSlug() {
        UUID id = UUID.fromString("12345678-1234-1234-1234-1234567890ab");
        Set<String> products = Set.of("core-rulebook");

        List<CatalogEntry> samples = List.of(
                new Cosm(id, "aysle", "Aysle", ClearanceLevel.ALPHA, products, Map.of("Magic", 24), "text", "laws"),
                new Race(id, "Elf", ClearanceLevel.ALPHA, products, true, Map.of(), "abilities", "text", "perk"),
                new Perk(id, "Ace Pilot", ClearanceLevel.BETA, products, false, Set.of("universal"), "prowess", "prereq", "text"),
                new PerkGroup(id, "Prowess", ClearanceLevel.ALPHA, products, "text", "infos"),
                new Spell(id, "Fireball", ClearanceLevel.ALPHA, products, "14", Map.of(), "1 action", new DifficultyNumber("standard", "12"), "50m", "instant", "text"),
                new SpellList(id, "Arcane", ClearanceLevel.ALPHA, products, Set.of("aysle"), Set.of("spellcraft"), List.of(), "text", "notes", null),
                new Miracle(id, "Blessing", ClearanceLevel.ALPHA, products, "12", Map.of(), "1 action", new DifficultyNumber("standard", "10"), "touch", "scene", "text"),
                new MiracleList(id, "Core Miracles", ClearanceLevel.ALPHA, products, Set.of("living-land"), Set.of("faith"), List.of(), "text", "notes", null),
                new Power(id, "Telekinesis", ClearanceLevel.ALPHA, products, "12", Map.of(), "1 action", new DifficultyNumber("standard", "10"), "10m", "concentration", "text", "enh", "lim"),
                new PowerList(id, "Psionics", ClearanceLevel.ALPHA, products, Set.of("core-earth"), Set.of("psionics"), List.of(), "text", "notes", null),
                new Item(id, "Heavy Pistol", ClearanceLevel.ALPHA, products, "Firearms", Set.of("core-earth"), "18", null, "400", "+1", "9mm", "15/30/50", "Semi-Auto", null, "text"),
                new Vehicle(id, "Sports Car", ClearanceLevel.ALPHA, products, "Ground", Set.of("core-earth"), "20", false, "Fast", "12", "+2", "Medium", "2", "10", "3", "14", "25000", List.of(), "text"),
                new Shard(id, "Heart of Aysle", ClearanceLevel.GAMMA, products, Set.of("aysle"), "5", "DN 14", "purpose", "text", "powers", "restrictions"),
                new Threat(id, "Gospog", ClearanceLevel.ALPHA, products, Set.of("orrorsh"), false, null, "quote", "text", "6", "9", "7", "8", "10", Map.of(), "8", null, null, "11", "10", "1", List.of(), List.of(), "0", Map.of()),
                new Publication(id, "sourcebook-aysle", "Aysle Sourcebook", 295843, null, Set.of(295843)),
                new Article(id, "Changelog", ClearanceLevel.ALPHA, "text"),
                new Tag(id, "Combat", ClearanceLevel.ALPHA, null)
        );

        EnumSet<CatalogArea> covered = EnumSet.noneOf(CatalogArea.class);
        for (CatalogEntry sample : samples) {
            covered.add(sample.catalogArea());
            assertThat(sample.id()).isEqualTo(id);
            assertThat(sample.slug()).isNotBlank();
        }
        assertThat(covered).containsExactlyInAnyOrder(CatalogArea.values());

        Publication pub = (Publication) samples.get(14);
        assertThat(pub.slug()).isEqualTo("sourcebook-aysle");
        assertThat(pub.productUrl()).isEqualTo("https://www.drivethrurpg.com/product/295843");
        assertThat(samples.get(0).cosmSlugs()).containsExactly("aysle");
        assertThat(samples.get(2).slug()).isEqualTo("ace-pilot");
    }
}
