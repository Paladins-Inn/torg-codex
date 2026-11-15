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

package de.paladinsinn.torg.codex.api.controller;

import de.paladinsinn.torg.codex.api.dto.EntryTeaserDto;
import de.paladinsinn.torg.codex.api.dto.PublicationRefDto;
import de.paladinsinn.torg.codex.api.dto.RequiredProductDto;
import de.paladinsinn.torg.codex.api.mapper.EntryTeaserMapperImpl;
import de.paladinsinn.torg.codex.api.mapper.TorgMappingSupport;
import de.paladinsinn.torg.codex.application.port.in.CatalogPublicationReference;
import de.paladinsinn.torg.codex.application.port.in.CatalogReference;
import de.paladinsinn.torg.codex.application.port.in.CatalogReferenceQuery;
import de.paladinsinn.torg.codex.domain.access.EntryAccess;
import de.paladinsinn.torg.codex.domain.model.Spell;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogAccessResponsesTest {

    private static final UUID AYSLE_PUBLICATION = UUID.randomUUID();

    private final CatalogReferenceQuery references = new CatalogReferenceQuery() {
        @Override
        public Optional<CatalogReference> findCosmBySlug(String slug) {
            return Optional.empty();
        }

        @Override
        public Optional<CatalogReference> findPublicationByCodexId(String codexId) {
            return "sourcebook-aysle".equals(codexId)
                    ? Optional.of(new CatalogReference(AYSLE_PUBLICATION, "Aysle Sourcebook"))
                    : Optional.empty();
        }

        @Override
        public List<CatalogPublicationReference> findPublicationsByProductId(int productId) {
            return List.of();
        }
    };

    private final CatalogAccessResponses responses =
            new CatalogAccessResponses(new EntryTeaserMapperImpl(new TorgMappingSupport(references)));

    private final Spell spell = new Spell(UUID.randomUUID(), "Fireball", null,
            Set.of("unpublished-thing", "sourcebook-aysle"), "magic", null, null, null, null, null, "secret text");

    @Test
    void grantedMapsToTheExistingDetail() {
        ResponseEntity<?> response = responses.toResponse(EntryAccess.granted(spell), Spell::text);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("secret text");
    }

    @Test
    void teaserMapsToForbiddenWithTheClosedTeaserBody() {
        ResponseEntity<?> response = responses.toResponse(EntryAccess.teaserOf(spell), Spell::text);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo(new EntryTeaserDto(spell.id(), "Fireball", "spell", List.of(
                new RequiredProductDto("sourcebook-aysle", new PublicationRefDto(AYSLE_PUBLICATION, "Aysle Sourcebook")),
                new RequiredProductDto("unpublished-thing", null))));
    }

    @Test
    void absentMapsToTheExistingEmptyNotFound() {
        ResponseEntity<?> response = responses.toResponse(EntryAccess.<Spell>absent(), Spell::text);

        assertThat(response).isEqualTo(ResponseEntity.notFound().build());
        assertThat(response.getBody()).isNull();
    }

    @Test
    void teaserBodyHasNoProtectedFields() {
        assertThat(Arrays.stream(EntryTeaserDto.class.getRecordComponents()).map(RecordComponent::getName))
                .containsExactly("id", "name", "type", "requiredProducts");
        assertThat(Arrays.stream(RequiredProductDto.class.getRecordComponents()).map(RecordComponent::getName))
                .containsExactly("codexId", "publication");
    }
}
