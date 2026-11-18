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

package de.paladinsinn.torg.codex.application.service;

import de.paladinsinn.torg.codex.application.port.in.CatalogBrowseResult;
import de.paladinsinn.torg.codex.application.port.in.CatalogDetailResult;
import de.paladinsinn.torg.codex.application.port.in.CatalogFilterCriteria;
import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.application.port.out.CatalogUsageObserver;
import de.paladinsinn.torg.codex.application.service.CatalogBrowseService.AreaSource;
import de.paladinsinn.torg.codex.domain.access.CatalogAccessPolicy;
import de.paladinsinn.torg.codex.domain.access.OwnedProducts;
import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import de.paladinsinn.torg.codex.domain.catalog.DetailAccessOutcome;
import de.paladinsinn.torg.codex.domain.catalog.GroupingDimension;
import de.paladinsinn.torg.codex.domain.markup.EntityType;
import de.paladinsinn.torg.codex.domain.model.Article;
import de.paladinsinn.torg.codex.domain.model.CatalogEntry;
import de.paladinsinn.torg.codex.domain.model.ClearanceLevel;
import de.paladinsinn.torg.codex.domain.model.Cosm;
import de.paladinsinn.torg.codex.domain.model.Perk;
import de.paladinsinn.torg.codex.domain.model.PerkGroup;
import de.paladinsinn.torg.codex.domain.model.Publication;
import de.paladinsinn.torg.codex.domain.model.Race;
import de.paladinsinn.torg.codex.domain.model.Spell;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogBrowseServiceTest {

    @Test
    void filtersSortsAndCountsOnlyKnownValidFacetsWithoutDoubleCountingCategories() {
        List<String> events = new ArrayList<>();
        CatalogUsageObserver usage = new CatalogUsageObserver() {
            @Override
            public void categoryAccessed(final CatalogArea area) {
                events.add("cat:" + area.slug());
            }

            @Override
            public void cosmAccessed(final String cosmSlug) {
                events.add("cosm:" + cosmSlug);
            }

            @Override
            public void groupingAccessed(final GroupingDimension dimension, final String groupKey) {
                events.add("grp:" + dimension.getTag() + ":" + groupKey);
            }

            @Override
            public void detailAccessed(final CatalogArea area, final String entrySlug, final DetailAccessOutcome outcome) {
                events.add("det:" + area.slug() + ":" + entrySlug + ":" + outcome.getTag());
            }

            @Override
            public void detailNotFound(final CatalogArea area) {
                events.add("nf:" + area.slug());
            }
        };

        Cosm aysle = new Cosm(UUID.randomUUID(), "aysle", "Aysle", ClearanceLevel.ALPHA, Set.of("core-rulebook"), Map.of(), "", "");
        Race elf = new Race(UUID.randomUUID(), "Elf", ClearanceLevel.ALPHA, Set.of("core-rulebook"), true, Map.of(), "", "", "");
        PerkGroup prowess = new PerkGroup(UUID.randomUUID(), "Prowess", ClearanceLevel.ALPHA, Set.of("core-rulebook"), "", "");
        PerkGroup racial = new PerkGroup(UUID.randomUUID(), "Racial", ClearanceLevel.ALPHA, Set.of("core-rulebook"), "", "");

        Perk acePilot = new Perk(UUID.randomUUID(), "Ace Pilot", ClearanceLevel.BETA, Set.of("core-rulebook"),
                false, Set.of("aysle"), "prowess", "Air vehicles", "txt");
        Perk elfSight = new Perk(UUID.randomUUID(), "Elf Sight", ClearanceLevel.ALPHA, Set.of("core-rulebook"),
                false, Set.of("aysle"), "racial", "Elf", "txt");
        Perk lockedPerk = new Perk(UUID.randomUUID(), "Aysle Secret", ClearanceLevel.ALPHA, Set.of("sourcebook-aysle"),
                false, Set.of("aysle"), "prowess", "", "txt");
        Publication aysleBook = new Publication(UUID.randomUUID(), "sourcebook-aysle", "Aysle Sourcebook", 295843, null, Set.of(295843));

        CatalogAccessPolicy policy = new CatalogAccessPolicy();
        var browse = new CatalogBrowseService(List.of(
                AreaSource.restricted(CatalogArea.COSM, new RestrictedCatalogQueryService<>(
                        EntityType.COSM, port(List.of(aysle)), OwnedProducts::anonymous, (a, i, n) -> { }, policy, usage)),
                AreaSource.restricted(CatalogArea.RACE, new RestrictedCatalogQueryService<>(
                        EntityType.RACE, port(List.of(elf)), OwnedProducts::anonymous, (a, i, n) -> { }, policy, usage)),
                AreaSource.restricted(CatalogArea.PERK_GROUP, new RestrictedCatalogQueryService<>(
                        EntityType.GROUP, port(List.of(prowess, racial)), OwnedProducts::anonymous, (a, i, n) -> { }, policy, usage)),
                AreaSource.restricted(CatalogArea.PERK, new RestrictedCatalogQueryService<>(
                        EntityType.PERK, port(List.of(acePilot, elfSight, lockedPerk)), OwnedProducts::anonymous, (a, i, n) -> { }, policy, usage)),
                AreaSource.publications(new CatalogQueryService<>(CatalogArea.PUBLICATION, port(List.of(aysleBook)), usage))
        ), usage);

        CatalogFilterCriteria bogusOnlyCriteria = new CatalogFilterCriteria(
                "",
                Set.of("bogus-cosm-999"),
                Map.of(GroupingDimension.RACE, Set.of("bogus-race-999")),
                "name",
                CatalogFilterCriteria.SortDirection.ASC);

        CatalogBrowseResult bogusResult = browse.browse(CatalogArea.PERK, bogusOnlyCriteria);
        assertThat(bogusResult.totalAccessibleInCategory()).isEqualTo(2);
        assertThat(bogusResult.entries()).isEmpty();
        assertThat(events).containsExactly("cat:perks");

        events.clear();
        CatalogFilterCriteria validCriteria = new CatalogFilterCriteria(
                "elf",
                Set.of("aysle", "bogus-cosm-999"),
                Map.of(GroupingDimension.RACE, Set.of("elf", "bogus-race-999")),
                "name",
                CatalogFilterCriteria.SortDirection.ASC);
        CatalogBrowseResult matched = browse.browse(CatalogArea.PERK, validCriteria);
        assertThat(matched.entries()).extracting(e -> e.entry().name()).containsExactly("Elf Sight");
        assertThat(events).containsExactly("cat:perks", "cosm:aysle", "grp:race:elf");

        // Teaser lookup enriches required product with Publication metadata
        CatalogDetailResult teaser = browse.findDetail(CatalogArea.PERK, "aysle-secret");
        assertThat(teaser).isInstanceOf(CatalogDetailResult.RestrictedTeaser.class);
        var teaserResult = (CatalogDetailResult.RestrictedTeaser) teaser;
        assertThat(teaserResult.requiredProducts()).hasSize(1);
        assertThat(teaserResult.requiredProducts().getFirst().publication()).contains(aysleBook);
    }

    @Test
    void groupDetailListsOnlyAccessibleMembersSortedByNameWithCanonicalLinks() {
        PerkGroup group = new PerkGroup(UUID.randomUUID(), "Prowess", ClearanceLevel.ALPHA,
                Set.of("core-rulebook"), "", "");
        PerkGroup empty = new PerkGroup(UUID.randomUUID(), "Empty", ClearanceLevel.ALPHA,
                Set.of("core-rulebook"), "", "");
        Perk zeta = perk("Zeta", Set.of("core-rulebook"), "prowess");
        Perk alpha = perk("Alpha", Set.of("core-rulebook"), " Prowess ");
        Perk unrelated = perk("Alpha", Set.of("core-rulebook"), "racial");
        Perk locked = perk("Locked", Set.of("sourcebook-aysle"), "prowess");
        Perk unassigned = perk("Unassigned", Set.of(), "prowess");
        Perk withoutGroup = perk("No Group", Set.of("core-rulebook"), null);
        CatalogAccessPolicy policy = new CatalogAccessPolicy();
        CatalogUsageObserver usage = CatalogUsageObserver.NO_OP;
        var browse = new CatalogBrowseService(List.of(
                AreaSource.restricted(CatalogArea.PERK_GROUP, new RestrictedCatalogQueryService<>(
                        EntityType.GROUP, port(List.of(group, empty)), OwnedProducts::anonymous,
                        (a, i, n) -> { }, policy, usage)),
                AreaSource.restricted(CatalogArea.PERK, new RestrictedCatalogQueryService<>(
                        EntityType.PERK, port(List.of(zeta, alpha, unrelated, locked, unassigned, withoutGroup)),
                        OwnedProducts::anonymous, (a, i, n) -> { }, policy, usage))
        ), usage);

        var result = (CatalogDetailResult.GrantedProductEntry) browse.findDetail(CatalogArea.PERK_GROUP, "prowess");
        assertThat(result.embeddedReferences()).containsOnlyKeys("Perks");
        assertThat(result.embeddedReferences().get("Perks")).containsExactly(
                new CatalogDetailResult.LinkedEntryRef(
                        alpha.id(), "Alpha", "alpha-" + alpha.id().toString().split("-")[0], CatalogArea.PERK),
                new CatalogDetailResult.LinkedEntryRef(zeta.id(), "Zeta", "zeta", CatalogArea.PERK));
        assertThat(browse.findDetail(CatalogArea.PERK_GROUP, group.id().toString())).isEqualTo(result);
        var emptyResult = (CatalogDetailResult.GrantedProductEntry) browse.findDetail(CatalogArea.PERK_GROUP, "empty");
        assertThat(emptyResult.embeddedReferences()).isEmpty();
    }

    @Test
    void publicationDetailComputesDistinctAccessibleCountsPerProductBoundAreaWithoutCategoryVisits() {
        List<String> usageEvents = new ArrayList<>();
        CatalogUsageObserver usage = new CatalogUsageObserver() {
            @Override
            public void categoryAccessed(final CatalogArea area) {
                usageEvents.add("cat:" + area.slug());
            }

            @Override
            public void cosmAccessed(final String cosmSlug) {
                usageEvents.add("cosm:" + cosmSlug);
            }

            @Override
            public void groupingAccessed(final GroupingDimension dimension, final String groupKey) {
                usageEvents.add("grp:" + dimension.getTag() + ":" + groupKey);
            }

            @Override
            public void detailAccessed(final CatalogArea area, final String entrySlug, final DetailAccessOutcome outcome) {
                usageEvents.add("det:" + area.slug() + ":" + entrySlug + ":" + outcome.getTag());
            }

            @Override
            public void detailNotFound(final CatalogArea area) {
                usageEvents.add("nf:" + area.slug());
            }
        };

        Publication paidA = new Publication(UUID.randomUUID(), "pct-paid-a", "Paid A", 1001, null, Set.of(1001));
        Publication paidB = new Publication(UUID.randomUUID(), "pct-paid-b", "Paid B", 1002, null, Set.of(1002));
        Publication noKey = new Publication(UUID.randomUUID(), "   ", "No Key Pub", 0, null, Set.of());
        Article article = new Article(UUID.randomUUID(), "Rules FAQ", ClearanceLevel.ALPHA, "text");

        UUID duplicateSpellId = UUID.randomUUID();
        Spell paidAOnly = spell(UUID.randomUUID(), "Paid A Only", Set.of("pct-paid-a"));
        Spell sharedSpellDuplicate1 = spell(duplicateSpellId, "Shared Spell", Set.of("pct-paid-a", "pct-paid-b"));
        Spell sharedSpellDuplicate2 = spell(duplicateSpellId, "Shared Spell", Set.of("pct-paid-a", "pct-paid-b"));
        Spell prefixMismatch = spell(UUID.randomUUID(), "Prefix Mismatch", Set.of("pct-paid-a-deluxe"));
        Spell unassigned = spell(UUID.randomUUID(), "Unassigned", Set.of());
        Perk corePerk = perk("Core Perk", Set.of("core-rulebook"), "");

        AtomicReference<OwnedProducts> callerRef = new AtomicReference<>(OwnedProducts.anonymous());
        AtomicInteger spellFindAllCalls = new AtomicInteger();
        AtomicInteger perkFindAllCalls = new AtomicInteger();

        CatalogPersistencePort<Spell> spellPort = countingPort(
                List.of(paidAOnly, sharedSpellDuplicate1, sharedSpellDuplicate2, prefixMismatch, unassigned),
                spellFindAllCalls);
        CatalogPersistencePort<Perk> perkPort = countingPort(List.of(corePerk), perkFindAllCalls);

        CatalogAccessPolicy policy = new CatalogAccessPolicy();
        var browse = new CatalogBrowseService(List.of(
                AreaSource.publications(new CatalogQueryService<>(
                        CatalogArea.PUBLICATION, port(List.of(paidA, paidB, noKey)), usage)),
                AreaSource.articles(new CatalogQueryService<>(
                        CatalogArea.ARTICLE, port(List.of(article)), usage)),
                AreaSource.restricted(CatalogArea.SPELL, new RestrictedCatalogQueryService<>(
                        EntityType.SPELL, spellPort, callerRef::get, (a, i, n) -> { }, policy, usage)),
                AreaSource.restricted(CatalogArea.PERK, new RestrictedCatalogQueryService<>(
                        EntityType.PERK, perkPort, callerRef::get, (a, i, n) -> { }, policy, usage))
        ), usage);

        // 1. Anonymous reader on paidA: 0 spells, 0 perks, at most 1 peek per area, no categoryAccessed
        var anonPaidA = (CatalogDetailResult.PublicReferenceEntry) browse.findDetail(
                CatalogArea.PUBLICATION, "pct-paid-a");
        assertThat(anonPaidA.contentCounts())
                .hasSize(14)
                .extracting(CatalogDetailResult.PublicationContentCount::area)
                .containsExactly(
                        CatalogArea.RACE, CatalogArea.PERK, CatalogArea.PERK_GROUP, CatalogArea.SPELL,
                        CatalogArea.SPELL_LIST, CatalogArea.MIRACLE, CatalogArea.MIRACLE_LIST, CatalogArea.POWER,
                        CatalogArea.POWER_LIST, CatalogArea.ITEM, CatalogArea.VEHICLE, CatalogArea.COSM,
                        CatalogArea.SHARD, CatalogArea.THREAT);
        assertThat(anonPaidA.contentCounts())
                .allSatisfy(c -> assertThat(c.count()).isZero());
        assertThat(spellFindAllCalls.get()).isEqualTo(1);
        assertThat(perkFindAllCalls.get()).isEqualTo(1);
        assertThat(usageEvents).noneMatch(e -> e.startsWith("cat:"));

        // 2. Owner of paidB on paidA: sees shared spell once (deduplicated UUID), not paidAOnly
        callerRef.set(OwnedProducts.of(Set.of("pct-paid-b")));
        var ownerBOnPaidA = (CatalogDetailResult.PublicReferenceEntry) browse.findDetail(
                CatalogArea.PUBLICATION, "pct-paid-a");
        assertThat(countFor(ownerBOnPaidA, CatalogArea.SPELL)).isEqualTo(1);

        // 3. Owner of paidA on paidA: sees paidAOnly + deduplicated sharedSpell = 2
        callerRef.set(OwnedProducts.of(Set.of("pct-paid-a")));
        var ownerAOnPaidA = (CatalogDetailResult.PublicReferenceEntry) browse.findDetail(
                CatalogArea.PUBLICATION, "pct-paid-a");
        assertThat(countFor(ownerAOnPaidA, CatalogArea.SPELL)).isEqualTo(2);

        // 4. Publication with blank codexId: all zero counts
        var noKeyResult = (CatalogDetailResult.PublicReferenceEntry) browse.findDetail(
                CatalogArea.PUBLICATION, noKey.id().toString());
        assertThat(noKeyResult.contentCounts())
                .hasSize(14)
                .allSatisfy(c -> assertThat(c.count()).isZero());

        // 5. Non-publication detail and missing publication do not read restricted areas
        int spellsBeforeNonPub = spellFindAllCalls.get();
        int perksBeforeNonPub = perkFindAllCalls.get();
        var articleDetail = (CatalogDetailResult.PublicReferenceEntry) browse.findDetail(
                CatalogArea.ARTICLE, "rules-faq");
        assertThat(articleDetail.contentCounts()).isEmpty();
        var missingPub = browse.findDetail(CatalogArea.PUBLICATION, "does-not-exist");
        assertThat(missingPub).isInstanceOf(CatalogDetailResult.Absent.class);
        assertThat(spellFindAllCalls.get()).isEqualTo(spellsBeforeNonPub);
        assertThat(perkFindAllCalls.get()).isEqualTo(perksBeforeNonPub);
    }

    @Test
    void browseFiltersByPublicationSelectionPreservesFacetsAndHandlesUnavailableSelections() {
        Publication paidA = new Publication(UUID.randomUUID(), "pct-paid-a", "Paid A", 1001, null, Set.of(1001));
        Publication paidB = new Publication(UUID.randomUUID(), "pct-paid-b", "Paid B", 1002, null, Set.of(1002));
        Cosm aysle = new Cosm(UUID.randomUUID(), "aysle", "Aysle", ClearanceLevel.ALPHA, Set.of("core-rulebook"), Map.of(), "", "");
        PerkGroup prowess = new PerkGroup(UUID.randomUUID(), "Prowess", ClearanceLevel.ALPHA, Set.of("core-rulebook"), "", "");

        UUID sharedPerkId = UUID.randomUUID();
        Perk paidAPerk1 = new Perk(UUID.randomUUID(), "Paid A Zeta", ClearanceLevel.ALPHA, Set.of("pct-paid-a"),
                false, Set.of("aysle"), "prowess", "", "");
        Perk paidAPerk2 = new Perk(sharedPerkId, "Shared Alpha", ClearanceLevel.BETA, Set.of("pct-paid-a", "pct-paid-b"),
                false, Set.of("aysle"), "prowess", "", "");
        Perk paidAPerk2Dup = new Perk(sharedPerkId, "Shared Alpha", ClearanceLevel.BETA, Set.of("pct-paid-a", "pct-paid-b"),
                false, Set.of("aysle"), "prowess", "", "");
        Perk corePerk = new Perk(UUID.randomUUID(), "Core Perk", ClearanceLevel.ALPHA, Set.of("core-rulebook"),
                false, Set.of("aysle"), "prowess", "", "");

        AtomicReference<OwnedProducts> callerRef = new AtomicReference<>(OwnedProducts.of(Set.of("pct-paid-a")));
        AtomicInteger pubFindAllCalls = new AtomicInteger();
        List<String> usageEvents = new ArrayList<>();
        CatalogUsageObserver usage = new CatalogUsageObserver() {
            @Override
            public void categoryAccessed(final CatalogArea area) {
                usageEvents.add("cat:" + area.slug());
            }

            @Override
            public void cosmAccessed(final String cosmSlug) {
                usageEvents.add("cosm:" + cosmSlug);
            }

            @Override
            public void groupingAccessed(final GroupingDimension dimension, final String groupKey) {
                usageEvents.add("grp:" + dimension.getTag() + ":" + groupKey);
            }

            @Override
            public void detailAccessed(final CatalogArea area, final String entrySlug, final DetailAccessOutcome outcome) {
                usageEvents.add("det:" + area.slug() + ":" + entrySlug + ":" + outcome.getTag());
            }

            @Override
            public void detailNotFound(final CatalogArea area) {
                usageEvents.add("nf:" + area.slug());
            }
        };

        CatalogAccessPolicy policy = new CatalogAccessPolicy();
        var browse = new CatalogBrowseService(List.of(
                AreaSource.publications(new CatalogQueryService<>(
                        CatalogArea.PUBLICATION, countingPort(List.of(paidA, paidB), pubFindAllCalls), usage)),
                AreaSource.restricted(CatalogArea.COSM, new RestrictedCatalogQueryService<>(
                        EntityType.COSM, port(List.of(aysle)), callerRef::get, (a, i, n) -> { }, policy, usage)),
                AreaSource.restricted(CatalogArea.PERK_GROUP, new RestrictedCatalogQueryService<>(
                        EntityType.GROUP, port(List.of(prowess)), callerRef::get, (a, i, n) -> { }, policy, usage)),
                AreaSource.restricted(CatalogArea.PERK, new RestrictedCatalogQueryService<>(
                        EntityType.PERK, port(List.of(paidAPerk1, paidAPerk2, paidAPerk2Dup, corePerk)),
                        callerRef::get, (a, i, n) -> { }, policy, usage))
        ), usage);

        // Known publication: deduplicates rows, scopes denominator to publication, combines with query/cosm/sort
        CatalogFilterCriteria knownCriteria = new CatalogFilterCriteria(
                "shared",
                Set.of("aysle"),
                Map.of(GroupingDimension.PERK_GROUP, Set.of("prowess")),
                "name",
                CatalogFilterCriteria.SortDirection.DESC,
                new CatalogFilterCriteria.PublicationFilter.ByUuid(paidA.id()));
        CatalogBrowseResult knownResult = browse.browse(CatalogArea.PERK, knownCriteria);
        assertThat(knownResult.publicationSelection())
                .isEqualTo(new CatalogBrowseResult.PublicationSelection.Selected(paidA.id(), "Paid A"));
        assertThat(knownResult.totalAccessibleInCategory()).isEqualTo(2);
        assertThat(knownResult.entries()).extracting(e -> e.entry().name()).containsExactly("Shared Alpha");
        assertThat(pubFindAllCalls.get()).isEqualTo(1);
        assertThat(usageEvents).containsExactly("cat:perks", "cosm:aysle", "grp:perk-group:prowess");

        // Unknown UUID: unavailable selection, 0 entries, no unfiltered fallback
        usageEvents.clear();
        UUID unknownId = UUID.randomUUID();
        CatalogBrowseResult unknownResult = browse.browse(CatalogArea.PERK, new CatalogFilterCriteria(
                "", Set.of(), Map.of(), "name", CatalogFilterCriteria.SortDirection.ASC,
                new CatalogFilterCriteria.PublicationFilter.ByUuid(unknownId)));
        assertThat(unknownResult.publicationSelection())
                .isEqualTo(new CatalogBrowseResult.PublicationSelection.Unavailable());
        assertThat(unknownResult.totalAccessibleInCategory()).isZero();
        assertThat(unknownResult.entries()).isEmpty();
        assertThat(usageEvents).containsExactly("cat:perks");

        // Invalid syntax: unavailable selection, 0 entries, no publication peek needed
        int pubCallsBeforeInvalid = pubFindAllCalls.get();
        CatalogBrowseResult invalidResult = browse.browse(CatalogArea.PERK, new CatalogFilterCriteria(
                "", Set.of(), Map.of(), "name", CatalogFilterCriteria.SortDirection.ASC,
                new CatalogFilterCriteria.PublicationFilter.Invalid()));
        assertThat(invalidResult.publicationSelection())
                .isEqualTo(new CatalogBrowseResult.PublicationSelection.Unavailable());
        assertThat(invalidResult.totalAccessibleInCategory()).isZero();
        assertThat(invalidResult.entries()).isEmpty();
        assertThat(pubFindAllCalls.get()).isEqualTo(pubCallsBeforeInvalid);
    }

    private static int countFor(final CatalogDetailResult.PublicReferenceEntry entry, final CatalogArea area) {
        return entry.contentCounts().stream()
                .filter(c -> c.area() == area)
                .findFirst()
                .map(CatalogDetailResult.PublicationContentCount::count)
                .orElse(-1);
    }

    private static Spell spell(final UUID id, final String name, final Set<String> products) {
        return new Spell(id, name, ClearanceLevel.ALPHA, products, "", Map.of(), "", null, "", "", "");
    }

    private static <T extends CatalogEntry> CatalogPersistencePort<T> countingPort(
            final List<T> items,
            final AtomicInteger findAllCounter) {
        return new CatalogPersistencePort<>() {
            @Override
            public List<T> findAll() {
                findAllCounter.incrementAndGet();
                return items;
            }

            @Override
            public Optional<T> findById(final UUID id) {
                return items.stream().filter(i -> i.id().equals(id)).findFirst();
            }

            @Override
            public List<T> findByCosm(final String cosm) {
                return items.stream().filter(i -> i.cosmSlugs().contains(cosm)).toList();
            }
        };
    }

    private static Perk perk(final String name, final Set<String> products, final String group) {
        return new Perk(UUID.randomUUID(), name, ClearanceLevel.ALPHA, products, false, Set.of(), group, "", "");
    }

    private static <T extends CatalogEntry> CatalogPersistencePort<T> port(final List<T> items) {
        return new CatalogPersistencePort<>() {
            @Override
            public List<T> findAll() {
                return items;
            }

            @Override
            public Optional<T> findById(final UUID id) {
                return items.stream().filter(i -> i.id().equals(id)).findFirst();
            }

            @Override
            public List<T> findByCosm(final String cosm) {
                return items.stream().filter(i -> i.cosmSlugs().contains(cosm)).toList();
            }
        };
    }
}
