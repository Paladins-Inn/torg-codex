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
import de.paladinsinn.torg.codex.domain.model.Spell;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntryAccessTest {

    private static final UUID ID = UUID.fromString("6f1c2d3e-0000-4000-8000-000000000001");

    @Test
    void grantedCarriesTheEntry() {
        Spell spell = spell(Set.of("core-rulebook"));

        EntryAccess<Spell> access = EntryAccess.granted(spell);

        assertThat(access).isInstanceOf(EntryAccess.Granted.class);
        assertThat(((EntryAccess.Granted<Spell>) access).entry()).isSameAs(spell);
    }

    @Test
    void teaserCarriesOnlyIdNameTypeAndSortedProducts() {
        EntryAccess<Spell> access = EntryAccess.teaserOf(spell(Set.of("sourcebook-tharkold", "sourcebook-aysle")));

        assertThat(access).isInstanceOf(EntryAccess.Teaser.class);
        EntryAccess.Teaser<Spell> teaser = (EntryAccess.Teaser<Spell>) access;
        assertThat(teaser.id()).isEqualTo(ID);
        assertThat(teaser.name()).isEqualTo("Fireball");
        assertThat(teaser.type()).isEqualTo(EntityType.SPELL);
        assertThat(teaser.requiredProducts()).containsExactly("sourcebook-aysle", "sourcebook-tharkold");
    }

    @Test
    void teaserCannotRetainTheEntryOrProtectedFields() {
        assertThat(Arrays.stream(EntryAccess.Teaser.class.getRecordComponents()).map(RecordComponent::getName))
                .containsExactly("id", "name", "type", "requiredProducts");
    }

    @Test
    void teaserProductsAreImmutable() {
        EntryAccess.Teaser<Spell> teaser = (EntryAccess.Teaser<Spell>) EntryAccess.teaserOf(spell(Set.of("sourcebook-aysle")));

        assertThatThrownBy(() -> teaser.requiredProducts().add("x")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void teaserRequiresAssignedProducts() {
        assertThatThrownBy(() -> EntryAccess.teaserOf(spell(Set.of()))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void absentCarriesNothing() {
        EntryAccess<Spell> access = EntryAccess.absent();

        assertThat(access).isInstanceOf(EntryAccess.Absent.class);
        assertThat(EntryAccess.Absent.class.getRecordComponents()).isEmpty();
        assertThat(access).isEqualTo(EntryAccess.<Spell>absent());
    }

    private static Spell spell(Set<String> products) {
        return new Spell(ID, "Fireball", null, products, "magic", null, null, null, null, null, "secret text");
    }
}
