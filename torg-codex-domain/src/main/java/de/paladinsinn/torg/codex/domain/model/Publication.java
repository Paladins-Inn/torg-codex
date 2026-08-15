package de.paladinsinn.torg.codex.domain.model;

import lombok.Builder;
import lombok.Value;

import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

/**
 * Framework-independent domain model for a published Torg Eternity product.
 *
 * <p>The cover-image URL is derived exactly as in persistence, from the
 * {@code primaryProductId}.
 */
@Value
@Builder
public class Publication {

    private static final String DRIVETHRURPG_COVER_BASE = "https://www.drivethrurpg.com/images/3444/";

    @NotNull
    UUID id;
    @NotNull
    String codexId;
    @NotNull
    String name;
    int primaryProductId;
    String thirdParty;
    Set<Integer> productIds;

    /**
     * Returns the DriveThruRPG cover-image URL for this publication, based on the
     * {@code primaryProductId}.
     */
    public String getCoverURL() {
        return DRIVETHRURPG_COVER_BASE + primaryProductId + ".jpg";
    }
}
