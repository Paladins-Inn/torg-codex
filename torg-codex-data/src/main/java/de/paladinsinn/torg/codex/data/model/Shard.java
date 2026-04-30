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
}
