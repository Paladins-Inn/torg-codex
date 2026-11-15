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
 * Structured grouping and facet dimensions used for generic catalog filtering and group-specific
 * usage counters (FR-016, FR-017).
 */
@Getter
@RequiredArgsConstructor
public enum GroupingDimension {
    PERK_GROUP("perk-group", "group", "Perk Group"),
    SPELL_LIST("spell-list", "list", "Spell List"),
    MIRACLE_LIST("miracle-list", "list", "Miracle List"),
    POWER_LIST("power-list", "list", "Power List"),
    TAG("tag", "tag", "Tag"),
    ITEM_TYPE("item-type", "type", "Item Type"),
    VEHICLE_TYPE("vehicle-type", "type", "Vehicle Type"),
    CLEARANCE("clearance", "clearance", "Clearance"),
    RACE("race", "race", "Race");

    private final String tag;
    private final String queryParam;
    private final String displayName;
}
