package de.paladinsinn.torg.codex.domain.model;

import lombok.Builder;
import lombok.Value;

import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

/**
 * Framework-independent domain model for a perk (special ability / advantage).
 */
@Value
@Builder
public class Perk {
    @NotNull
    UUID id;
    @NotNull
    String name;
    ClearanceLevel clearanceLevel;
    Set<String> products;
    boolean contradiction;
    String cosm;
    String group;
    String prerequisites;
    String text;
}
