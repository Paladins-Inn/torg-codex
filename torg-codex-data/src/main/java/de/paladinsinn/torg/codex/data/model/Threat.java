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

import java.util.*;
import java.util.stream.Collectors;

/**
 * An NPC, creature, or antagonist (threat) in the Torg Eternity setting.
 */
@Entity
@Table(name = "torg_threat")
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class Threat extends TorgEntity {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_threat_products", joinColumns = @JoinColumn(name = "threat_id"))
    @Column(name = "product")
    private Set<String> products = new HashSet<>();

    /** Slug id of the cosm this threat originates from. */
    @Column(length = 64)
    private String cosm;

    /** Whether this is a unique named character (true) or a generic template (false). */
    @Column(nullable = false)
    private boolean unique = false;

    /** Subtitle or role (e.g. "High Concept Warrior"). */
    @Column(name = "sub_name")
    private String subName;

    /** In-world quote attributed to this character. */
    @Column(columnDefinition = "TEXT")
    private String quote;

    /** Full description including lore and tactics. */
    @Column(columnDefinition = "TEXT")
    private String text;

    // -------------------------------------------------------------------------
    // Attributes
    // -------------------------------------------------------------------------

    /**
     * Charisma attribute value; may contain non-numeric entries like {@code "-"} for
     * creatures without a normal attribute (e.g. undead).
     */
    @Column(name = "attr_charisma", length = 32)
    private String charisma;

    /**
     * Dexterity attribute value; may contain non-numeric entries like {@code "-"} or
     * qualified values like {@code "10 (-1)"}.
     */
    @Column(name = "attr_dexterity", length = 32)
    private String dexterity;

    /** Mind attribute value; may contain non-numeric entries like {@code "-"}. */
    @Column(name = "attr_mind", length = 32)
    private String mind;

    /** Spirit attribute value; may contain non-numeric entries like {@code "-"}. */
    @Column(name = "attr_spirit", length = 32)
    private String spirit;

    /**
     * Strength attribute value; may contain qualified values like {@code "8 (15)"}
     * or non-numeric entries like {@code "-"}.
     */
    @Column(name = "attr_strength", length = 32)
    private String strength;

    // -------------------------------------------------------------------------
    // Skills – key is the skill name (e.g. DODGE), value is the skill total
    // -------------------------------------------------------------------------

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_threat_skills", joinColumns = @JoinColumn(name = "threat_id"))
    @MapKeyColumn(name = "skill")
    @Column(name = "value", length = 64)
    private Map<String, String> skills = new HashMap<>();

    // -------------------------------------------------------------------------
    // Movement
    // -------------------------------------------------------------------------

    /** Walking movement value; also used when movement is a single integer. */
    @Column(name = "move_walk", length = 8)
    private String moveWalk;

    @Column(name = "move_fly", length = 8)
    private String moveFly;

    @Column(name = "move_swim", length = 8)
    private String moveSwim;

    // -------------------------------------------------------------------------
    // Defences
    // -------------------------------------------------------------------------

    /**
     * Toughness value, optionally including armour in parentheses,
     * e.g. {@code "15 (3)"}.
     */
    @Column(length = 32)
    private String tough;

    /**
     * Shock threshold as a string; {@code null} means the threat is immune
     * to shock (originally represented as {@code "-"} in the YAML).
     */
    @Column(length = 32)
    private String shock;

    /**
     * Number of wound boxes as a string to accommodate special values like
     * {@code "Special"}, {@code "1 per vine cluster"}, etc.
     * {@code null} if not applicable.
     */
    @Column(length = 32)
    private String wounds;

    // -------------------------------------------------------------------------
    // Gear & perks
    // -------------------------------------------------------------------------

    /** Slug ids of equipped items. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_threat_equipment", joinColumns = @JoinColumn(name = "threat_id"))
    @Column(name = "item_id")
    private List<String> equipment = new ArrayList<>();

    /** Slug ids of possessed perks. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_threat_perks", joinColumns = @JoinColumn(name = "threat_id"))
    @Column(name = "perk_id")
    private List<String> perks = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Other combat fields
    // -------------------------------------------------------------------------

    /**
     * Description of possibility ownership
     * (e.g. {@code "Never"}, {@code "Common (3)"}).
     */
    @Column(length = 64)
    private String possibilities;

    /**
     * Named special abilities.  Key = ability name, value = raw description.
     * Use {@link #getSpecialAbilities()} after calling {@link #withCensor} for rendered output.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_threat_special_abilities", joinColumns = @JoinColumn(name = "threat_id"))
    @MapKeyColumn(name = "ability_name", length = 128)
    @Column(name = "ability_description", columnDefinition = "TEXT")
    private Map<String, String> specialAbilities = new LinkedHashMap<>();

    /** Returns {@link #quote} rendered and product-gate-filtered by the injected censor. */
    public String getQuote() {
        return render(quote);
    }

    /** Returns {@link #text} rendered and product-gate-filtered by the injected censor. */
    public String getText() {
        return render(text);
    }

    /**
     * Returns a copy of {@link #specialAbilities} with every description rendered
     * and product-gate-filtered by the injected censor. Insertion order is preserved.
     */
    public Map<String, String> getSpecialAbilities() {
        return specialAbilities.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> render(e.getValue()),
                        (a, b) -> b,
                        LinkedHashMap::new
                ));
    }

    /**
     * Returns the raw, un-rendered {@link #quote} without applying the censor.
     * Used by the entity&#8596;domain persistence mapper, which must preserve the
     * exact persisted value; product-gate rendering is a presentation concern.
     */
    public String getRawQuote() {
        return quote;
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
     * Returns the raw, un-rendered {@link #specialAbilities} without applying the censor.
     * Used by the entity&#8596;domain persistence mapper, which must preserve the
     * exact persisted value; product-gate rendering is a presentation concern.
     */
    public java.util.Map<String, String> getRawSpecialAbilities() {
        return specialAbilities;
    }
}
