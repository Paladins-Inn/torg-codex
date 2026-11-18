/*
 * Copyright (c) 2026.  Roland T. Lichti <rlichti@kaiserpfalz-edv.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * ERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * You may contact me via email rlichti@kaiserpfalz-edv.de or via mail
 *
 * Kaiserpfalz EDV-Service
 * Roland T. Lichti
 * Darmstädter Str. 12
 * 64625 Bensheim
 * GERMANY
 */

package de.paladinsinn.torg.codex.domain.model;

import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import de.paladinsinn.torg.codex.domain.catalog.CatalogSlugs;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

/**
 * Framework-independent domain model for a published Torg Eternity product.
 *
 * <p>The cover-image URL is derived exactly as in persistence, from the
 * {@code primaryProductId}.
 */
public record Publication(
    @NotNull UUID id,
    @NotNull String codexId,
    @NotNull String name,
    int primaryProductId,
    String thirdParty,
    Set<Integer> productIds
) implements CatalogEntry {

    private static final String DRIVETHRURPG_COVER_BASE = "https://www.drivethrurpg.com/images/3444/";
    private static final String DRIVETHRURPG_PRODUCT_BASE = "https://www.drivethrurpg.com/product/";

    @Override
    public CatalogArea catalogArea() {
        return CatalogArea.PUBLICATION;
    }

    @Override
    public String slug() {
        String byCodexId = CatalogSlugs.normalize(codexId);
        return byCodexId.isEmpty() ? CatalogSlugs.normalize(name) : byCodexId;
    }

    @Override
    public Set<String> productCodexIds() {
        return codexId == null || codexId.isBlank() ? Set.of() : Set.of(codexId.trim());
    }

    /**
     * Returns the DriveThruRPG cover-image URL for this publication, based on the
     * {@code primaryProductId}.
     *
     * @return the cover-image URL
     */
    public String coverURL() {
        return DRIVETHRURPG_COVER_BASE + primaryProductId + ".jpg";
    }

    /**
     * Returns the DriveThruRPG 140-pixel JPEG thumbnail URL when {@code primaryProductId > 0}.
     *
     * @return primary thumbnail URL, or {@code null} when no DriveThruRPG product id is assigned
     */
    public String thumbnailUrl() {
        return primaryProductId > 0 ? DRIVETHRURPG_COVER_BASE + primaryProductId + "-thumb140.jpg" : null;
    }

    /**
     * Returns the DriveThruRPG 140-pixel PNG fallback thumbnail URL when {@code primaryProductId > 0}.
     *
     * @return fallback thumbnail URL, or {@code null} when no DriveThruRPG product id is assigned
     */
    public String thumbnailFallbackUrl() {
        return primaryProductId > 0 ? DRIVETHRURPG_COVER_BASE + primaryProductId + "-thumb140.png" : null;
    }

    /**
     * Returns the DriveThruRPG product page URL when {@code primaryProductId > 0}.
     *
     * @return product URL, or {@code null} when no DriveThruRPG product id is assigned
     */
    public String productUrl() {
        return primaryProductId > 0 ? DRIVETHRURPG_PRODUCT_BASE + primaryProductId : null;
    }
}
