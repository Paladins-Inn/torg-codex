package de.paladinsinn.torg.codex.data.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A standalone article such as credits, changelog, or rules clarifications.
 *
 * <p>Articles are pure textual content. Like all Torg entities they support
 * clearance-level gating via {@link TorgEntity#getClearanceLevel()}.
 * They do not carry product associations.
 */
@Entity
@Table(name = "torg_article")
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class Article extends TorgEntity {

    /** Full article body, potentially containing markup and markdown. */
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
