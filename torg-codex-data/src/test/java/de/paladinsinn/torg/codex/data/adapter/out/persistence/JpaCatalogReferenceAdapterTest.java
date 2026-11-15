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

package de.paladinsinn.torg.codex.data.adapter.out.persistence;

import de.paladinsinn.torg.codex.application.port.out.CatalogEntryReference;
import de.paladinsinn.torg.codex.data.mapper.CatalogReferenceEntityMapperImpl;
import de.paladinsinn.torg.codex.data.model.Cosm;
import de.paladinsinn.torg.codex.data.repository.CosmRepository;
import de.paladinsinn.torg.codex.data.repository.PublicationRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JpaCatalogReferenceAdapterTest {

    private final CosmRepository cosms = mock(CosmRepository.class);
    private final JpaCatalogReferenceAdapter adapter = new JpaCatalogReferenceAdapter(
            cosms, mock(PublicationRepository.class), new CatalogReferenceEntityMapperImpl());

    @Test
    void cosmReferenceCarriesIdNameAndProducts() {
        UUID id = UUID.randomUUID();
        Cosm cosm = mock(Cosm.class);
        when(cosm.getId()).thenReturn(id);
        when(cosm.getName()).thenReturn("Aysle");
        when(cosm.getProducts()).thenReturn(Set.of("sourcebook-aysle"));
        when(cosms.findBySlug("aysle")).thenReturn(Optional.of(cosm));

        assertThat(adapter.findCosmBySlug("aysle"))
                .contains(new CatalogEntryReference(id, "Aysle", Set.of("sourcebook-aysle")));
    }

    @Test
    void unknownSlugIsEmpty() {
        when(cosms.findBySlug("nowhere")).thenReturn(Optional.empty());

        assertThat(adapter.findCosmBySlug("nowhere")).isEmpty();
    }
}
