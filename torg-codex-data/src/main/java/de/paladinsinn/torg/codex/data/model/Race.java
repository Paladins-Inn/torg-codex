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
 * A playable or non-human race (e.g. Dwarf, Edeinos, Elf, Human).
 */
@Entity
@Table(name = "torg_race")
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class Race extends TorgEntity {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_race_products", joinColumns = @JoinColumn(name = "race_id"))
    @Column(name = "product")
    private Set<String> products = new HashSet<>();

    @Column(nullable = false)
    private boolean major = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_race_attribute_limits", joinColumns = @JoinColumn(name = "race_id"))
    @MapKeyColumn(name = "attribute")
    @Column(name = "max_value")
    private Map<String, Integer> attributeLimits = new HashMap<>();

    @Column(columnDefinition = "TEXT")
    private String abilities;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(name = "perk_text", columnDefinition = "TEXT")
    private String perkText;

    /**
     * Returns {@link #abilities} rendered and product-gate-filtered by the injected censor.
     * Racial ability descriptions may contain entity-reference and game-token markup.
     */
    public String getAbilities() {
        return render(abilities);
    }

    /** Returns {@link #text} rendered and product-gate-filtered by the injected censor. */
    public String getText() {
        return render(text);
    }

    /** Returns {@link #perkText} rendered and product-gate-filtered by the injected censor. */
    public String getPerkText() {
        return render(perkText);
    }
}
