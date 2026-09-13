/*
 * Copyright (c) 2026. Roland T. Lichti <rlichti@kaiserpfalz-edv.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package de.paladinsinn.torg.codex.api.mapper;

import de.paladinsinn.torg.codex.application.port.in.CatalogReference;
import de.paladinsinn.torg.codex.application.port.in.CatalogReferenceQuery;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TorgMappingSupportTest {

    private final Map<String, CatalogReference> cosms = Map.of(
            "core-earth", new CatalogReference(UUID.randomUUID(), "Core Earth"),
            "tharkold", new CatalogReference(UUID.randomUUID(), "Tharkold"));
    private final CatalogReferenceQuery references = new CatalogReferenceQuery() {
        @Override
        public Optional<CatalogReference> findCosmBySlug(String slug) {
            return Optional.ofNullable(cosms.get(slug));
        }

        @Override
        public Optional<CatalogReference> findPublicationByCodexId(String codexId) {
            return Optional.empty();
        }

        @Override
        public List<de.paladinsinn.torg.codex.application.port.in.CatalogPublicationReference>
                findPublicationsByProductId(int productId) {
            return List.of();
        }
    };
    private final TorgMappingSupport support = new TorgMappingSupport(references);

    @Test
    void foldsCosmsIntoTheCompatibleReferenceShape() {
        final UUID coreEarthId = cosms.get("core-earth").id();

        assertThat(support.toCosmRef(Set.of())).isNull();
        assertThat(support.toCosmRef(Set.of("core-earth")))
                .satisfies(result -> {
                    assertThat(result.id()).isEqualTo(coreEarthId);
                    assertThat(result.name()).isEqualTo("Core Earth");
                });
        assertThat(support.toCosmRef(Set.of("unknown")))
                .satisfies(result -> {
                    assertThat(result.id()).isNull();
                    assertThat(result.name()).isEqualTo("unknown");
                });
        assertThat(support.toCosmRef(new LinkedHashSet<>(Set.of("tharkold", "unknown", "core-earth"))))
                .satisfies(result -> {
                    assertThat(result.id()).isNull();
                    assertThat(result.name()).isEqualTo("Core Earth, Tharkold, unknown");
                });
    }

    @Test
    void foldsUnlockingPerksDeterministically() {
        assertThat(support.foldSlugs(null)).isNull();
        assertThat(support.foldSlugs(Set.of())).isNull();
        assertThat(support.foldSlugs(Set.of("miracles"))).isEqualTo("miracles");
        assertThat(support.foldSlugs(Set.of("miracles", "exemplar-of-light")))
                .isEqualTo("exemplar-of-light, miracles");
    }
}
