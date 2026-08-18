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

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A vehicle (aircraft, car, ship, etc.) that characters can operate or ride.
 */
@Entity
@Table(name = "torg_vehicle")
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class Vehicle extends TorgEntity {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_vehicle_products", joinColumns = @JoinColumn(name = "vehicle_id"))
    @Column(name = "product")
    private Set<String> products = new HashSet<>();

    /**
     * Vehicle category (e.g. {@code "AIRCRAFT"}, {@code "CAR"}, {@code "BOAT"}).
     */
    @Column(length = 32)
    private String type;

    @Column(length = 64)
    private String cosm;

    /** Minimum Tech Axiom required. */
    @Column(name = "axiom_tech", length = 8)
    private String axiomTech;

    /** Whether this is a unique named vehicle. */
    @Column(nullable = false)
    private boolean unique = false;

    /** Real-world top speed in km/h (or relevant units). */
    @Column(length = 16)
    private String speed;

    /** Game-abstracted speed value used in combat. */
    @Column(name = "speed_value", length = 16)
    private String speedValue;

    /**
     * Speed modifier category:
     * {@code "SLOW"}, {@code "FAST"}, {@code "VERY_FAST"}, etc.
     */
    @Column(name = "speed_mod", length = 16)
    private String speedMod;

    /**
     * Size category: {@code "SMALL"}, {@code "LARGE"}, {@code "VERY_LARGE"}, etc.
     */
    @Column(length = 16)
    private String size;

    /** Maximum number of occupants. */
    @Column(length = 255)
    private String passengers;

    /**
     * Maneuverability rating modifier (positive or negative integer).
     */
    @Column(name = "maneuver_rating", length = 8)
    private String maneuverRating;

    /** Number of wound boxes the vehicle can absorb before being destroyed. */
    @Column(length = 8)
    private String wounds;

    /**
     * Toughness with optional armour value in parentheses, e.g. {@code "24 (4)"}.
     */
    @Column(length = 32)
    private String tough;

    /**
     * Price with wealth level in parentheses, e.g. {@code "12000000 (35)"}.
     */
    @Column(length = 32)
    private String price;

    /**
     * Weapon mounts on this vehicle.
     * Each entry references an {@link Item} by slug id.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_vehicle_weaponry", joinColumns = @JoinColumn(name = "vehicle_id"))
    private List<VehicleWeapon> weaponry = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String text;

    /** Returns {@link #text} rendered and product-gate-filtered by the injected censor. */
    public String getText() {
        return render(text);
    }

    /**
     * Returns the raw, un-rendered {@link #text} without applying the censor.
     * Used by the entity&#8596;domain persistence mapper, which must preserve the
     * exact persisted value; product-gate rendering is a presentation concern.
     */
    public String getRawText() {
        return text;
    }
}
