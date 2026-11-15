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

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The effective set of products (codex ids) a caller owns.
 *
 * <p>Always contains the free tier {@value #FREE_TIER}, for anonymous and authenticated callers
 * alike (ADR-019). Unknown ids are kept but have no effect on access decisions. Instances are
 * immutable and have value semantics.
 *
 * @param codexIds the owned codex ids; normalized to an unmodifiable set that contains the free tier
 */
public record OwnedProducts(Set<String> codexIds) {

    /** The codex id of the free tier every caller owns. */
    public static final String FREE_TIER = "core-rulebook";

    private static final OwnedProducts ANONYMOUS = new OwnedProducts(Set.of());

    /**
     * Normalizes the owned codex ids: drops {@code null} and blank ids and adds the free tier.
     */
    public OwnedProducts {
        codexIds = Stream.concat(normalize(codexIds).stream(), Stream.of(FREE_TIER))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Creates the effective ownership from the given codex ids.
     *
     * @param codexIds the codex ids the caller owns; may be {@code null}
     * @return the effective ownership, always including the free tier
     */
    public static OwnedProducts of(final Set<String> codexIds) {
        return new OwnedProducts(codexIds);
    }

    /**
     * Returns the ownership of a caller that owns nothing but the free tier.
     *
     * @return the free-tier-only ownership
     */
    public static OwnedProducts anonymous() {
        return ANONYMOUS;
    }

    /**
     * Checks whether at least one of the given products is owned.
     *
     * @param products the products an entry is assigned to; may be {@code null}
     * @return {@code true} if the intersection with the owned products is not empty
     */
    public boolean ownsAnyOf(final Set<String> products) {
        return products != null && products.stream().filter(Objects::nonNull).anyMatch(codexIds::contains);
    }

    /**
     * Returns the non-blank codex ids of the given set.
     *
     * @param ids the ids to normalize; may be {@code null} or contain {@code null} elements
     * @return the unmodifiable set of non-blank ids
     */
    static Set<String> normalize(final Set<String> ids) {
        if (ids == null) {
            return Set.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
