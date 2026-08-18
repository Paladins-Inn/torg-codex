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

import java.util.HashSet;
import java.util.Set;

/**
 * A piece of equipment, weapon, or armour.
 */
@Entity
@Table(name = "torg_item")
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class Item extends TorgEntity {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_item_products", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "product")
    private Set<String> products = new HashSet<>();

    @Column(length = 32)
    private String type;

    @Column(length = 64)
    private String cosm;

    @Column(name = "axiom_tech", length = 8)
    private String axiomTech;

    @Column(name = "axiom_magic", length = 8)
    private String axiomMagic;

    @Column(length = 32)
    private String price;

    @Column(length = 16)
    private String bonus;

    @Column(length = 64)
    private String ammo;

    @Column(length = 64)
    private String range;

    @Column(columnDefinition = "TEXT")
    private String features;

    @Column(name = "additional_features", columnDefinition = "TEXT")
    private String additionalFeatures;

    @Column(columnDefinition = "TEXT")
    private String text;

    /** Returns {@link #additionalFeatures} rendered and product-gate-filtered by the injected censor. */
    public String getAdditionalFeatures() {
        return render(additionalFeatures);
    }

    /** Returns {@link #text} rendered and product-gate-filtered by the injected censor. */
    public String getText() {
        return render(text);
    }

    /**
     * Returns the raw, un-rendered {@link #additionalFeatures} without applying the censor.
     * Used by the entity&#8596;domain persistence mapper, which must preserve the
     * exact persisted value; product-gate rendering is a presentation concern.
     */
    public String getRawAdditionalFeatures() {
        return additionalFeatures;
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
