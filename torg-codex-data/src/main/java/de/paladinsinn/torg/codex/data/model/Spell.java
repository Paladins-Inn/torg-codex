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
 * A magic spell available in Torg Eternity.
 */
@Entity
@Table(name = "torg_spell")
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class Spell extends TorgEntity {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_spell_products", joinColumns = @JoinColumn(name = "spell_id"))
    @Column(name = "product")
    private Set<String> products = new HashSet<>();

    @Column(name = "axiom", length = 32)
    private String axiom;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_spell_required_skills", joinColumns = @JoinColumn(name = "spell_id"))
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
