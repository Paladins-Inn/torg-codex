package de.paladinsinn.torg.codex.domain.model;

import lombok.Builder;
import lombok.Value;

import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Framework-independent domain model for a psionic or pulp-power ability.
 */
@Value
@Builder
public class Power {
    @NotNull
    UUID id;
    @NotNull
    String name;
    ClearanceLevel clearanceLevel;
    Set<String> products;
    String axiom;
    Map<String, Integer> requiredSkills;
    String castingTime;
    DifficultyNumber dn;
    String range;
    String duration;
    String text;
    String enhancements;
    String limitations;
}
