package de.paladinsinn.torg.codex.domain.model;

import lombok.Builder;
import lombok.Value;

import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

/**
 * Framework-independent domain model for an Eternity Shard.
 */
@Value
@Builder
public class Shard {
    @NotNull
    UUID id;
    @NotNull
    String name;
    ClearanceLevel clearanceLevel;
    Set<String> products;
    String cosm;
    String possibilities;
    String tappingDifficulty;
    String purpose;
    String text;
    String powers;
    String restrictions;
}
