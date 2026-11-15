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

import de.paladinsinn.torg.codex.domain.model.Spell;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogAccessPolicyTest {

    private final CatalogAccessPolicy policy = new CatalogAccessPolicy();

    @Test
    void anonymousCallerIsGrantedFreeTierEntries() {
        assertThat(policy.decide(spell(Set.of("core-rulebook")), OwnedProducts.anonymous()))
                .isEqualTo(AccessDecision.GRANTED);
    }

    @Test
    void anonymousCallerGetsATeaserForOtherProducts() {
        assertThat(policy.decide(spell(Set.of("sourcebook-aysle")), OwnedProducts.anonymous()))
                .isEqualTo(AccessDecision.TEASER);
    }

    @Test
    void ownerIsGrantedOwnedEntries() {
        assertThat(policy.decide(spell(Set.of("sourcebook-aysle")), OwnedProducts.of(Set.of("sourcebook-aysle"))))
                .isEqualTo(AccessDecision.GRANTED);
    }

    @Test
    void ownerWithoutExplicitCoreAuthorityIsGrantedFreeTierEntries() {
        assertThat(policy.decide(spell(Set.of("core-rulebook")), OwnedProducts.of(Set.of("sourcebook-aysle"))))
                .isEqualTo(AccessDecision.GRANTED);
    }

    @Test
    void ownerGetsATeaserForUnownedProducts() {
        assertThat(policy.decide(spell(Set.of("sourcebook-tharkold")), OwnedProducts.of(Set.of("sourcebook-aysle"))))
                .isEqualTo(AccessDecision.TEASER);
    }

    @Test
    void owningAnyOfMultipleProductsGrantsAccess() {
        assertThat(policy.decide(spell(Set.of("sourcebook-tharkold", "sourcebook-aysle")),
                OwnedProducts.of(Set.of("sourcebook-aysle"))))
                .isEqualTo(AccessDecision.GRANTED);
    }

    @Test
    void staleOrUnknownAuthoritiesGrantNothing() {
        assertThat(policy.decide(spell(Set.of("sourcebook-aysle")), OwnedProducts.of(Set.of("DRIVETHRU_USER", "groups"))))
                .isEqualTo(AccessDecision.TEASER);
    }

    @Test
    void entriesWithoutProductsAreUnassigned() {
        assertThat(policy.decide(spell(null), OwnedProducts.anonymous())).isEqualTo(AccessDecision.UNASSIGNED);
        assertThat(policy.decide(spell(Set.of()), OwnedProducts.anonymous())).isEqualTo(AccessDecision.UNASSIGNED);
    }

    @Test
    void blankProductsCountAsUnassigned() {
        Set<String> products = new HashSet<>();
        products.add(null);
        products.add(" ");

        assertThat(policy.decide(spell(products), OwnedProducts.anonymous())).isEqualTo(AccessDecision.UNASSIGNED);
    }

    @Test
    void unassignedIsDecidedBeforeOwnershipFailClosed() {
        OwnedProducts everything = OwnedProducts.of(Set.of("sourcebook-aysle", "sourcebook-tharkold", "groups"));

        assertThat(policy.decide(spell(Set.of()), everything)).isEqualTo(AccessDecision.UNASSIGNED);
    }

    private static Spell spell(Set<String> products) {
        return new Spell(UUID.randomUUID(), "Fireball", null, products, null, null, null, null, null, null, null);
    }
}
