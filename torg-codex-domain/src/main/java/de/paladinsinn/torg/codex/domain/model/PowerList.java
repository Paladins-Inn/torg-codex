package de.paladinsinn.torg.codex.domain.model;

import lombok.Builder;
import lombok.Value;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Framework-independent domain model for a named collection of powers.
 */
@Value
@Builder
public class PowerList {
    @NotNull
    UUID id;
    @NotNull
    String name;
    ClearanceLevel clearanceLevel;
    Set<String> products;
    String cosm;
    String unlockingPerk;
    List<UUID> powers;
    String text;
    String notes;
    String disableIf;
}
