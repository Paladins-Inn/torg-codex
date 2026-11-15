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

import de.paladinsinn.torg.codex.domain.catalog.DetailSectionDescriptor.AttributeField;
import de.paladinsinn.torg.codex.domain.catalog.DetailSectionDescriptor.Attributes;
import de.paladinsinn.torg.codex.domain.catalog.DetailSectionDescriptor.EmbeddedReferences;
import de.paladinsinn.torg.codex.domain.catalog.DetailSectionDescriptor.Prose;
import de.paladinsinn.torg.codex.domain.catalog.DetailSectionDescriptor.ProseMap;
import de.paladinsinn.torg.codex.domain.markup.EntityType;
import de.paladinsinn.torg.codex.domain.model.Article;
import de.paladinsinn.torg.codex.domain.model.CatalogEntry;
import de.paladinsinn.torg.codex.domain.model.Cosm;
import de.paladinsinn.torg.codex.domain.model.DifficultyNumber;
import de.paladinsinn.torg.codex.domain.model.Item;
import de.paladinsinn.torg.codex.domain.model.Miracle;
import de.paladinsinn.torg.codex.domain.model.MiracleList;
import de.paladinsinn.torg.codex.domain.model.Perk;
import de.paladinsinn.torg.codex.domain.model.PerkGroup;
import de.paladinsinn.torg.codex.domain.model.Power;
import de.paladinsinn.torg.codex.domain.model.PowerList;
import de.paladinsinn.torg.codex.domain.model.Publication;
import de.paladinsinn.torg.codex.domain.model.Race;
import de.paladinsinn.torg.codex.domain.model.Shard;
import de.paladinsinn.torg.codex.domain.model.Spell;
import de.paladinsinn.torg.codex.domain.model.SpellList;
import de.paladinsinn.torg.codex.domain.model.Tag;
import de.paladinsinn.torg.codex.domain.model.Threat;
import de.paladinsinn.torg.codex.domain.model.Vehicle;
import de.paladinsinn.torg.codex.domain.model.VehicleWeapon;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable registry of {@link CatalogCategoryDescriptor} instances for all 17 {@link CatalogArea}s.
 */
public final class CatalogCategoryRegistry {

    private static final Map<CatalogArea, CatalogCategoryDescriptor<? extends CatalogEntry>> DESCRIPTORS;

    static {
        Map<CatalogArea, CatalogCategoryDescriptor<? extends CatalogEntry>> map = new EnumMap<>(CatalogArea.class);
        register(map, cosmDescriptor());
        register(map, raceDescriptor());
        register(map, perkDescriptor());
        register(map, perkGroupDescriptor());
        register(map, spellDescriptor());
        register(map, spellListDescriptor());
        register(map, miracleDescriptor());
        register(map, miracleListDescriptor());
        register(map, powerDescriptor());
        register(map, powerListDescriptor());
        register(map, itemDescriptor());
        register(map, vehicleDescriptor());
        register(map, shardDescriptor());
        register(map, threatDescriptor());
        register(map, publicationDescriptor());
        register(map, articleDescriptor());
        register(map, tagDescriptor());
        DESCRIPTORS = Map.copyOf(map);
    }

    private CatalogCategoryRegistry() {
    }

    public static CatalogCategoryDescriptor<? extends CatalogEntry> forArea(final CatalogArea area) {
        CatalogCategoryDescriptor<? extends CatalogEntry> descriptor = DESCRIPTORS.get(area);
        if (descriptor == null) {
            throw new IllegalArgumentException("Missing descriptor for area: " + area);
        }
        return descriptor;
    }

    @SuppressWarnings("unchecked")
    public static <E extends CatalogEntry> CatalogCategoryDescriptor<E> forArea(
            final CatalogArea area, final Class<E> entryClass) {
        CatalogCategoryDescriptor<?> descriptor = forArea(area);
        if (!descriptor.entryClass().equals(entryClass)) {
            throw new IllegalArgumentException("Area " + area + " expects " + descriptor.entryClass() + ", got " + entryClass);
        }
        return (CatalogCategoryDescriptor<E>) descriptor;
    }

    public static Collection<CatalogCategoryDescriptor<? extends CatalogEntry>> all() {
        return DESCRIPTORS.values();
    }

    private static <E extends CatalogEntry> void register(
            final Map<CatalogArea, CatalogCategoryDescriptor<? extends CatalogEntry>> target,
            final CatalogCategoryDescriptor<E> descriptor) {
        target.put(descriptor.area(), descriptor);
    }

    private static CatalogCategoryDescriptor<Cosm> cosmDescriptor() {
        return new CatalogCategoryDescriptor<>(
                CatalogArea.COSM,
                Cosm.class,
                List.of(
                        ColumnDescriptor.primaryName(),
                        ColumnDescriptor.text("slug", "Slug", true, Cosm::slug),
                        ColumnDescriptor.clearance(),
                        ColumnDescriptor.text("axioms", "Axioms", false, c -> formatIntMap(c.axioms()))),
                List.of(FilterFacetDescriptor.grouping(GroupingDimension.CLEARANCE)),
                List.of(
                        new Attributes<>("Axioms", List.of(
                                AttributeField.plain("Slug", Cosm::slug),
                                AttributeField.plain("Axioms", c -> formatIntMap(c.axioms())))),
                        new Prose<>("Description", Cosm::text),
                        new Prose<>("World Laws", Cosm::worldLaws)),
                null);
    }

    private static CatalogCategoryDescriptor<Race> raceDescriptor() {
        return new CatalogCategoryDescriptor<>(
                CatalogArea.RACE,
                Race.class,
                List.of(
                        ColumnDescriptor.primaryName(),
                        ColumnDescriptor.clearance(),
                        ColumnDescriptor.flag("major", "Major Race", Race::major)),
                List.of(FilterFacetDescriptor.grouping(GroupingDimension.CLEARANCE)),
                List.of(
                        new Attributes<>("Characteristics", List.of(
                                AttributeField.plain("Category", r -> r.major() ? "Major Race" : "Minor Race"),
                                AttributeField.plain("Attribute Limits", r -> formatIntMap(r.attributeLimits())),
                                AttributeField.markup("Abilities", Race::abilities))),
                        new Prose<>("Description", Race::text),
                        new Prose<>("Racial Perks", Race::perkText)),
                null);
    }

    private static CatalogCategoryDescriptor<Perk> perkDescriptor() {
        return new CatalogCategoryDescriptor<>(
                CatalogArea.PERK,
                Perk.class,
                List.of(
                        ColumnDescriptor.primaryName(),
                        ColumnDescriptor.clearance(),
                        ColumnDescriptor.groupBadge("group", "Group", true, p -> displaySlug(p.group())),
                        ColumnDescriptor.cosms(),
                        ColumnDescriptor.flag("contradiction", "Contradiction", Perk::contradiction),
                        ColumnDescriptor.markup("prerequisites", "Prerequisites", Perk::prerequisites)),
                List.of(
                        FilterFacetDescriptor.grouping(GroupingDimension.CLEARANCE),
                        FilterFacetDescriptor.cosm(),
                        FilterFacetDescriptor.grouping(GroupingDimension.PERK_GROUP),
                        FilterFacetDescriptor.grouping(GroupingDimension.RACE)),
                List.of(
                        new Attributes<>("Perk Details", List.of(
                                AttributeField.plain("Group", p -> displaySlug(p.group())),
                                AttributeField.plain("Contradiction", p -> p.contradiction() ? "Yes" : "No"),
                                AttributeField.markup("Prerequisites", Perk::prerequisites))),
                        new Prose<>("", Perk::text)),
                (perk, dim) -> dim == GroupingDimension.PERK_GROUP
                        ? singleSlug(perk.group())
                        : Set.of());
    }

    private static CatalogCategoryDescriptor<PerkGroup> perkGroupDescriptor() {
        return new CatalogCategoryDescriptor<>(
                CatalogArea.PERK_GROUP,
                PerkGroup.class,
                List.of(
                        ColumnDescriptor.primaryName(),
                        ColumnDescriptor.clearance()),
                List.of(FilterFacetDescriptor.grouping(GroupingDimension.CLEARANCE)),
                List.of(
                        new Prose<>("", PerkGroup::text),
                        new Prose<>("Additional Information", PerkGroup::infos)),
                null);
    }

    private static CatalogCategoryDescriptor<Spell> spellDescriptor() {
        return new CatalogCategoryDescriptor<>(
                CatalogArea.SPELL,
                Spell.class,
                List.of(
                        ColumnDescriptor.primaryName(),
                        ColumnDescriptor.clearance(),
                        ColumnDescriptor.text("axiom", "Axiom", true, Spell::axiom),
                        ColumnDescriptor.text("dn", "DN", true, s -> formatDn(s.dn())),
                        ColumnDescriptor.text("castingTime", "Casting Time", false, Spell::castingTime),
                        ColumnDescriptor.text("range", "Range", false, Spell::range),
                        ColumnDescriptor.text("duration", "Duration", false, Spell::duration)),
                List.of(
                        FilterFacetDescriptor.grouping(GroupingDimension.CLEARANCE),
                        FilterFacetDescriptor.grouping(GroupingDimension.SPELL_LIST)),
                List.of(
                        new Attributes<>("Casting Statistics", List.of(
                                AttributeField.plain("Magic Axiom", Spell::axiom),
                                AttributeField.plain("Required Skills", s -> formatIntMap(s.requiredSkills())),
                                AttributeField.plain("Casting Time", Spell::castingTime),
                                AttributeField.plain("Difficulty Number", s -> formatDn(s.dn())),
                                AttributeField.plain("Range", Spell::range),
                                AttributeField.plain("Duration", Spell::duration))),
                        new Prose<>("", Spell::text)),
                null);
    }

    private static CatalogCategoryDescriptor<SpellList> spellListDescriptor() {
        return new CatalogCategoryDescriptor<>(
                CatalogArea.SPELL_LIST,
                SpellList.class,
                List.of(
                        ColumnDescriptor.primaryName(),
                        ColumnDescriptor.clearance(),
                        ColumnDescriptor.cosms(),
                        ColumnDescriptor.text("unlockingPerks", "Unlocking Perks", false, l -> formatSlugs(l.unlockingPerks())),
                        ColumnDescriptor.text("count", "Spells", true, l -> String.valueOf(l.referencedEntries().size()))),
                List.of(
                        FilterFacetDescriptor.grouping(GroupingDimension.CLEARANCE),
                        FilterFacetDescriptor.cosm()),
                List.of(
                        new Attributes<>("List Properties", List.of(
                                AttributeField.plain("Unlocking Perks", l -> formatSlugs(l.unlockingPerks())))),
                        new Prose<>("", SpellList::text),
                        new Prose<>("Notes", SpellList::notes),
                        new EmbeddedReferences<>("Spells", CatalogArea.SPELL, SpellList::referencedEntries)),
                null);
    }

    private static CatalogCategoryDescriptor<Miracle> miracleDescriptor() {
        return new CatalogCategoryDescriptor<>(
                CatalogArea.MIRACLE,
                Miracle.class,
                List.of(
                        ColumnDescriptor.primaryName(),
                        ColumnDescriptor.clearance(),
                        ColumnDescriptor.text("axiom", "Axiom", true, Miracle::axiom),
                        ColumnDescriptor.text("dn", "DN", true, m -> formatDn(m.dn())),
                        ColumnDescriptor.text("castingTime", "Casting Time", false, Miracle::castingTime),
                        ColumnDescriptor.text("range", "Range", false, Miracle::range),
                        ColumnDescriptor.text("duration", "Duration", false, Miracle::duration)),
                List.of(
                        FilterFacetDescriptor.grouping(GroupingDimension.CLEARANCE),
                        FilterFacetDescriptor.grouping(GroupingDimension.MIRACLE_LIST)),
                List.of(
                        new Attributes<>("Invocation Statistics", List.of(
                                AttributeField.plain("Spirit Axiom", Miracle::axiom),
                                AttributeField.plain("Required Skills", m -> formatIntMap(m.requiredSkills())),
                                AttributeField.plain("Casting Time", Miracle::castingTime),
                                AttributeField.plain("Difficulty Number", m -> formatDn(m.dn())),
                                AttributeField.plain("Range", Miracle::range),
                                AttributeField.plain("Duration", Miracle::duration))),
                        new Prose<>("", Miracle::text)),
                null);
    }

    private static CatalogCategoryDescriptor<MiracleList> miracleListDescriptor() {
        return new CatalogCategoryDescriptor<>(
                CatalogArea.MIRACLE_LIST,
                MiracleList.class,
                List.of(
                        ColumnDescriptor.primaryName(),
                        ColumnDescriptor.clearance(),
                        ColumnDescriptor.cosms(),
                        ColumnDescriptor.text("unlockingPerks", "Unlocking Perks", false, l -> formatSlugs(l.unlockingPerks())),
                        ColumnDescriptor.text("count", "Miracles", true, l -> String.valueOf(l.referencedEntries().size()))),
                List.of(
                        FilterFacetDescriptor.grouping(GroupingDimension.CLEARANCE),
                        FilterFacetDescriptor.cosm()),
                List.of(
                        new Attributes<>("List Properties", List.of(
                                AttributeField.plain("Unlocking Perks", l -> formatSlugs(l.unlockingPerks())))),
                        new Prose<>("", MiracleList::text),
                        new Prose<>("Notes", MiracleList::notes),
                        new EmbeddedReferences<>("Miracles", CatalogArea.MIRACLE, MiracleList::referencedEntries)),
                null);
    }

    private static CatalogCategoryDescriptor<Power> powerDescriptor() {
        return new CatalogCategoryDescriptor<>(
                CatalogArea.POWER,
                Power.class,
                List.of(
                        ColumnDescriptor.primaryName(),
                        ColumnDescriptor.clearance(),
                        ColumnDescriptor.text("axiom", "Axiom", true, Power::axiom),
                        ColumnDescriptor.text("dn", "DN", true, p -> formatDn(p.dn())),
                        ColumnDescriptor.text("castingTime", "Activation", false, Power::castingTime),
                        ColumnDescriptor.text("range", "Range", false, Power::range),
                        ColumnDescriptor.text("duration", "Duration", false, Power::duration)),
                List.of(
                        FilterFacetDescriptor.grouping(GroupingDimension.CLEARANCE),
                        FilterFacetDescriptor.grouping(GroupingDimension.POWER_LIST)),
                List.of(
                        new Attributes<>("Power Statistics", List.of(
                                AttributeField.plain("Axiom", Power::axiom),
                                AttributeField.plain("Required Skills", p -> formatIntMap(p.requiredSkills())),
                                AttributeField.plain("Activation Time", Power::castingTime),
                                AttributeField.plain("Difficulty Number", p -> formatDn(p.dn())),
                                AttributeField.plain("Range", Power::range),
                                AttributeField.plain("Duration", Power::duration))),
                        new Prose<>("", Power::text),
                        new Prose<>("Enhancements", Power::enhancements),
                        new Prose<>("Limitations", Power::limitations)),
                null);
    }

    private static CatalogCategoryDescriptor<PowerList> powerListDescriptor() {
        return new CatalogCategoryDescriptor<>(
                CatalogArea.POWER_LIST,
                PowerList.class,
                List.of(
                        ColumnDescriptor.primaryName(),
                        ColumnDescriptor.clearance(),
                        ColumnDescriptor.cosms(),
                        ColumnDescriptor.text("unlockingPerks", "Unlocking Perks", false, l -> formatSlugs(l.unlockingPerks())),
                        ColumnDescriptor.text("count", "Powers", true, l -> String.valueOf(l.referencedEntries().size()))),
                List.of(
                        FilterFacetDescriptor.grouping(GroupingDimension.CLEARANCE),
                        FilterFacetDescriptor.cosm()),
                List.of(
                        new Attributes<>("List Properties", List.of(
                                AttributeField.plain("Unlocking Perks", l -> formatSlugs(l.unlockingPerks())))),
                        new Prose<>("", PowerList::text),
                        new Prose<>("Notes", PowerList::notes),
                        new EmbeddedReferences<>("Powers", CatalogArea.POWER, PowerList::referencedEntries)),
                null);
    }

    private static CatalogCategoryDescriptor<Item> itemDescriptor() {
        return new CatalogCategoryDescriptor<>(
                CatalogArea.ITEM,
                Item.class,
                List.of(
                        ColumnDescriptor.primaryName(),
                        ColumnDescriptor.groupBadge("type", "Type", true, i -> displaySlug(i.type())),
                        ColumnDescriptor.cosms(),
                        ColumnDescriptor.clearance(),
                        ColumnDescriptor.text("axiom", "Axiom", true, CatalogCategoryRegistry::formatItemAxioms),
                        ColumnDescriptor.text("price", "Price", true, Item::price),
                        ColumnDescriptor.text("bonus", "Bonus / Damage", false, Item::bonus)),
                List.of(
                        FilterFacetDescriptor.grouping(GroupingDimension.ITEM_TYPE),
                        FilterFacetDescriptor.cosm(),
                        FilterFacetDescriptor.grouping(GroupingDimension.CLEARANCE)),
                List.of(
                        new Attributes<>("Equipment Statistics", List.of(
                                AttributeField.plain("Type", i -> displaySlug(i.type())),
                                AttributeField.plain("Tech Axiom", Item::axiomTech),
                                AttributeField.plain("Magic Axiom", Item::axiomMagic),
                                AttributeField.plain("Price", Item::price),
                                AttributeField.plain("Damage / Bonus", Item::bonus),
                                AttributeField.plain("Ammo", Item::ammo),
                                AttributeField.plain("Range", Item::range),
                                AttributeField.markup("Features", Item::features))),
                        new Prose<>("Additional Features", Item::additionalFeatures),
                        new Prose<>("", Item::text)),
                (item, dim) -> dim == GroupingDimension.ITEM_TYPE
                        ? singleSlug(item.type())
                        : Set.of());
    }

    private static CatalogCategoryDescriptor<Vehicle> vehicleDescriptor() {
        return new CatalogCategoryDescriptor<>(
                CatalogArea.VEHICLE,
                Vehicle.class,
                List.of(
                        ColumnDescriptor.primaryName(),
                        ColumnDescriptor.groupBadge("type", "Type", true, v -> displaySlug(v.type())),
                        ColumnDescriptor.cosms(),
                        ColumnDescriptor.clearance(),
                        ColumnDescriptor.text("axiomTech", "Tech Axiom", true, Vehicle::axiomTech),
                        ColumnDescriptor.text("speed", "Top Speed", false, Vehicle::speed),
                        ColumnDescriptor.text("tough", "Toughness", true, Vehicle::tough),
                        ColumnDescriptor.text("price", "Price", true, Vehicle::price)),
                List.of(
                        FilterFacetDescriptor.grouping(GroupingDimension.VEHICLE_TYPE),
                        FilterFacetDescriptor.cosm(),
                        FilterFacetDescriptor.grouping(GroupingDimension.CLEARANCE)),
                List.of(
                        new Attributes<>("Vehicle Statistics", List.of(
                                AttributeField.plain("Type", v -> displaySlug(v.type())),
                                AttributeField.plain("Unique", v -> v.unique() ? "Yes" : ""),
                                AttributeField.plain("Tech Axiom", Vehicle::axiomTech),
                                AttributeField.plain("Top Speed", Vehicle::speed),
                                AttributeField.plain("Speed Value / Mod", v -> joinNonBlank(" / ", v.speedValue(), v.speedMod())),
                                AttributeField.plain("Size", Vehicle::size),
                                AttributeField.plain("Passengers", Vehicle::passengers),
                                AttributeField.plain("Maneuver Rating", Vehicle::maneuverRating),
                                AttributeField.plain("Toughness", Vehicle::tough),
                                AttributeField.plain("Wounds", Vehicle::wounds),
                                AttributeField.plain("Price", Vehicle::price))),
                        new Prose<>("", Vehicle::text),
                        new EmbeddedReferences<>("Mounted Weaponry", CatalogArea.ITEM, CatalogCategoryRegistry::extractVehicleWeaponIds)),
                (vehicle, dim) -> dim == GroupingDimension.VEHICLE_TYPE
                        ? singleSlug(vehicle.type())
                        : Set.of());
    }

    private static CatalogCategoryDescriptor<Shard> shardDescriptor() {
        return new CatalogCategoryDescriptor<>(
                CatalogArea.SHARD,
                Shard.class,
                List.of(
                        ColumnDescriptor.primaryName(),
                        ColumnDescriptor.cosms(),
                        ColumnDescriptor.clearance(),
                        ColumnDescriptor.text("possibilities", "Possibilities", false, Shard::possibilities),
                        ColumnDescriptor.text("tappingDifficulty", "Tapping DN", false, Shard::tappingDifficulty)),
                List.of(
                        FilterFacetDescriptor.cosm(),
                        FilterFacetDescriptor.grouping(GroupingDimension.CLEARANCE)),
                List.of(
                        new Attributes<>("Shard Attributes", List.of(
                                AttributeField.plain("Possibilities", Shard::possibilities),
                                AttributeField.plain("Tapping Difficulty", Shard::tappingDifficulty),
                                AttributeField.markup("Purpose", Shard::purpose))),
                        new Prose<>("", Shard::text),
                        new Prose<>("Powers", Shard::powers),
                        new Prose<>("Restrictions", Shard::restrictions)),
                null);
    }

    private static CatalogCategoryDescriptor<Threat> threatDescriptor() {
        return new CatalogCategoryDescriptor<>(
                CatalogArea.THREAT,
                Threat.class,
                List.of(
                        ColumnDescriptor.primaryName(),
                        ColumnDescriptor.cosms(),
                        ColumnDescriptor.clearance(),
                        ColumnDescriptor.flag("unique", "Unique", Threat::unique),
                        ColumnDescriptor.text("tough", "Toughness", true, Threat::tough),
                        ColumnDescriptor.text("wounds", "Wounds", true, Threat::wounds)),
                List.of(
                        FilterFacetDescriptor.cosm(),
                        FilterFacetDescriptor.grouping(GroupingDimension.CLEARANCE)),
                List.of(
                        new Attributes<>("Attributes & Defenses", List.of(
                                AttributeField.plain("Subtitle", Threat::subName),
                                AttributeField.plain("Unique", t -> t.unique() ? "Yes" : ""),
                                AttributeField.plain("Attributes", t -> String.format(
                                        "CHA %s | DEX %s | MIN %s | SPI %s | STR %s",
                                        dashIfBlank(t.charisma()), dashIfBlank(t.dexterity()),
                                        dashIfBlank(t.mind()), dashIfBlank(t.spirit()), dashIfBlank(t.strength()))),
                                AttributeField.plain("Movement", t -> joinLabeled(
                                        "Walk", t.moveWalk(), "Fly", t.moveFly(), "Swim", t.moveSwim())),
                                AttributeField.plain("Toughness", Threat::tough),
                                AttributeField.plain("Shock", Threat::shock),
                                AttributeField.plain("Wounds", Threat::wounds),
                                AttributeField.plain("Possibilities", Threat::possibilities),
                                AttributeField.plain("Skills", t -> formatStringMap(t.skills())),
                                AttributeField.markup("Perks", t -> joinList(t.perks())),
                                AttributeField.markup("Equipment", t -> joinList(t.equipment())))),
                        new Prose<>("Quote", Threat::quote),
                        new Prose<>("", Threat::text),
                        new ProseMap<>("Special Abilities", Threat::specialAbilities)),
                null);
    }

    private static CatalogCategoryDescriptor<Publication> publicationDescriptor() {
        return new CatalogCategoryDescriptor<>(
                CatalogArea.PUBLICATION,
                Publication.class,
                List.of(
                        ColumnDescriptor.primaryName(),
                        ColumnDescriptor.text("codexId", "Codex ID", true, Publication::codexId),
                        ColumnDescriptor.text("thirdParty", "Publisher / Line", true,
                                p -> p.thirdParty() == null || p.thirdParty().isBlank() ? "Ulisses Spiele" : p.thirdParty()),
                        ColumnDescriptor.externalLink("store", "DriveThruRPG", Publication::productUrl)),
                List.of(),
                List.of(
                        new Attributes<>("Publication Metadata", List.of(
                                AttributeField.plain("Codex ID", Publication::codexId),
                                AttributeField.plain("Publisher / Line",
                                        p -> p.thirdParty() == null || p.thirdParty().isBlank() ? "Ulisses Spiele" : p.thirdParty()),
                                AttributeField.plain("Primary Product ID",
                                        p -> p.primaryProductId() > 0 ? String.valueOf(p.primaryProductId()) : ""),
                                AttributeField.plain("DriveThruRPG Store", Publication::productUrl)))),
                null);
    }

    private static CatalogCategoryDescriptor<Article> articleDescriptor() {
        return new CatalogCategoryDescriptor<>(
                CatalogArea.ARTICLE,
                Article.class,
                List.of(
                        ColumnDescriptor.primaryName(),
                        ColumnDescriptor.clearance()),
                List.of(FilterFacetDescriptor.grouping(GroupingDimension.CLEARANCE)),
                List.of(new Prose<>("", Article::text)),
                null);
    }

    private static CatalogCategoryDescriptor<Tag> tagDescriptor() {
        return new CatalogCategoryDescriptor<>(
                CatalogArea.TAG,
                Tag.class,
                List.of(
                        ColumnDescriptor.primaryName(),
                        ColumnDescriptor.clearance(),
                        ColumnDescriptor.flag("hasParent", "Sub-Tag", t -> t.parent() != null)),
                List.of(FilterFacetDescriptor.grouping(GroupingDimension.CLEARANCE)),
                List.of(new EmbeddedReferences<>("Parent Tag", CatalogArea.TAG,
                        t -> t.parent() == null ? List.of() : List.of(t.parent()))),
                null);
    }

    private static Set<String> singleSlug(final String raw) {
        String slug = CatalogSlugs.normalize(raw);
        return slug.isEmpty() ? Set.of() : Set.of(slug);
    }

    private static String displaySlug(final String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim();
        return trimmed.contains("-") || trimmed.equals(trimmed.toLowerCase())
                ? EntityType.toDisplayName(CatalogSlugs.normalize(trimmed))
                : trimmed;
    }

    private static String formatSlugs(final Set<String> slugs) {
        if (slugs == null || slugs.isEmpty()) {
            return "";
        }
        return slugs.stream()
                .filter(Objects::nonNull)
                .sorted()
                .map(EntityType::toDisplayName)
                .collect(Collectors.joining(", "));
    }

    private static String formatDn(final DifficultyNumber dn) {
        if (dn == null) {
            return "";
        }
        return joinNonBlank(" ", dn.level(), dn.text());
    }

    private static String formatItemAxioms(final Item item) {
        String tech = item.axiomTech() == null || item.axiomTech().isBlank() ? "" : "T" + item.axiomTech().trim();
        String magic = item.axiomMagic() == null || item.axiomMagic().isBlank() ? "" : "M" + item.axiomMagic().trim();
        return joinNonBlank(", ", tech, magic);
    }

    private static String formatIntMap(final Map<String, Integer> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
    }

    private static String formatStringMap(final Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + " " + e.getValue())
                .collect(Collectors.joining(", "));
    }

    private static String joinList(final List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        return items.stream().filter(Objects::nonNull).filter(s -> !s.isBlank()).collect(Collectors.joining(", "));
    }

    private static String joinNonBlank(final String delimiter, final String first, final String second) {
        boolean hasFirst = first != null && !first.isBlank();
        boolean hasSecond = second != null && !second.isBlank();
        if (hasFirst && hasSecond) {
            return first.trim() + delimiter + second.trim();
        }
        return hasFirst ? first.trim() : (hasSecond ? second.trim() : "");
    }

    private static String joinLabeled(
            final String l1, final String v1,
            final String l2, final String v2,
            final String l3, final String v3) {
        StringBuilder sb = new StringBuilder();
        appendLabeled(sb, l1, v1);
        appendLabeled(sb, l2, v2);
        appendLabeled(sb, l3, v3);
        return sb.toString();
    }

    private static void appendLabeled(final StringBuilder sb, final String label, final String value) {
        if (value != null && !value.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" | ");
            }
            sb.append(label).append(' ').append(value.trim());
        }
    }

    private static String dashIfBlank(final String value) {
        return value == null || value.isBlank() ? "–" : value.trim();
    }

    private static List<java.util.UUID> extractVehicleWeaponIds(final Vehicle vehicle) {
        if (vehicle.weaponry() == null || vehicle.weaponry().isEmpty()) {
            return List.of();
        }
        return vehicle.weaponry().stream()
                .filter(Objects::nonNull)
                .map(VehicleWeapon::weaponId)
                .filter(Objects::nonNull)
                .toList();
    }
}
