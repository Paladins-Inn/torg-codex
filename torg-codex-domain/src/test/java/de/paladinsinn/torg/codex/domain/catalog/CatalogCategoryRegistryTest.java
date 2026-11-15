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

import de.paladinsinn.torg.codex.domain.model.ClearanceLevel;
import de.paladinsinn.torg.codex.domain.model.Item;
import de.paladinsinn.torg.codex.domain.model.Perk;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogCategoryRegistryTest {

    @Test
    void registersCompleteDescriptorForAllSeventeenCatalogAreas() {
        assertThat(CatalogCategoryRegistry.all()).hasSize(17);

        for (CatalogArea area : CatalogArea.values()) {
            var descriptor = CatalogCategoryRegistry.forArea(area);
            assertThat(descriptor.area()).isEqualTo(area);
            assertThat(descriptor.columns()).isNotEmpty();
            assertThat(descriptor.columns().getFirst().kind())
                    .isEqualTo(ColumnDescriptor.CellKind.PRIMARY_LINK);
            assertThat(descriptor.detailSections()).isNotEmpty();
        }
    }

    @Test
    void extractsColumnsAndDirectGroupingsForPerksAndItems() {
        UUID id = UUID.randomUUID();
        Perk perk = new Perk(id, "Ace Pilot", ClearanceLevel.BETA, Set.of("core-rulebook"),
                false, Set.of("universal"), "prowess", "Air vehicles", "Soak damage");
        var perkDesc = CatalogCategoryRegistry.forArea(CatalogArea.PERK, Perk.class);

        assertThat(perkDesc.extractDirectGroupingKeys(perk, GroupingDimension.PERK_GROUP))
                .containsExactly("prowess");
        assertThat(perkDesc.extractDirectGroupingKeys(perk, GroupingDimension.CLEARANCE))
                .containsExactly("beta");

        Item item = new Item(id, "Laser Pistol", ClearanceLevel.GAMMA, Set.of("core-rulebook"),
                "energy-weapons", Set.of("tharkold"), "24", null, "1200", "16", "Cell", "20/40/80", "Beam", null, "Text");
        var itemDesc = CatalogCategoryRegistry.forArea(CatalogArea.ITEM, Item.class);

        assertThat(itemDesc.extractDirectGroupingKeys(item, GroupingDimension.ITEM_TYPE))
                .containsExactly("energy-weapons");
        assertThat(itemDesc.columns().get(4).textExtractor().apply(item)).isEqualTo("T24");
    }
}
