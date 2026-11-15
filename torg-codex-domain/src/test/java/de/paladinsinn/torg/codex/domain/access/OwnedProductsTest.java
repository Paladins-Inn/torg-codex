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

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnedProductsTest {

    @Test
    void freeTierIsTheCoreRulebook() {
        assertThat(OwnedProducts.FREE_TIER).isEqualTo("core-rulebook");
    }

    @Test
    void anonymousOwnsOnlyTheFreeTier() {
        assertThat(OwnedProducts.anonymous().codexIds()).containsExactly(OwnedProducts.FREE_TIER);
    }

    @Test
    void nullInputYieldsTheFreeTier() {
        assertThat(OwnedProducts.of(null)).isEqualTo(OwnedProducts.anonymous());
    }

    @Test
    void emptyInputYieldsTheFreeTier() {
        assertThat(OwnedProducts.of(Set.of())).isEqualTo(OwnedProducts.anonymous());
    }

    @Test
    void ownedProductsAreUnitedWithTheFreeTier() {
        assertThat(OwnedProducts.of(Set.of("sourcebook-aysle")).codexIds())
                .containsExactlyInAnyOrder("sourcebook-aysle", OwnedProducts.FREE_TIER);
    }

    @Test
    void nullAndBlankIdsAreIgnored() {
        Set<String> input = new HashSet<>();
        input.add(null);
        input.add(" ");
        input.add("sourcebook-aysle");

        assertThat(OwnedProducts.of(input).codexIds())
                .containsExactlyInAnyOrder("sourcebook-aysle", OwnedProducts.FREE_TIER);
    }

    @Test
    void unknownIdsAreKeptButHaveNoEffect() {
        OwnedProducts owned = OwnedProducts.of(Set.of("DRIVETHRU_USER"));

        assertThat(owned.codexIds()).contains("DRIVETHRU_USER");
        assertThat(owned.ownsAnyOf(Set.of("sourcebook-aysle"))).isFalse();
    }

    @Test
    void ownsAnyOfMatchesASingleProduct() {
        assertThat(OwnedProducts.of(Set.of("sourcebook-aysle")).ownsAnyOf(Set.of("sourcebook-aysle"))).isTrue();
    }

    @Test
    void ownsAnyOfMatchesTheFreeTierForEveryone() {
        assertThat(OwnedProducts.anonymous().ownsAnyOf(Set.of(OwnedProducts.FREE_TIER))).isTrue();
    }

    @Test
    void ownsAnyOfMatchesOneOfMultipleProducts() {
        assertThat(OwnedProducts.of(Set.of("sourcebook-aysle"))
                .ownsAnyOf(Set.of("sourcebook-tharkold", "sourcebook-aysle"))).isTrue();
    }

    @Test
    void ownsAnyOfRejectsNonMatchingProducts() {
        assertThat(OwnedProducts.of(Set.of("sourcebook-aysle")).ownsAnyOf(Set.of("sourcebook-tharkold"))).isFalse();
    }

    @Test
    void ownsAnyOfRejectsNullAndEmptyProductSets() {
        OwnedProducts owned = OwnedProducts.anonymous();

        assertThat(owned.ownsAnyOf(null)).isFalse();
        assertThat(owned.ownsAnyOf(Set.of())).isFalse();
    }

    @Test
    void codexIdsAreImmutable() {
        Set<String> source = new HashSet<>(Set.of("sourcebook-aysle"));
        OwnedProducts owned = OwnedProducts.of(source);
        source.add("sourcebook-tharkold");

        assertThat(owned.codexIds()).doesNotContain("sourcebook-tharkold");
        assertThatThrownBy(() -> owned.codexIds().add("x")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void valueSemantics() {
        assertThat(OwnedProducts.of(Set.of("a", "b"))).isEqualTo(OwnedProducts.of(Set.of("b", "a")))
                .hasSameHashCodeAs(OwnedProducts.of(Set.of("b", "a")));
    }
}
