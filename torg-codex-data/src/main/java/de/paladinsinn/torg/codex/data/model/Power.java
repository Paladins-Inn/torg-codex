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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A psionic or pulp-power ability.
 */
@Entity
@Table(name = "torg_power")
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class Power extends TorgEntity {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_power_products", joinColumns = @JoinColumn(name = "power_id"))
    @Column(name = "product")
    private Set<String> products = new HashSet<>();

    /** Minimum Psionics (or relevant) Axiom required (may contain text like "12 or 14 (see below)"). */
    @Column(name = "axiom", length = 32)
    private String axiom;

    /**
     * Required skill(s) and their minimum values.
     * Key = skill name (e.g. {@code "TELEPATHY"}), value = minimum skill rating.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_power_required_skills", joinColumns = @JoinColumn(name = "power_id"))
    @MapKeyColumn(name = "skill")
    @Column(name = "required_value")
    private Map<String, Integer> requiredSkills = new HashMap<>();

    @Column(name = "casting_time", length = 64)
    private String castingTime;

    @Embedded
    private DifficultyNumber dn = new DifficultyNumber();

    @Column(length = 64)
    private String range;

    @Column(length = 64)
    private String duration;

    @Column(columnDefinition = "TEXT")
    private String text;

    /** Optional upgrades that can be applied to this power. */
    @Column(columnDefinition = "TEXT")
    private String enhancements;

    /** Restrictions that trade capability for enhancements. */
    @Column(columnDefinition = "TEXT")
    private String limitations;

    /** Returns {@link #text} rendered and product-gate-filtered by the injected censor. */
    public String getText() {
        return render(text);
    }

    /** Returns {@link #enhancements} rendered and product-gate-filtered by the injected censor. */
    public String getEnhancements() {
        return render(enhancements);
    }

    /** Returns {@link #limitations} rendered and product-gate-filtered by the injected censor. */
    public String getLimitations() {
        return render(limitations);
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
     * Returns the raw, un-rendered {@link #enhancements} without applying the censor.
     * Used by the entity&#8596;domain persistence mapper, which must preserve the
     * exact persisted value; product-gate rendering is a presentation concern.
     */
    public String getRawEnhancements() {
        return enhancements;
    }

    /**
     * Returns the raw, un-rendered {@link #limitations} without applying the censor.
     * Used by the entity&#8596;domain persistence mapper, which must preserve the
     * exact persisted value; product-gate rendering is a presentation concern.
     */
    public String getRawLimitations() {
        return limitations;
    }
}
