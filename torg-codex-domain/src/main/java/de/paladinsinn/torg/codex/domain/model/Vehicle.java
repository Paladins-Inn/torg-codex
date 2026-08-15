package de.paladinsinn.torg.codex.domain.model;

import lombok.Builder;
import lombok.Value;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Framework-independent domain model for a vehicle.
 */
@Value
@Builder
public class Vehicle {
    @NotNull
    UUID id;
    @NotNull
    String name;
    ClearanceLevel clearanceLevel;
    Set<String> products;
    String type;
    String cosm;
    String axiomTech;
    boolean unique;
    String speed;
    String speedValue;
    String speedMod;
    String size;
    String passengers;
    String maneuverRating;
    String wounds;
    String tough;
    String price;
    List<VehicleWeapon> weaponry;
    String text;
}
