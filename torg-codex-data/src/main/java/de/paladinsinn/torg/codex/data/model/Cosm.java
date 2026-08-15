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
 * A cosm (reality / alternate dimension) in the Torg Eternity setting.
 */
@Entity
@Table(name = "torg_cosm")
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class Cosm extends TorgEntity {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_cosm_products", joinColumns = @JoinColumn(name = "cosm_id"))
    @Column(name = "product")
    private Set<String> products = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_cosm_axioms", joinColumns = @JoinColumn(name = "cosm_id"))
    @MapKeyColumn(name = "axiom")
    @Column(name = "value")
    private Map<String, Integer> axioms = new HashMap<>();

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(name = "world_laws", columnDefinition = "TEXT")
    private String worldLaws;

    /** Returns {@link #text} rendered and product-gate-filtered by the injected censor. */
    public String getText() {
        return render(text);
    }

    /**
     * Returns {@link #worldLaws} rendered and product-gate-filtered by the injected censor.
     * World law text typically contains extensive {@code <IF:sourcebook-…>} blocks.
     */
    public String getWorldLaws() {
        return render(worldLaws);
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
     * Returns the raw, un-rendered {@link #worldLaws} without applying the censor.
     * Used by the entity&#8596;domain persistence mapper, which must preserve the
     * exact persisted value; product-gate rendering is a presentation concern.
     */
    public String getRawWorldLaws() {
        return worldLaws;
    }
}
