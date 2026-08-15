package de.paladinsinn.torg.codex.data.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * A perk (special ability / advantage) a Storm Knight can acquire.
 */
@Entity
@Table(name = "torg_perk")
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class Perk extends TorgEntity {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_perk_products", joinColumns = @JoinColumn(name = "perk_id"))
    @Column(name = "product")
    private Set<String> products = new HashSet<>();

    @Column(nullable = false)
    private boolean contradiction = false;

    @Column(length = 64)
    private String cosm;

    @Column(name = "perk_group", length = 64)
    private String group;

    @Column(columnDefinition = "TEXT")
    private String prerequisites;

    @Column(columnDefinition = "TEXT")
    private String text;

    /**
     * Returns {@link #prerequisites} rendered and product-gate-filtered by the injected censor.
     * Prerequisites may contain entity-reference markup such as {@code <perk:frightening-aspect>}.
     */
    public String getPrerequisites() {
        return render(prerequisites);
    }

    /** Returns {@link #text} rendered and product-gate-filtered by the injected censor. */
    public String getText() {
        return render(text);
    }

    /**
     * Returns the raw, un-rendered {@link #prerequisites} without applying the censor.
     * Used by the entity&#8596;domain persistence mapper, which must preserve the
     * exact persisted value; product-gate rendering is a presentation concern.
     */
    public String getRawPrerequisites() {
        return prerequisites;
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
