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

package de.paladinsinn.torg.codex.domain.model;

import lombok.Getter;

@Getter
public enum ClearanceLevel {
    ALPHA("alpha", "α"),
    BETA("beta", "β"),
    GAMMA("gamma", "γ"),
    DELTA("delta", "Δ"),
    OMEGA("omega", "Ω");

    /**
     *  Returns the full name of this clearance level.
     */
    private final String fullName;
    /**
     *  Returns the symbol of this clearance level.
     */
    private final String symbol;

    ClearanceLevel(String fullName, String symbol) {
        this.fullName = fullName;
        this.symbol = symbol;
    }

    /**
     * Resolves a clearance level from its stored name or symbol.
     *
     * @param value the stored value
     * @return the matching clearance level
     * @throws IllegalArgumentException when the value does not identify a clearance level
     */
    @SuppressWarnings("unused")
    public static ClearanceLevel fromDb(String value) {
        if (value == null || value.isBlank()) {
            return ALPHA;
        }

        for (ClearanceLevel clearanceLevel : values()) {
            if (clearanceLevel.symbol.equals(value)
                    || clearanceLevel.name().equalsIgnoreCase(value)
                    || clearanceLevel.fullName.equalsIgnoreCase(value)) {
                return clearanceLevel;
            }
        }

        throw new IllegalArgumentException("Unknown ClearanceLevel value: " + value);
    }
}
