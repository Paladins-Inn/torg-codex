package de.paladinsinn.torg.codex.data.markup;

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

    public String tag() {
        return tag;
    }

    public String urlSegment() {
        return urlSegment;
    }

    public static Optional<EntityType> fromTag(String tag) {
        return Optional.ofNullable(BY_TAG.get(tag));
    }

    public static String toDisplayName(String entityId) {
        String[] parts = entityId.split("-");
        var sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(' ');
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
