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

import de.paladinsinn.torg.codex.application.port.in.CatalogPublicationReference;
import de.paladinsinn.torg.codex.application.port.in.CatalogReference;
import de.paladinsinn.torg.codex.application.port.out.CatalogAccessObserver;
import de.paladinsinn.torg.codex.application.port.out.CatalogEntryReference;
import de.paladinsinn.torg.codex.application.port.out.CatalogReferencePersistencePort;
import de.paladinsinn.torg.codex.domain.access.CatalogAccessPolicy;
import de.paladinsinn.torg.codex.domain.markup.EntityType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogReferenceQueryServiceTest {

    private final CatalogEntryReference aysle = new CatalogEntryReference(UUID.randomUUID(), "Aysle", Set.of("sourcebook-aysle"));
    private final CatalogEntryReference orphan = new CatalogEntryReference(UUID.randomUUID(), "Orphan", Set.of());
    private final CatalogReference publication = new CatalogReference(UUID.randomUUID(), "Aysle Sourcebook");
    private final List<String> reports = new ArrayList<>();
    private final CatalogAccessObserver observer = (area, id, name) -> reports.add(area.tag() + ":" + id + ":" + name);

    private final CatalogReferenceQueryService service = new CatalogReferenceQueryService(new CatalogReferencePersistencePort() {
        private final Map<String, CatalogEntryReference> cosms = Map.of("aysle", aysle, "orphan", orphan);

        @Override
        public Optional<CatalogEntryReference> findCosmBySlug(String slug) {
            return Optional.ofNullable(cosms.get(slug));
        }

        @Override
        public Optional<CatalogReference> findPublicationByCodexId(String codexId) {
            return "sourcebook-aysle".equals(codexId) ? Optional.of(publication) : Optional.empty();
        }

        @Override
        public List<CatalogPublicationReference> findPublicationsByProductId(int productId) {
            return List.of(new CatalogPublicationReference(publication.id(), publication.name(), "sourcebook-aysle"));
        }
    }, observer, new CatalogAccessPolicy());

    @Test
    void assignedCosmResolvesToIdAndNameRegardlessOfOwnership() {
        assertThat(service.findCosmBySlug("aysle")).contains(new CatalogReference(aysle.id(), "Aysle"));
        assertThat(reports).isEmpty();
    }

    @Test
    void unassignedCosmIsHiddenAndReported() {
        assertThat(service.findCosmBySlug("orphan")).isEmpty();
        assertThat(reports).containsExactly(EntityType.COSM.tag() + ":" + orphan.id() + ":Orphan");
    }

    @Test
    void unknownCosmIsEmptyWithoutReport() {
        assertThat(service.findCosmBySlug("nowhere")).isEmpty();
        assertThat(reports).isEmpty();
    }

    @Test
    void publicationLookupsAreUnchanged() {
        assertThat(service.findPublicationByCodexId("sourcebook-aysle")).contains(publication);
        assertThat(service.findPublicationByCodexId("unknown")).isEmpty();
        assertThat(service.findPublicationsByProductId(42)).extracting(CatalogPublicationReference::codexId)
                .containsExactly("sourcebook-aysle");
    }

    @Test
    void entryReferenceIsAProductBoundCosm() {
        assertThat(aysle.entityType()).isEqualTo(EntityType.COSM);
    }
}
