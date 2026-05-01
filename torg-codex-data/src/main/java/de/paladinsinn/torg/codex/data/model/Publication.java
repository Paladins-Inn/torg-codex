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

package de.paladinsinn.torg.codex.data.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A published Torg Eternity product (book, deck, supplement, etc.) available on DriveThruRPG.
 *
 * <p>Each publication is identified by a stable {@link #codexId} (e.g. {@code "core-rulebook"})
 * and may be sold under several DriveThru product IDs (English original + localised editions).
 * The {@link #primaryProductId} is the canonical English product ID used for cover images.
 */
@Entity
@Table(name = "torg_publication")
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class Publication {

    private static final String DRIVETHRURPG_COVER_BASE = "https://www.drivethrurpg.com/images/3444/";

    /** Primary key – stable UUID generated from the codex id. */
    @Id
    private UUID id;

    /**
     * Stable human-readable identifier for this publication within the codex
     * (e.g. {@code "core-rulebook"}, {@code "sourcebook-aysle"}).
     */
    @Column(name = "codex_id", length = 128, nullable = false, unique = true)
    private String codexId;

    /** Full title of this publication. */
    @Column(nullable = false)
    private String name;

    /**
     * The canonical (English) DriveThruRPG product ID used for cover-image URLs.
     */
    @Column(name = "primary_product_id", nullable = false)
    private int primaryProductId;

    /**
     * Name of the third-party author/publisher, or {@code null} for official Ulisses / Pinnacle
     * products.
     */
    @Column(name = "third_party")
    private String thirdParty;

    /**
     * All known DriveThruRPG product IDs for this publication (English + localised editions).
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "torg_publication_product_ids",
            joinColumns = @JoinColumn(name = "publication_id"))
    @Column(name = "product_id")
    private Set<Integer> productIds = new HashSet<>();

    /**
     * Returns the DriveThruRPG cover-image URL for this publication, based on the
     * {@link #primaryProductId}.
     *
     * @return full cover URL, e.g.
     *         {@code https://www.drivethrurpg.com/images/3444/216248.jpg}
     */
    public String getCoverURL() {
        return DRIVETHRURPG_COVER_BASE + primaryProductId + ".jpg";
    }
}

