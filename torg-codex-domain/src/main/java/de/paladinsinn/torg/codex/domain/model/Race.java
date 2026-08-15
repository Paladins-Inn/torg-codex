package de.paladinsinn.torg.codex.domain.model;

import lombok.Builder;
import lombok.Value;

import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Framework-independent domain model for a playable or non-human race.
 */
@Value
@Builder
public class Race {
    @NotNull
    UUID id;
    @NotNull
    String name;
    ClearanceLevel clearanceLevel;
    Set<String> products;
    boolean major;
    Map<String, Integer> attributeLimits;
    String abilities;
    String text;
    String perkText;
}
