package de.paladinsinn.torg.codex.domain.model;

import lombok.Builder;
import lombok.Value;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Framework-independent domain model for a hierarchical categorisation tag.
 */
@Value
@Builder
public class Tag {
    @NotNull
    UUID id;
    @NotNull
    String name;
    ClearanceLevel clearanceLevel;
    UUID parent;
}
