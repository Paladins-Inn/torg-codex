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
import de.paladinsinn.torg.codex.domain.model.Article;
import de.paladinsinn.torg.codex.domain.model.Cosm;
import de.paladinsinn.torg.codex.domain.model.Item;
import de.paladinsinn.torg.codex.domain.model.Miracle;
import de.paladinsinn.torg.codex.domain.model.MiracleList;
import de.paladinsinn.torg.codex.domain.model.Perk;
import de.paladinsinn.torg.codex.domain.model.PerkGroup;
import de.paladinsinn.torg.codex.domain.model.Power;
import de.paladinsinn.torg.codex.domain.model.PowerList;
import de.paladinsinn.torg.codex.domain.model.ProductBoundEntry;
import de.paladinsinn.torg.codex.domain.model.Publication;
import de.paladinsinn.torg.codex.domain.model.Race;
import de.paladinsinn.torg.codex.domain.model.Shard;
import de.paladinsinn.torg.codex.domain.model.Spell;
import de.paladinsinn.torg.codex.domain.model.SpellList;
import de.paladinsinn.torg.codex.domain.model.Tag;
import de.paladinsinn.torg.codex.domain.model.Threat;
import de.paladinsinn.torg.codex.domain.model.Vehicle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ProductBoundEntryTest {

    static Stream<Arguments> productBoundRecords() {
        return Stream.of(
                Arguments.of(Cosm.class, EntityType.COSM),
                Arguments.of(Item.class, EntityType.ITEM),
                Arguments.of(Miracle.class, EntityType.MIRACLE),
                Arguments.of(MiracleList.class, EntityType.MIRACLE_LIST),
                Arguments.of(Perk.class, EntityType.PERK),
                Arguments.of(PerkGroup.class, EntityType.GROUP),
                Arguments.of(Power.class, EntityType.POWER),
                Arguments.of(PowerList.class, EntityType.POWER_LIST),
                Arguments.of(Race.class, EntityType.RACE),
                Arguments.of(Shard.class, EntityType.SHARD),
                Arguments.of(Spell.class, EntityType.SPELL),
                Arguments.of(SpellList.class, EntityType.SPELL_LIST),
                Arguments.of(Threat.class, EntityType.THREAT),
                Arguments.of(Vehicle.class, EntityType.VEHICLE));
    }

    @ParameterizedTest
    @MethodSource("productBoundRecords")
    void recordIsProductBoundAndNamesItsArea(Class<?> recordType, EntityType area) throws Exception {
        assertThat(ProductBoundEntry.class).isAssignableFrom(recordType);

        UUID id = UUID.randomUUID();
        ProductBoundEntry entry = (ProductBoundEntry) instantiate(recordType, id, "Name", Set.of("core-rulebook"));

        assertThat(entry.entityType()).isEqualTo(area);
        assertThat(entry.id()).isEqualTo(id);
        assertThat(entry.name()).isEqualTo("Name");
        assertThat(entry.products()).containsExactly("core-rulebook");
    }

    @Test
    void everyEntityTypeHasExactlyOneProductBoundRecord() {
        List<EntityType> areas = productBoundRecords().map(arguments -> (EntityType) arguments.get()[1]).toList();

        assertThat(areas).containsExactlyInAnyOrder(EntityType.values());
    }

    @Test
    void referenceDataIsNotProductBound() {
        assertThat(ProductBoundEntry.class.isAssignableFrom(Article.class)).isFalse();
        assertThat(ProductBoundEntry.class.isAssignableFrom(Publication.class)).isFalse();
        assertThat(ProductBoundEntry.class.isAssignableFrom(Tag.class)).isFalse();
    }

    /**
     * Instantiates a record through its canonical constructor, setting id, name and products and
     * leaving every other component at its default value.
     */
    static Object instantiate(Class<?> recordType, UUID id, String name, Set<String> products) throws Exception {
        RecordComponent[] components = recordType.getRecordComponents();
        Class<?>[] types = Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new);
        Object[] values = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            values[i] = switch (components[i].getName()) {
                case "id" -> id;
                case "name" -> name;
                case "products" -> products;
                default -> defaultValue(components[i].getType());
            };
        }
        Constructor<?> constructor = recordType.getDeclaredConstructor(types);
        return constructor.newInstance(values);
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        return null;
    }
}
