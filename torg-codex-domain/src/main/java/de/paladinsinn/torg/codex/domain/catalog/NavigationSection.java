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

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Top-level navigation groupings of the Torg Codex reader interface.
 */
@Getter
@RequiredArgsConstructor
public enum NavigationSection {
    STORM_KNIGHTS("storm-knights", "Storm Knights", "bi-person-badge"),
    GEAR_AND_VEHICLES("gear-and-vehicles", "Gear & Vehicles", "bi-shield-shaded"),
    GAME_MASTER("game-master", "Game Master", "bi-compass"),
    REFERENCE("reference", "Reference", "bi-journal-text");

    private final String slug;
    private final String displayName;
    private final String icon;
}
