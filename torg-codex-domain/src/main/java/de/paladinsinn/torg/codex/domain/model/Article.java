package de.paladinsinn.torg.codex.domain.model;

import lombok.Builder;
import lombok.Value;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Framework-independent domain model for a standalone article
 * (credits, changelog, rules clarifications).
 *
 * <p>Carries the raw, un-rendered {@code text}; product-gate censorship/markup
 * rendering remains an adapter concern.
 */
@Value
@Builder
public class Article {
    @NotNull
    UUID id;
    @NotNull
    String name;
    ClearanceLevel clearanceLevel;
    String text;
}
