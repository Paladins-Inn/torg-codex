package de.paladinsinn.torg.codex.domain.model;

import lombok.Value;

import java.util.UUID;

@Value
public class VehicleWeapon {
    UUID weaponId;
    String ammo;
    String amount;
}
