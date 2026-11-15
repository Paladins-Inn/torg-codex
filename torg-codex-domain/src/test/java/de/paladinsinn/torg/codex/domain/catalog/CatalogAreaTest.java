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

import de.paladinsinn.torg.codex.domain.markup.EntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogAreaTest {

    @Test
    void coversAllSeventeenAreasWithFourteenProductBoundAndThreePublicReferenceAreas() {
        assertThat(CatalogArea.values()).hasSize(17);
        assertThat(CatalogArea.values()).filteredOn(CatalogArea::isProductBound).hasSize(14);
        assertThat(CatalogArea.values()).filteredOn(area -> !area.isProductBound())
                .containsExactlyInAnyOrder(CatalogArea.PUBLICATION, CatalogArea.ARTICLE, CatalogArea.TAG);
    }

    @Test
    void mapsEveryEntityTypeBijectivelyAndAlignsWithEntityReferenceUrls() {
        for (EntityType type : EntityType.values()) {
            CatalogArea area = CatalogArea.fromEntityType(type);
            assertThat(area.productBoundType()).contains(type);
            assertThat(type.urlSegment()).isEqualTo(area.canonicalPath() + "/");
        }
    }

    @Test
    void resolvesCanonicalPathsAndPerkGroupAlias() {
        assertThat(CatalogArea.fromPathSegment("perks")).contains(CatalogArea.PERK);
        assertThat(CatalogArea.fromPathSegment("groups")).contains(CatalogArea.PERK_GROUP);
        assertThat(CatalogArea.fromPathSegment("perk-groups")).contains(CatalogArea.PERK_GROUP);
        assertThat(CatalogArea.PERK_GROUP.isAliasPath("perk-groups")).isTrue();
        assertThat(CatalogArea.PERK_GROUP.isAliasPath("groups")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "ADMIN", "api", "login", "logout", "webjars", "themes", "actuator", "", "   ", "decks"})
    void rejectsReservedAndUnknownPathSegments(String segment) {
        assertThat(CatalogArea.fromPathSegment(segment)).isEmpty();
        assertThat(CatalogArea.fromPathSegment(null)).isEmpty();
    }

    @Test
    void exposesGroupingAndOutcomeMetadata() {
        assertThat(GroupingDimension.values()).hasSize(9);
        assertThat(DetailAccessOutcome.values())
                .extracting(DetailAccessOutcome::getTag)
                .containsExactly("drm-granted", "public-reference", "teaser", "not-found");
        assertThat(NavigationSection.values()).allSatisfy(section ->
                assertThat(CatalogArea.bySection(section)).isNotEmpty());
    }
}
