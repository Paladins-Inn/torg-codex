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
}
