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

import lombok.Builder;
import lombok.Value;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Framework-independent domain model for a vehicle.
 */
@Value
@Builder
public class Vehicle {
    @NotNull
    UUID id;
    @NotNull
    String name;
    ClearanceLevel clearanceLevel;
    Set<String> products;
    String type;
    String cosm;
    String axiomTech;
    boolean unique;
    String speed;
    String speedValue;
    String speedMod;
    String size;
    String passengers;
    String maneuverRating;
    String wounds;
    String tough;
    String price;
    List<VehicleWeapon> weaponry;
    String text;
}
