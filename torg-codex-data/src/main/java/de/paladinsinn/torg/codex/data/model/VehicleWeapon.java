package de.paladinsinn.torg.codex.data.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * A weapon mount on a {@link Vehicle}.
 *
 * <p>References an {@link Item} by its slug id and carries ammunition/count data.
 */
@Embeddable
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class VehicleWeapon {

    /** Slug id of the mounted weapon (references {@link Item#getId()}). */
    @Column(name = "weapon_id", nullable = false)
    private UUID weaponId;

    /** Ammunition capacity; {@code null} when not applicable. */
    @Column(name = "ammo", length = 16)
    private String ammo;

    /** Number of identical mounts; {@code null} means one. */
    @Column(name = "amount", length = 16)
    private String amount;
}
