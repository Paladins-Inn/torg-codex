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

package de.paladinsinn.torg.codex.data.model;

import lombok.Getter;

/**
 * Access clearance levels for product-gated content in the Torg Eternity Codex.
 *
 * <p>Each level carries the full written-out name as well as the corresponding
 * Greek symbol used in the source data.  The {@link #symbol} is the value
 * persisted in the database via {@link ClearanceLevelConverter}.</p>
 *
 * <p>Levels in ascending order: ALPHA &lt; BETA &lt; GAMMA &lt; DELTA &lt; OMEGA.</p>
 */
@Getter
public enum ClearanceLevel {

    /** α – lowest clearance, generally available to all Storm Knights. */
    ALPHA("alpha", "α"),

    /** β – second tier, campaign / sourcebook content. */
    BETA("beta", "β"),

    /** γ – third tier, advanced or restricted material. */
    GAMMA("gamma", "γ"),

    /** Δ – fourth tier, highly classified content. */
    DELTA("delta", "Δ"),

    /** Ω – highest clearance, top-secret Storm Knight intel. */
    OMEGA("omega", "Ω");

    // -------------------------------------------------------------------------

    /** The lower-case English full name of this level. */
    private final String fullName;

    /**
     * The Greek symbol used for this level.
     *
     * <p>This is also the value stored in the database column.</p>
     */
    private final String symbol;

    ClearanceLevel(String fullName, String symbol) {
        this.fullName = fullName;
        this.symbol   = symbol;
    }

    /**
     * Resolves a database value (symbol or upper-case name) to the corresponding
     * enum constant, or {@code null} if the value is blank/null.
     *
     * @throws IllegalArgumentException if the value is non-blank but not recognised
     */
    public static ClearanceLevel fromDb(String value) {
        // if it is not set, it is ALPHA.
        if (value == null || value.isBlank()) {
            return ALPHA;
        }

        for (final ClearanceLevel c : values()) {
            if (c.symbol.equals(value) || c.name().equalsIgnoreCase(value) || c.fullName.equalsIgnoreCase(value)) {
                return c;
            }
        }

        throw new IllegalArgumentException("Unknown ClearanceLevel value: " + value);
    }
}
