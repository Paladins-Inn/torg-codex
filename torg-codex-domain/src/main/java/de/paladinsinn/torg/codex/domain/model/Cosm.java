package de.paladinsinn.torg.codex.domain.model;

import lombok.Builder;
import lombok.Value;

import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Framework-independent domain model for a cosm (reality / alternate dimension).
 */
@Value
@Builder
public class Cosm {
    @NotNull
    UUID id;
    @NotNull
    String name;
    ClearanceLevel clearanceLevel;
    Set<String> products;
    Map<String, Integer> axioms;
    String text;
    String worldLaws;
}
