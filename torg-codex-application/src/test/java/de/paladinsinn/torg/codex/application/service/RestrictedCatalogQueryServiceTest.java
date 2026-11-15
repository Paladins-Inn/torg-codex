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

import de.paladinsinn.torg.codex.application.port.out.CallerProductsPort;
import de.paladinsinn.torg.codex.application.port.out.CatalogAccessObserver;
import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.domain.access.CatalogAccessPolicy;
import de.paladinsinn.torg.codex.domain.access.EntryAccess;
import de.paladinsinn.torg.codex.domain.access.OwnedProducts;
import de.paladinsinn.torg.codex.domain.markup.EntityType;
import de.paladinsinn.torg.codex.domain.model.Spell;
import de.paladinsinn.torg.codex.domain.model.SpellList;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RestrictedCatalogQueryServiceTest {

    private final Spell core = spell("Core Spell", Set.of("core-rulebook"));
    private final Spell aysle = spell("Aysle Spell", Set.of("sourcebook-tharkold", "sourcebook-aysle"));
    private final Spell unassigned = spell("Orphan Spell", Set.of());

    private final FakeSpellPersistence persistence = new FakeSpellPersistence(List.of(core, aysle, unassigned));
    private final CountingCallerProducts caller = new CountingCallerProducts();
    private final RecordingObserver observer = new RecordingObserver();

    private final RestrictedCatalogQueryService<Spell> service = new RestrictedCatalogQueryService<>(
            EntityType.SPELL, persistence, caller, observer, new CatalogAccessPolicy());

    @Test
    void anonymousListContainsOnlyFreeTierEntries() {
        assertThat(service.findAll()).containsExactly(core);
    }

    @Test
    void ownerListContainsFreeTierAndOwnedEntries() {
        caller.owned = OwnedProducts.of(Set.of("sourcebook-aysle"));

        assertThat(service.findAll()).containsExactly(core, aysle);
    }

    @Test
    void listReportsAndDropsUnassignedEntries() {
        caller.owned = OwnedProducts.of(Set.of("sourcebook-aysle", "sourcebook-tharkold"));

        assertThat(service.findAll()).doesNotContain(unassigned);
        assertThat(observer.reports).containsExactly(new Report(EntityType.SPELL, unassigned.id(), "Orphan Spell"));
    }

    @Test
    void cosmListAppliesTheSameRules() {
        persistence.cosmEntries = List.of(aysle, core, unassigned);

        assertThat(service.findByCosm("aysle")).containsExactly(core);
        assertThat(persistence.lastCosm).isEqualTo("aysle");
        assertThat(observer.reports).hasSize(1);
    }

    @Test
    void detailOfAnOwnedEntryIsGranted() {
        assertThat(service.findById(core.id())).isEqualTo(EntryAccess.granted(core));
    }

    @Test
    void detailOfAnUnownedEntryIsASortedTeaser() {
        EntryAccess<Spell> access = service.findById(aysle.id());

        assertThat(access).isInstanceOf(EntryAccess.Teaser.class);
        EntryAccess.Teaser<Spell> teaser = (EntryAccess.Teaser<Spell>) access;
        assertThat(teaser.id()).isEqualTo(aysle.id());
        assertThat(teaser.name()).isEqualTo("Aysle Spell");
        assertThat(teaser.type()).isEqualTo(EntityType.SPELL);
        assertThat(teaser.requiredProducts()).containsExactly("sourcebook-aysle", "sourcebook-tharkold");
        assertThat(observer.reports).isEmpty();
    }

    @Test
    void detailOfAnUnassignedEntryIsAbsentAndReported() {
        assertThat(service.findById(unassigned.id())).isInstanceOf(EntryAccess.Absent.class);
        assertThat(observer.reports).containsExactly(new Report(EntityType.SPELL, unassigned.id(), "Orphan Spell"));
    }

    @Test
    void detailOfAMissingEntryIsAbsentAndNotReported() {
        EntryAccess<Spell> missing = service.findById(UUID.randomUUID());

        assertThat(missing).isEqualTo(service.findById(unassigned.id()));
        assertThat(observer.reports).hasSize(1);
    }

    @Test
    void repeatedRequestsReportEveryEncounterWithoutDeduplication() {
        service.findAll();
        service.findAll();
        service.findById(unassigned.id());

        assertThat(observer.reports).hasSize(3);
    }

    @Test
    void ownershipIsResolvedExactlyOncePerInvocation() {
        service.findAll();
        assertThat(caller.calls).isEqualTo(1);

        service.findByCosm("aysle");
        assertThat(caller.calls).isEqualTo(2);

        service.findById(aysle.id());
        assertThat(caller.calls).isEqualTo(3);
    }

    @Test
    void listsKeepOnlyAccessibleReferencesInTheirOriginalOrder() {
        UUID missing = UUID.randomUUID();
        SpellList first = spellList("First", Set.of("core-rulebook"), aysle.id(), core.id(), missing, unassigned.id());
        SpellList second = spellList("Second", Set.of("core-rulebook"), unassigned.id(), core.id());
        RestrictedCatalogQueryService<SpellList> lists = listService(List.of(first, second));

        assertThat(lists.findAll()).extracting(SpellList::spells)
                .containsExactly(List.of(core.id()), List.of(core.id()));

        caller.owned = OwnedProducts.of(Set.of("sourcebook-aysle"));
        assertThat(lists.findAll()).extracting(SpellList::spells)
                .containsExactly(List.of(aysle.id(), core.id()), List.of(core.id()));
    }

    @Test
    void referencesAreResolvedWithOneBatchAndOneOwnershipLookupPerInvocation() {
        UUID missing = UUID.randomUUID();
        SpellList first = spellList("First", Set.of("core-rulebook"), aysle.id(), core.id(), missing);
        SpellList second = spellList("Second", Set.of("core-rulebook"), unassigned.id(), core.id());
        RestrictedCatalogQueryService<SpellList> lists = listService(List.of(first, second));

        lists.findAll();

        assertThat(persistence.batchRequests).hasSize(1);
        assertThat(persistence.batchRequests.getFirst())
                .containsExactlyInAnyOrder(aysle.id(), core.id(), missing, unassigned.id());
        assertThat(caller.calls).isEqualTo(1);
    }

    @Test
    void unassignedReferencesAreReportedOncePerInvocation() {
        SpellList first = spellList("First", Set.of("core-rulebook"), unassigned.id());
        SpellList second = spellList("Second", Set.of("core-rulebook"), unassigned.id(), core.id());
        RestrictedCatalogQueryService<SpellList> lists = listService(List.of(first, second));

        lists.findAll();
        assertThat(observer.reports).containsExactly(new Report(EntityType.SPELL, unassigned.id(), "Orphan Spell"));

        lists.findAll();
        assertThat(observer.reports).hasSize(2);
    }

    @Test
    void grantedDetailsHaveFilteredReferences() {
        SpellList list = spellList("Detail", Set.of("core-rulebook"), aysle.id(), core.id());
        RestrictedCatalogQueryService<SpellList> lists = listService(List.of(list));

        EntryAccess<SpellList> access = lists.findById(list.id());

        assertThat(access).isInstanceOf(EntryAccess.Granted.class);
        assertThat(((EntryAccess.Granted<SpellList>) access).entry().spells()).containsExactly(core.id());
        assertThat(persistence.batchRequests).hasSize(1);
        assertThat(caller.calls).isEqualTo(1);
    }

    @Test
    void teaserAndAbsentResultsResolveNoReferences() {
        SpellList teased = spellList("Teased", Set.of("sourcebook-aysle"), core.id());
        SpellList orphan = spellList("Orphan", Set.of(), core.id());
        RestrictedCatalogQueryService<SpellList> lists = listService(List.of(teased, orphan));

        assertThat(lists.findById(teased.id())).isInstanceOf(EntryAccess.Teaser.class);
        assertThat(lists.findById(orphan.id())).isInstanceOf(EntryAccess.Absent.class);
        assertThat(lists.findById(UUID.randomUUID())).isInstanceOf(EntryAccess.Absent.class);
        assertThat(persistence.batchRequests).isEmpty();
    }

    @Test
    void listsWithoutReferencesNeedNoBatch() {
        RestrictedCatalogQueryService<SpellList> lists = listService(List.of(spellList("Empty", Set.of("core-rulebook"))));

        assertThat(lists.findAll()).extracting(SpellList::spells).containsExactly(List.of());
        assertThat(persistence.batchRequests).isEmpty();
    }

    private RestrictedCatalogQueryService<SpellList> listService(List<SpellList> entries) {
        CatalogAccessPolicy policy = new CatalogAccessPolicy();
        return RestrictedCatalogQueryService.withReferences(EntityType.SPELL_LIST, new FakeListPersistence(entries), caller,
                observer, policy, new ReferencedEntryResolver<>(persistence, observer, policy));
    }

    private static SpellList spellList(String name, Set<String> products, UUID... spells) {
        return new SpellList(UUID.randomUUID(), name, null, products, Set.of(), Set.of(), new ArrayList<>(List.of(spells)),
                "text", null, null);
    }

    private static Spell spell(String name, Set<String> products) {
        return new Spell(UUID.randomUUID(), name, null, products, null, null, null, null, null, null, "text");
    }

    record Report(EntityType area, UUID id, String name) {
    }

    static final class RecordingObserver implements CatalogAccessObserver {
        final List<Report> reports = new ArrayList<>();

        @Override
        public void entryWithoutProducts(EntityType area, UUID id, String name) {
            reports.add(new Report(area, id, name));
        }
    }

    static final class CountingCallerProducts implements CallerProductsPort {
        OwnedProducts owned = OwnedProducts.anonymous();
        int calls;

        @Override
        public OwnedProducts current() {
            calls++;
            return owned;
        }
    }

    static final class FakeListPersistence implements CatalogPersistencePort<SpellList> {
        private final List<SpellList> entries;

        FakeListPersistence(List<SpellList> entries) {
            this.entries = entries;
        }

        @Override
        public List<SpellList> findAll() {
            return entries;
        }

        @Override
        public Optional<SpellList> findById(UUID id) {
            return entries.stream().filter(entry -> entry.id().equals(id)).findFirst();
        }

        @Override
        public List<SpellList> findByCosm(String cosm) {
            return entries;
        }
    }

    static final class FakeSpellPersistence implements CatalogPersistencePort<Spell> {
        private final List<Spell> entries;
        List<Spell> cosmEntries = List.of();
        String lastCosm;
        final List<List<UUID>> batchRequests = new ArrayList<>();

        @Override
        public List<Spell> findAllById(Collection<UUID> ids) {
            batchRequests.add(List.copyOf(ids));
            return entries.stream().filter(entry -> ids.contains(entry.id())).toList();
        }

        FakeSpellPersistence(List<Spell> entries) {
            this.entries = entries;
        }

        @Override
        public List<Spell> findAll() {
            return entries;
        }

        @Override
        public Optional<Spell> findById(UUID id) {
            return entries.stream().filter(entry -> entry.id().equals(id)).findFirst();
        }

        @Override
        public List<Spell> findByCosm(String cosm) {
            lastCosm = cosm;
            return cosmEntries;
        }
    }
}
