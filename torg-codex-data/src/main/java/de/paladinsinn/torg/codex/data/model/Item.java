package de.paladinsinn.torg.codex.data.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * A piece of equipment, weapon, or armour.
 */
@Entity
@Table(name = "torg_item")
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class Item extends TorgEntity {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "torg_item_products", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "product")
    private Set<String> products = new HashSet<>();

    @Column(length = 32)
    private String type;

    @Column(length = 64)
    private String cosm;

    @Column(name = "axiom_tech", length = 8)
    private String axiomTech;

    @Column(name = "axiom_magic", length = 8)
    private String axiomMagic;

    @Column(length = 32)
    private String price;

    @Column(length = 16)
    private String bonus;

    @Column(length = 64)
    private String ammo;

    @Column(length = 64)
    private String range;

    @Column(columnDefinition = "TEXT")
    private String features;

    @Column(name = "additional_features", columnDefinition = "TEXT")
    private String additionalFeatures;

    @Column(columnDefinition = "TEXT")
    private String text;

    /** Returns {@link #additionalFeatures} rendered and product-gate-filtered by the injected censor. */
    public String getAdditionalFeatures() {
        return render(additionalFeatures);
    }

    /** Returns {@link #text} rendered and product-gate-filtered by the injected censor. */
    public String getText() {
        return render(text);
    }
}
