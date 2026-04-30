package de.paladinsinn.torg.codex.data.model;

import java.util.UUID;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A hierarchical tag used to categorise entities.
 *
 * <p>Tags form a tree via the {@link #parent} field, which stores the slug id
 * of the parent tag ({@code null} for root tags).
 */
@Entity
@Table(name = "torg_tag")
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class Tag extends TorgEntity {

    /**
     * Slug id of the parent tag, or {@code null} for a root-level tag.
     * Self-referencing is kept deliberately lightweight as a plain string
     * to avoid circular JPA relationships.
     */
    @Column(name = "parent_id")
    private UUID parent;
}
