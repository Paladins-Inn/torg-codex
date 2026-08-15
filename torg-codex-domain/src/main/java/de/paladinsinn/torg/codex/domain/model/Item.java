package de.paladinsinn.torg.codex.domain.model;

import lombok.Builder;
import lombok.Value;

import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

/**
 * Framework-independent domain model for a piece of equipment, weapon, or armour.
 */
@Value
@Builder
public class Item {
    @NotNull
    UUID id;
    @NotNull
    String name;
    ClearanceLevel clearanceLevel;
    Set<String> products;
    String type;
    String cosm;
    String axiomTech;
    String axiomMagic;
    String price;
    String bonus;
    String ammo;
    String range;
    String features;
    String additionalFeatures;
    String text;
}
