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

package de.paladinsinn.torg.codex.domain.catalog;

import de.paladinsinn.torg.codex.domain.markup.EntityType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Unified enumeration of all 17 Torg Codex catalog areas: the 14 product-bound areas ({@link EntityType})
 * plus the 3 public reference areas ({@code PUBLICATIONS}, {@code ARTICLES}, {@code TAGS}).
 *
 * <p>{@link EntityType} stays dedicated to the 14 product-bound areas so existing DRM architecture
 * invariants (ArchUnit A3) remain untouched.
 */
public enum CatalogArea {
    RACE(new AreaPath("races", Set.of(), "race"),
            new AreaPresentation("Races", "Race", "Playable species and character heritages across the Possibility Wars.",
                    NavigationSection.STORM_KNIGHTS, "bi-people"),
            EntityType.RACE, null),
    PERK(new AreaPath("perks", Set.of(), "perk"),
            new AreaPresentation("Perks", "Perk", "Special abilities, edges, and advantages available to Storm Knights.",
                    NavigationSection.STORM_KNIGHTS, "bi-stars"),
            EntityType.PERK, null),
    PERK_GROUP(new AreaPath("groups", Set.of("perk-groups"), "group"),
            new AreaPresentation("Perk Groups", "Perk Group", "Perk categories and group rules governing perk selection.",
                    NavigationSection.STORM_KNIGHTS, "bi-collection"),
            EntityType.GROUP, GroupingDimension.PERK_GROUP),
    SPELL(new AreaPath("spells", Set.of(), "spell"),
            new AreaPresentation("Spells", "Spell", "Arcane spells with axioms, skill requirements, and casting statistics.",
                    NavigationSection.STORM_KNIGHTS, "bi-magic"),
            EntityType.SPELL, null),
    SPELL_LIST(new AreaPath("spell-lists", Set.of(), "spell-list"),
            new AreaPresentation("Spell Lists", "Spell List", "Tradition and cosm spell lists unlocking arcane magic.",
                    NavigationSection.STORM_KNIGHTS, "bi-journal-bookmark"),
            EntityType.SPELL_LIST, GroupingDimension.SPELL_LIST),
    MIRACLE(new AreaPath("miracles", Set.of(), "miracle"),
            new AreaPresentation("Miracles", "Miracle", "Faith-driven miracles invoked across realms.",
                    NavigationSection.STORM_KNIGHTS, "bi-sun"),
            EntityType.MIRACLE, null),
    MIRACLE_LIST(new AreaPath("miracle-lists", Set.of(), "miracle-list"),
            new AreaPresentation("Miracle Lists", "Miracle List", "Faith traditions and miracle collections.",
                    NavigationSection.STORM_KNIGHTS, "bi-journal-plus"),
            EntityType.MIRACLE_LIST, GroupingDimension.MIRACLE_LIST),
    POWER(new AreaPath("powers", Set.of(), "power"),
            new AreaPresentation("Psionics", "Power", "Psionic and pulp powers with enhancements and limitations.",
                    NavigationSection.STORM_KNIGHTS, "bi-lightning-charge"),
            EntityType.POWER, null),
    POWER_LIST(new AreaPath("power-lists", Set.of(), "power-list"),
            new AreaPresentation("Power Lists", "Power List", "Psionic disciplines and pulp power lists.",
                    NavigationSection.STORM_KNIGHTS, "bi-journal-code"),
            EntityType.POWER_LIST, GroupingDimension.POWER_LIST),
    ITEM(new AreaPath("items", Set.of(), "item"),
            new AreaPresentation("Items & Gear", "Item", "General gear, armor, melee and ranged weapons, cyberware, and occultech.",
                    NavigationSection.GEAR_AND_VEHICLES, "bi-shield-shaded"),
            EntityType.ITEM, null),
    VEHICLE(new AreaPath("vehicles", Set.of(), "vehicle"),
            new AreaPresentation("Vehicles", "Vehicle", "Ground, air, and water vehicles with speed, toughness, and weaponry.",
                    NavigationSection.GEAR_AND_VEHICLES, "bi-truck-front"),
            EntityType.VEHICLE, null),
    COSM(new AreaPath("cosms", Set.of(), "cosm"),
            new AreaPresentation("Cosms", "Cosm", "Invading realities, axioms, and world laws.",
                    NavigationSection.GAME_MASTER, "bi-globe2"),
            EntityType.COSM, null),
    SHARD(new AreaPath("shards", Set.of(), "shard"),
            new AreaPresentation("Eternity Shards", "Eternity Shard", "Powerful artifacts of Possibility energy.",
                    NavigationSection.GAME_MASTER, "bi-gem"),
            EntityType.SHARD, null),
    THREAT(new AreaPath("threats", Set.of(), "threat"),
            new AreaPresentation("Threats", "Threat", "Adversaries, creatures, and NPCs with full game statistics.",
                    NavigationSection.GAME_MASTER, "bi-exclamation-octagon"),
            EntityType.THREAT, null),
    PUBLICATION(new AreaPath("publications", Set.of(), "publication"),
            new AreaPresentation("Publications", "Publication", "Official and Infiniverse sourcebooks and supplements.",
                    NavigationSection.REFERENCE, "bi-book"),
            null, null),
    ARTICLE(new AreaPath("articles", Set.of(), "article"),
            new AreaPresentation("Articles", "Article", "Rules clarifications, changelog, and reference articles.",
                    NavigationSection.REFERENCE, "bi-file-earmark-text"),
            null, null),
    TAG(new AreaPath("tags", Set.of(), "tag"),
            new AreaPresentation("Tags", "Tag", "Hierarchical categorization tags across codex entries.",
                    NavigationSection.REFERENCE, "bi-tags"),
            null, GroupingDimension.TAG);

    private record AreaPath(String slug, Set<String> aliases, String singularTag) {
    }

    private record AreaPresentation(
            String displayName,
            String singularName,
            String description,
            NavigationSection section,
            String icon) {
    }

    /** Path prefixes explicitly reserved for non-catalog routes (FR-019, web-ui-routes.md). */
    public static final Set<String> RESERVED_PATH_PREFIXES = Set.of(
            "admin", "api", "login", "logout", "webjars", "themes", "actuator", "static", "error");

    private static final Map<String, CatalogArea> BY_PATH;
    private static final Map<EntityType, CatalogArea> BY_ENTITY_TYPE;

    static {
        Map<String, CatalogArea> byPath = new HashMap<>();
        Map<EntityType, CatalogArea> byEntity = new EnumMap<>(EntityType.class);
        for (CatalogArea area : values()) {
            byPath.put(area.slug, area);
            for (String alias : area.aliases) {
                byPath.put(alias, area);
            }
            if (area.entityType != null) {
                byEntity.put(area.entityType, area);
            }
        }
        BY_PATH = Map.copyOf(byPath);
        BY_ENTITY_TYPE = Map.copyOf(byEntity);
    }

    private final String slug;
    private final Set<String> aliases;
    private final String singularTag;
    private final String displayName;
    private final String singularName;
    private final String description;
    private final NavigationSection section;
    private final String icon;
    private final EntityType entityType;
    private final GroupingDimension selfGrouping;

    CatalogArea(
            final AreaPath path,
            final AreaPresentation presentation,
            final EntityType entityType,
            final GroupingDimension selfGrouping) {
        this.slug = path.slug();
        this.aliases = Set.copyOf(path.aliases());
        this.singularTag = path.singularTag();
        this.displayName = presentation.displayName();
        this.singularName = presentation.singularName();
        this.description = presentation.description();
        this.section = presentation.section();
        this.icon = presentation.icon();
        this.entityType = entityType;
        this.selfGrouping = selfGrouping;
    }

    public String slug() {
        return slug;
    }

    public Set<String> aliases() {
        return aliases;
    }

    public String singularTag() {
        return singularTag;
    }

    public String displayName() {
        return displayName;
    }

    public String singularName() {
        return singularName;
    }

    public String description() {
        return description;
    }

    public NavigationSection section() {
        return section;
    }

    public String icon() {
        return icon;
    }

    public boolean isProductBound() {
        return entityType != null;
    }

    public Optional<EntityType> productBoundType() {
        return Optional.ofNullable(entityType);
    }

    public Optional<GroupingDimension> selfGroupingDimension() {
        return Optional.ofNullable(selfGrouping);
    }

    public String canonicalPath() {
        return "/" + slug;
    }

    public String detailPath(final String entrySlug) {
        return "/" + slug + "/" + entrySlug;
    }

    /**
     * Resolves a URL path segment (canonical or alias) to its {@link CatalogArea}.
     * Reserved prefixes such as {@code admin} and {@code api} always return {@link Optional#empty()}.
     *
     * @param segment the single URL path segment
     * @return the matching catalog area, if any
     */
    public static Optional<CatalogArea> fromPathSegment(final String segment) {
        if (segment == null || segment.isBlank()) {
            return Optional.empty();
        }
        String normalized = segment.trim().toLowerCase(Locale.ROOT);
        if (RESERVED_PATH_PREFIXES.contains(normalized)) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_PATH.get(normalized));
    }

    /**
     * Returns {@code true} when {@code segment} is a non-canonical alias path (e.g. {@code perk-groups})
     * that should redirect to {@link #canonicalPath()}.
     *
     * @param segment the URL path segment
     * @return whether the segment is an alias
     */
    public boolean isAliasPath(final String segment) {
        return segment != null && aliases.contains(segment.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Maps a product-bound {@link EntityType} to its {@link CatalogArea}.
     *
     * @param type the product-bound entity type
     * @return the corresponding catalog area
     */
    public static CatalogArea fromEntityType(final EntityType type) {
        CatalogArea area = BY_ENTITY_TYPE.get(type);
        if (area == null) {
            throw new IllegalArgumentException("Unmapped EntityType: " + type);
        }
        return area;
    }

    /**
     * Returns all catalog areas belonging to the given navigation section in display order.
     *
     * @param section the navigation section
     * @return ordered list of areas
     */
    public static List<CatalogArea> bySection(final NavigationSection section) {
        return Stream.of(values())
                .filter(area -> area.section == section)
                .toList();
    }
}
