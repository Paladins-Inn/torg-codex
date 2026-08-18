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
 * An Eternity Shard – a powerful artefact infused with possibility energy.
 */
@Entity
@Table(name = "torg_shard")
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class Shard extends TorgEntity {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_shard_products", joinColumns = @JoinColumn(name = "shard_id"))
    @Column(name = "product")
    private Set<String> products = new HashSet<>();

    @Column(length = 64)
    private String cosm;

    @Column(length = 64)
    private String possibilities;

    @Column(name = "tapping_difficulty", length = 64)
    private String tappingDifficulty;

    @Column(columnDefinition = "TEXT")
    private String purpose;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(columnDefinition = "TEXT")
    private String powers;

    @Column(columnDefinition = "TEXT")
    private String restrictions;

    /** Returns {@link #purpose} rendered and product-gate-filtered by the injected censor. */
    public String getPurpose() {
        return render(purpose);
    }

    /** Returns {@link #text} rendered and product-gate-filtered by the injected censor. */
    public String getText() {
        return render(text);
    }

    /** Returns {@link #powers} rendered and product-gate-filtered by the injected censor. */
    public String getPowers() {
        return render(powers);
    }

    /** Returns {@link #restrictions} rendered and product-gate-filtered by the injected censor. */
    public String getRestrictions() {
        return render(restrictions);
    }

    /**
     * Returns the raw, un-rendered {@link #purpose} without applying the censor.
     * Used by the entity&#8596;domain persistence mapper, which must preserve the
     * exact persisted value; product-gate rendering is a presentation concern.
     */
    public String getRawPurpose() {
        return purpose;
    }

    /**
     * Returns the raw, un-rendered {@link #text} without applying the censor.
     * Used by the entity&#8596;domain persistence mapper, which must preserve the
     * exact persisted value; product-gate rendering is a presentation concern.
     */
    public String getRawText() {
        return text;
    }

    /**
     * Returns the raw, un-rendered {@link #powers} without applying the censor.
     * Used by the entity&#8596;domain persistence mapper, which must preserve the
     * exact persisted value; product-gate rendering is a presentation concern.
     */
    public String getRawPowers() {
        return powers;
    }

    /**
     * Returns the raw, un-rendered {@link #restrictions} without applying the censor.
     * Used by the entity&#8596;domain persistence mapper, which must preserve the
     * exact persisted value; product-gate rendering is a presentation concern.
     */
    public String getRawRestrictions() {
        return restrictions;
    }
}
