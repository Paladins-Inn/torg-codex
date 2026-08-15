package de.paladinsinn.torg.codex.data.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * A perk category (group) such as Biotech, Cyberware, Darkness, Faith, etc.
 */
@Entity
@Table(name = "torg_perk_group")
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class PerkGroup extends TorgEntity {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_perk_group_products", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "product")
    private Set<String> products = new HashSet<>();

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(columnDefinition = "TEXT")
    private String infos;

    /** Returns {@link #text} rendered and product-gate-filtered by the injected censor. */
    public String getText() {
        return render(text);
    }

    /** Returns {@link #infos} rendered and product-gate-filtered by the injected censor. */
    public String getInfos() {
        return render(infos);
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
     * Returns the raw, un-rendered {@link #infos} without applying the censor.
     * Used by the entity&#8596;domain persistence mapper, which must preserve the
     * exact persisted value; product-gate rendering is a presentation concern.
     */
    public String getRawInfos() {
        return infos;
    }
}
