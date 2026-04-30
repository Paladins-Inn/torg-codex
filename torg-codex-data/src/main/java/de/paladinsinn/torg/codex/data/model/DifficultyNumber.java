package de.paladinsinn.torg.codex.data.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the difficulty number (DN) of a spell, miracle, or power.
 *
 * <p>Either {@link #level} is set to a standard keyword (e.g. {@code "STANDARD"})
 * or {@link #text} holds a free-form expression such as
 * {@code "Target's dodge or Dexterity"}.
 */
@Embeddable
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class DifficultyNumber {

    /** Standard difficulty keyword, e.g. {@code "STANDARD"}. */
    @Column(name = "dn_level", length = 32)
    private String level;

    /** Free-form difficulty expression when no standard level applies. */
    @Column(name = "dn_text")
    private String text;
}
