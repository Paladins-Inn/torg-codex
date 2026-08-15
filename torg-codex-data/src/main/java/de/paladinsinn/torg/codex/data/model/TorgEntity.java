package de.paladinsinn.torg.codex.data.model;

import de.paladinsinn.torg.codex.data.markup.Censor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Common base for all Torg Eternity codex entities.
 *
 * <p>The {@code id} is a UUID generated at persistence time.
 * Each concrete entity class owns its own {@code products} collection to
 * allow proper JPA collection-table naming.
 */
@MappedSuperclass
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public abstract class TorgEntity {

    /** Primary key – auto-generated UUID. */
    @Id
    @GeneratedValue
    private UUID id;

    /** Human-readable name of the entity. */
    private String name;

    /**
     * Access clearance level for product-gating.
     * {@code null} means publicly accessible.
     * Stored as the Greek symbol (e.g. {@code "α"}) via {@link ClearanceLevelConverter}.
     */
    @Column(name = "clearance_level", length = 4)
    private ClearanceLevel clearanceLevel;

    /**
     * The {@link Censor} to use when rendering product-gated text fields.
     * Set via {@link #withCensor(Censor)} before accessing any rendered text getter.
     */
    @Transient
    private Censor censor;

    /**
     * Associates a {@link Censor} with this entity so that censored text getters
     * (e.g. {@code getText()}, {@code getWorldLaws()}) can be called without
     * passing the censor explicitly on every call.
     *
     * @param censor the censor to use for rendering; must not be {@code null}
     * @return this entity (fluent API)
     */
    @SuppressWarnings("unchecked")
    public <T extends TorgEntity> T withCensor(Censor censor) {
        this.censor = censor;
        return (T) this;
    }

    /**
     * Renders {@code rawText} using the {@link Censor} set by {@link #withCensor}.
     *
     * @throws IllegalStateException if no {@code Censor} has been set
     */
    protected String render(String rawText) {
        if (censor == null) {
            throw new IllegalStateException(
                    "No Censor set on " + getClass().getSimpleName()
                    + ". Call withCensor(Censor) before accessing rendered text fields.");
        }
        return censor.apply(rawText);
    }
}
