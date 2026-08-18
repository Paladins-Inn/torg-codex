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

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * A weapon mount on a {@link Vehicle}.
 *
 * <p>References an {@link Item} by its slug id and carries ammunition/count data.
 */
@Embeddable
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class VehicleWeapon {

    /** Slug id of the mounted weapon (references {@link Item#getId()}). */
    @Column(name = "weapon_id", nullable = false)
    private UUID weaponId;

    /** Ammunition capacity; {@code null} when not applicable. */
    @Column(name = "ammo", length = 16)
    private String ammo;

    /** Number of identical mounts; {@code null} means one. */
    @Column(name = "amount", length = 16)
    private String amount;
}
