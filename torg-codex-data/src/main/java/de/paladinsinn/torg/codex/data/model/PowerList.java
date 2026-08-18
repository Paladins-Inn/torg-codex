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
import java.util.UUID;

/**
 * A named collection of psionic / pulp powers for a specific tradition.
 */
@Entity
@Table(name = "torg_power_list")
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class PowerList extends TorgEntity {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_power_list_products", joinColumns = @JoinColumn(name = "list_id"))
    @Column(name = "product")
    private Set<String> products = new HashSet<>();

    @Column(length = 64)
    private String cosm;

    @Column(name = "unlocking_perk", length = 128)
    private String unlockingPerk;

    @ElementCollection
    @CollectionTable(name = "torg_power_list_entries", joinColumns = @JoinColumn(name = "list_id"))
    @OrderColumn(name = "entry_order")
    @Column(name = "power_id")
    private List<UUID> powers = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "disable_if", length = 128)
    private String disableIf;

    /** Returns {@link #text} rendered and product-gate-filtered by the injected censor. */
    public String getText() {
        return render(text);
    }

    /** Returns {@link #notes} rendered and product-gate-filtered by the injected censor. */
    public String getNotes() {
        return render(notes);
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
     * Returns the raw, un-rendered {@link #notes} without applying the censor.
     * Used by the entity&#8596;domain persistence mapper, which must preserve the
     * exact persisted value; product-gate rendering is a presentation concern.
     */
    public String getRawNotes() {
        return notes;
    }
}
