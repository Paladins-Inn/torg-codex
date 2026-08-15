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
 * A named collection of miracles for a faith tradition.
 */
@Entity
@Table(name = "torg_miracle_list")
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class MiracleList extends TorgEntity {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_miracle_list_products", joinColumns = @JoinColumn(name = "list_id"))
    @Column(name = "product")
    private Set<String> products = new HashSet<>();

    @Column(length = 64)
    private String cosm;

    @Column(name = "unlocking_perk", length = 128)
    private String unlockingPerk;

    @ElementCollection
    @CollectionTable(name = "torg_miracle_list_entries", joinColumns = @JoinColumn(name = "list_id"))
    @OrderColumn(name = "entry_order")
    @Column(name = "miracle_id")
    private List<UUID> miracles = new ArrayList<>();

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
