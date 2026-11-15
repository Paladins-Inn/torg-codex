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

package de.paladinsinn.torg.codex.domain.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogSlugsTest {

    private record DummyEntry(UUID id, String name) {
    }

    @Test
    void normalizesNamesDiacriticsAndPunctuationDeterministically() {
        assertThat(CatalogSlugs.normalize("Ace Pilot")).isEqualTo("ace-pilot");
        assertThat(CatalogSlugs.normalize("Lobizón")).isEqualTo("lobizon");
        assertThat(CatalogSlugs.normalize("Heroes of the Possibility Wars #1")).isEqualTo("heroes-of-the-possibility-wars-1");
        assertThat(CatalogSlugs.normalize("  Große   Straße! ")).isEqualTo("grosse-strasse");
        assertThat(CatalogSlugs.normalize(null)).isEmpty();
        assertThat(CatalogSlugs.normalize("   ")).isEmpty();
    }

    @Test
    void detectsAndParsesCanonicalUuidsOnly() {
        UUID expected = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        assertThat(CatalogSlugs.isUuid("550e8400-e29b-41d4-a716-446655440000")).isTrue();
        assertThat(CatalogSlugs.tryParseUuid("550e8400-e29b-41d4-a716-446655440000")).contains(expected);
        assertThat(CatalogSlugs.isUuid("ace-pilot")).isFalse();
        assertThat(CatalogSlugs.tryParseUuid("ace-pilot")).isEmpty();
        assertThat(CatalogSlugs.tryParseUuid(null)).isEmpty();
    }

    @Test
    void disambiguatesSlugCollisionsIndependentlyOfInputOrder() {
        UUID firstId = UUID.fromString("11111111-e29b-41d4-a716-446655440000");
        UUID secondId = UUID.fromString("99999999-e29b-41d4-a716-446655440000");
        DummyEntry first = new DummyEntry(firstId, "Fireball");
        DummyEntry second = new DummyEntry(secondId, "Fireball!");

        Map<UUID, String> forward = CatalogSlugs.resolveCanonicalSlugs(
                List.of(first, second), DummyEntry::id, DummyEntry::name);
        Map<UUID, String> reversed = CatalogSlugs.resolveCanonicalSlugs(
                List.of(second, first), DummyEntry::id, DummyEntry::name);

        assertThat(forward).isEqualTo(reversed);
        assertThat(forward.get(firstId)).isEqualTo("fireball");
        assertThat(forward.get(secondId)).isEqualTo("fireball-99999999");
    }
}
