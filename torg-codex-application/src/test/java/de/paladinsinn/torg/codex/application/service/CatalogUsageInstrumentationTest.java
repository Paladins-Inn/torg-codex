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

import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.application.port.out.CatalogUsageObserver;
import de.paladinsinn.torg.codex.domain.access.CatalogAccessPolicy;
import de.paladinsinn.torg.codex.domain.access.EntryAccess;
import de.paladinsinn.torg.codex.domain.access.OwnedProducts;
import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import de.paladinsinn.torg.codex.domain.catalog.DetailAccessOutcome;
import de.paladinsinn.torg.codex.domain.catalog.GroupingDimension;
import de.paladinsinn.torg.codex.domain.markup.EntityType;
import de.paladinsinn.torg.codex.domain.model.Article;
import de.paladinsinn.torg.codex.domain.model.ClearanceLevel;
import de.paladinsinn.torg.codex.domain.model.Cosm;
import de.paladinsinn.torg.codex.domain.model.PerkGroup;
import de.paladinsinn.torg.codex.domain.model.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogUsageInstrumentationTest {

    private static final class RecordingUsageObserver implements CatalogUsageObserver {
        final List<String> events = new ArrayList<>();

        @Override
        public void categoryAccessed(final CatalogArea area) {
            events.add("category:" + area.slug());
        }

        @Override
        public void cosmAccessed(final String cosmSlug) {
            events.add("cosm:" + cosmSlug);
        }

        @Override
        public void groupingAccessed(final GroupingDimension dimension, final String groupKey) {
            events.add("grouping:" + dimension.getTag() + ":" + groupKey);
        }

        @Override
        public void detailAccessed(final CatalogArea area, final String entrySlug, final DetailAccessOutcome outcome) {
            events.add("detail:" + area.slug() + ":" + entrySlug + ":" + outcome.getTag());
        }

        @Override
        public void detailNotFound(final CatalogArea area) {
            events.add("not-found:" + area.slug());
        }
    }

    @Test
    void recordsRestrictedListCosmGroupingAndDetailOutcomesBySlugAndUuid() {
        UUID coreGroupId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID aysleGroupId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID orphanGroupId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        PerkGroup prowess = new PerkGroup(coreGroupId, "Prowess", ClearanceLevel.ALPHA, Set.of("core-rulebook"), "txt", null);
        PerkGroup spellcraft = new PerkGroup(aysleGroupId, "Spellcraft", ClearanceLevel.ALPHA, Set.of("sourcebook-aysle"), "txt", null);
        PerkGroup orphan = new PerkGroup(orphanGroupId, "Orphan", ClearanceLevel.ALPHA, Set.of(), "txt", null);
        List<PerkGroup> groups = List.of(prowess, spellcraft, orphan);

        RecordingUsageObserver usage = new RecordingUsageObserver();
        var service = new RestrictedCatalogQueryService<>(
                EntityType.GROUP,
                inMemoryPort(groups),
                OwnedProducts::anonymous,
                (a, id, name) -> { },
                new CatalogAccessPolicy(),
                usage);

        assertThat(service.peekAll()).containsExactly(prowess);
        assertThat(usage.events).isEmpty();

        assertThat(service.findAll()).containsExactly(prowess);
        assertThat(service.findByIdentifier("prowess")).isInstanceOf(EntryAccess.Granted.class);
        assertThat(service.findByIdentifier("spellcraft")).isInstanceOf(EntryAccess.Teaser.class);
        assertThat(service.findByIdentifier("orphan")).isInstanceOf(EntryAccess.Absent.class);
        assertThat(service.findByIdentifier("unknown-group")).isInstanceOf(EntryAccess.Absent.class);

        assertThat(usage.events).containsExactly(
                "category:groups",
                "detail:groups:prowess:drm-granted",
                "grouping:perk-group:prowess",
                "detail:groups:spellcraft:teaser",
                "not-found:groups",
                "not-found:groups");
    }

    @Test
    void recordsCosmAndPublicReferenceAccesses() {
        UUID cosmId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        Cosm aysle = new Cosm(cosmId, "aysle", "Aysle", ClearanceLevel.ALPHA, Set.of("core-rulebook"), Map.of(), "txt", "laws");
        RecordingUsageObserver usage = new RecordingUsageObserver();

        var cosmService = new RestrictedCatalogQueryService<>(
                EntityType.COSM,
                inMemoryPort(List.of(aysle)),
                OwnedProducts::anonymous,
                (a, id, name) -> { },
                new CatalogAccessPolicy(),
                usage);
        assertThat(cosmService.findByIdentifier("aysle")).isInstanceOf(EntryAccess.Granted.class);

        UUID articleId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        Article changelog = new Article(articleId, "Changelog", ClearanceLevel.ALPHA, "v1");
        var articleService = new CatalogQueryService<>(CatalogArea.ARTICLE, inMemoryPort(List.of(changelog)), usage);
        assertThat(articleService.findByIdentifier("changelog")).contains(changelog);
        assertThat(articleService.findByIdentifier("missing-article")).isEmpty();

        UUID tagId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        Tag combat = new Tag(tagId, "Combat", ClearanceLevel.ALPHA, null);
        var tagService = new CatalogQueryService<>(CatalogArea.TAG, inMemoryPort(List.of(combat)), usage);
        assertThat(tagService.findById(tagId)).contains(combat);

        assertThat(usage.events).containsExactly(
                "detail:cosms:aysle:drm-granted",
                "cosm:aysle",
                "detail:articles:changelog:public-reference",
                "not-found:articles",
                "detail:tags:combat:public-reference",
                "grouping:tag:combat");
    }

    private static <T extends de.paladinsinn.torg.codex.domain.model.CatalogEntry> CatalogPersistencePort<T> inMemoryPort(
            final List<T> items) {
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
