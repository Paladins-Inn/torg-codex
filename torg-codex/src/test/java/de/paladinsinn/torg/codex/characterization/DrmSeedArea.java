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

package de.paladinsinn.torg.codex.characterization;

import java.util.UUID;

/**
 * The 14 product-bound catalog areas as seeded by {@code sql/catalog-drm-seed.sql} (feature 005).
 *
 * <p>Every area has four entries: {@link Variant#CORE} ({@code core-rulebook}),
 * {@link Variant#AYSLE} ({@code sourcebook-aysle} and {@code drm-third-product}),
 * {@link Variant#THIRD} ({@code drm-third-product} only) and {@link Variant#UNASSIGNED} (no
 * product). Cosm-filterable entries belong to the cosm {@value #COSM}.
 */
public enum DrmSeedArea {
    COSMS(0x1, "cosms", "cosm", false),
    ITEMS(0x2, "items", "item", true),
    MIRACLES(0x3, "miracles", "miracle", false),
    MIRACLE_LISTS(0x4, "miracle-lists", "miracle-list", true),
    PERKS(0x5, "perks", "perk", true),
    PERK_GROUPS(0x6, "perk-groups", "group", false),
    POWERS(0x7, "powers", "power", false),
    POWER_LISTS(0x8, "power-lists", "power-list", true),
    RACES(0x9, "races", "race", false),
    SHARDS(0xa, "shards", "shard", true),
    SPELLS(0xb, "spells", "spell", false),
    SPELL_LISTS(0xc, "spell-lists", "spell-list", true),
    THREATS(0xd, "threats", "threat", true),
    VEHICLES(0xe, "vehicles", "vehicle", true);

    /** The cosm slug of every cosm-filterable seeded entry. */
    public static final String COSM = "drm-cosm";

    /** The seeded public article with a core-gated passage. */
    public static final UUID ARTICLE = UUID.fromString("d7a00000-000f-4000-8000-000000000001");

    /** The core item referencing the product-less cosm {@code drm-cosm-unassigned}. */
    public static final UUID ITEM_WITH_UNASSIGNED_COSM = UUID.fromString("d7a00000-0002-4000-8000-000000000005");

    /** The publication of {@code sourcebook-aysle} in the public free-tier dataset. */
    public static final UUID AYSLE_PUBLICATION = UUID.fromString("85d9fb28-22e7-5313-adb5-d0e2ab36e3fb");

    /** Text only rendered for owners of the free tier. */
    public static final String CORE_PASSAGE = "DRM-CORE-PASSAGE";

    /** Text only rendered for callers not owning the free tier. */
    public static final String UPSELL_PASSAGE = "DRM-UPSELL-PASSAGE";

    /** Text only rendered for owners of {@code sourcebook-aysle}. */
    public static final String AYSLE_OWNER_PASSAGE = "DRM-AYSLE-OWNER-ONLY";

    /** Text only rendered for callers not owning {@code sourcebook-aysle}. */
    public static final String AYSLE_TEASER_PASSAGE = "DRM-AYSLE-TEASER";

    private final int index;
    private final String collection;
    private final String tag;
    private final boolean cosmFilter;

    DrmSeedArea(int index, String collection, String tag, boolean cosmFilter) {
        this.index = index;
        this.collection = collection;
        this.tag = tag;
        this.cosmFilter = cosmFilter;
    }

    /** The seeded entry variants. */
    public enum Variant {
        CORE(1, "Core"),
        AYSLE(2, "Aysle"),
        THIRD(3, "Third"),
        UNASSIGNED(4, "Unassigned");

        private final int number;
        private final String label;

        Variant(int number, String label) {
            this.number = number;
            this.label = label;
        }
    }

    public String path() {
        return "/api/" + collection;
    }

    public String collection() {
        return collection;
    }

    public String tag() {
        return tag;
    }

    public boolean cosmFilter() {
        return cosmFilter;
    }

    public UUID id(Variant variant) {
        return UUID.fromString("d7a00000-%04x-4000-8000-%012x".formatted(index, variant.number));
    }

    public String name(Variant variant) {
        return "DRM-" + tag + "-" + variant.label;
    }

    public String detailPath(Variant variant) {
        return path() + "/" + id(variant);
    }
}
