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

package de.paladinsinn.torg.codex.domain.access;

import de.paladinsinn.torg.codex.domain.markup.EntityType;
import de.paladinsinn.torg.codex.domain.model.ClearanceLevel;
import de.paladinsinn.torg.codex.domain.model.MiracleList;
import de.paladinsinn.torg.codex.domain.model.PowerList;
import de.paladinsinn.torg.codex.domain.model.ReferencesEntries;
import de.paladinsinn.torg.codex.domain.model.SpellList;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReferencesEntriesTest {

    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();
    private final UUID third = UUID.randomUUID();

    @Test
    void spellListReferencesSpells() {
        SpellList list = new SpellList(UUID.randomUUID(), "Spells", ClearanceLevel.ALPHA, Set.of("core-rulebook"),
                Set.of("aysle"), Set.of("magic"), List.of(first, second, third), "text", "notes", "disable");

        assertThat(list).isInstanceOf(ReferencesEntries.class);
        assertThat(list.referencedType()).isEqualTo(EntityType.SPELL);
        assertThat(list.referencedEntries()).containsExactly(first, second, third);

        SpellList filtered = list.withReferencedEntries(List.of(third, first));

        assertThat(filtered.spells()).containsExactly(third, first);
        assertThat(filtered).usingRecursiveComparison().ignoringFields("spells").isEqualTo(list);
    }

    @Test
    void miracleListReferencesMiracles() {
        MiracleList list = new MiracleList(UUID.randomUUID(), "Miracles", ClearanceLevel.BETA, Set.of("core-rulebook"),
                Set.of("orrorsh"), Set.of("faith"), List.of(first, second), "text", "notes", "disable");

        assertThat(list.referencedType()).isEqualTo(EntityType.MIRACLE);
        assertThat(list.referencedEntries()).containsExactly(first, second);

        MiracleList filtered = list.withReferencedEntries(List.of(second));

        assertThat(filtered.miracles()).containsExactly(second);
        assertThat(filtered).usingRecursiveComparison().ignoringFields("miracles").isEqualTo(list);
    }

    @Test
    void powerListReferencesPowers() {
        PowerList list = new PowerList(UUID.randomUUID(), "Powers", ClearanceLevel.GAMMA, Set.of("core-rulebook"),
                Set.of("tharkold"), Set.of("psionics"), List.of(first, second), "text", "notes", "disable");

        assertThat(list.referencedType()).isEqualTo(EntityType.POWER);
        assertThat(list.referencedEntries()).containsExactly(first, second);

        PowerList filtered = list.withReferencedEntries(List.of());

        assertThat(filtered.powers()).isEmpty();
        assertThat(filtered).usingRecursiveComparison().ignoringFields("powers").isEqualTo(list);
    }

    @Test
    void missingReferenceListsAreEmpty() {
        SpellList list = new SpellList(UUID.randomUUID(), "Spells", null, Set.of(), null, null, null, null, null, null);

        assertThat(list.referencedEntries()).isEmpty();
    }
}
