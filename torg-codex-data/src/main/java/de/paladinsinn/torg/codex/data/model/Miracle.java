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
 * A faith-based miracle available to priests and holy warriors.
 */
@Entity
@Table(name = "torg_miracle")
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class Miracle extends TorgEntity {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_miracle_products", joinColumns = @JoinColumn(name = "miracle_id"))
    @Column(name = "product")
    private Set<String> products = new HashSet<>();

    @Column(name = "axiom", length = 32)
    private String axiom;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_miracle_required_skills", joinColumns = @JoinColumn(name = "miracle_id"))
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
