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

package de.paladinsinn.torg.codex.domain.markup;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum EntityType {
    COSM("cosm", "/cosms/"),
    GROUP("group", "/groups/"),
    PERK("perk", "/perks/"),
    SPELL("spell", "/spells/"),
    SPELL_LIST("spell-list", "/spell-lists/"),
    MIRACLE("miracle", "/miracles/"),
    MIRACLE_LIST("miracle-list", "/miracle-lists/"),
    POWER("power", "/powers/"),
    POWER_LIST("power-list", "/power-lists/"),
    THREAT("threat", "/threats/"),
    ITEM("item", "/items/"),
    VEHICLE("vehicle", "/vehicles/"),
    RACE("race", "/races/"),
    SHARD("shard", "/shards/");

    private static final Map<String, EntityType> BY_TAG = Stream.of(values())
            .collect(Collectors.toMap(EntityType::tag, Function.identity()));

    private final String tag;
    private final String urlSegment;

    EntityType(String tag, String urlSegment) {
        this.tag = tag;
        this.urlSegment = urlSegment;
    }

    /**
     * Returns the markup tag for this entity type.
     *
     * @return the markup tag
     */
    public String tag() {
        return tag;
    }

    /**
     * Returns the URL path segment for this entity type.
     *
     * @return the URL path segment
     */
    public String urlSegment() {
        return urlSegment;
    }

    /**
     * Finds an entity type by its markup tag.
     *
     * @param tag the markup tag
     * @return the matching entity type, if present
     */
    public static Optional<EntityType> fromTag(String tag) {
        return Optional.ofNullable(BY_TAG.get(tag));
    }

    /**
     * Converts a hyphenated entity identifier to its display name.
     *
     * @param entityId the entity identifier
     * @return the display name
     */
    public static String toDisplayName(String entityId) {
        String[] parts = entityId.split("-");
        var sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            String part = parts[i];
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1));
                }
            }
        }
        return sb.toString();
    }
}
