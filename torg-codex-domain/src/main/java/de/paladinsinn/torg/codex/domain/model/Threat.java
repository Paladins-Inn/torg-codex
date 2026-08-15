package de.paladinsinn.torg.codex.domain.model;

import lombok.Builder;
import lombok.Value;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Framework-independent domain model for a threat (NPC, creature, antagonist).
 *
 * <p>Collection/map ordering (notably {@code specialAbilities}) is preserved by
 * the mapping/persistence adapters.
 */
@Value
@Builder
public class Threat {
    @NotNull
    UUID id;
    @NotNull
    String name;
    ClearanceLevel clearanceLevel;
    Set<String> products;
    String cosm;
    boolean unique;
    String subName;
    String quote;
    String text;
    String charisma;
    String dexterity;
    String mind;
    String spirit;
    String strength;
    Map<String, String> skills;
    String moveWalk;
    String moveFly;
    String moveSwim;
    String tough;
    String shock;
    String wounds;
    List<String> equipment;
    List<String> perks;
    String possibilities;
    Map<String, String> specialAbilities;
}
