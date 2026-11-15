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

import de.paladinsinn.torg.codex.data.mapper.MiracleEntityMapper;
import de.paladinsinn.torg.codex.data.mapper.PowerEntityMapper;
import de.paladinsinn.torg.codex.data.mapper.SpellEntityMapper;
import de.paladinsinn.torg.codex.data.repository.MiracleRepository;
import de.paladinsinn.torg.codex.data.repository.PowerRepository;
import de.paladinsinn.torg.codex.data.repository.SpellRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the batched reads of the persistence adapters backing embedded-reference filtering
 * (feature 005, ADR-019): one repository call for the requested ids only.
 */
@SuppressWarnings("unchecked")
class BatchPersistenceAdapterTest {

    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();

    @Test
    void spellAdapterReadsRequestedIdsInOneBatch() {
        SpellRepository repository = mock(SpellRepository.class);
        SpellEntityMapper mapper = mock(SpellEntityMapper.class);
        var entity = new de.paladinsinn.torg.codex.data.model.Spell();
        var domain = mock(de.paladinsinn.torg.codex.domain.model.Spell.class);
        when(repository.findAllById(any())).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        var result = new JpaSpellPersistenceAdapter(repository, mapper).findAllById(List.of(first, second, first));

        assertThat(result).containsExactly(domain);
        assertThat(requested(repository)).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void miracleAdapterReadsRequestedIdsInOneBatch() {
        MiracleRepository repository = mock(MiracleRepository.class);
        MiracleEntityMapper mapper = mock(MiracleEntityMapper.class);
        var entity = new de.paladinsinn.torg.codex.data.model.Miracle();
        var domain = mock(de.paladinsinn.torg.codex.domain.model.Miracle.class);
        when(repository.findAllById(any())).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        var result = new JpaMiraclePersistenceAdapter(repository, mapper).findAllById(List.of(first, second));

        assertThat(result).containsExactly(domain);
        assertThat(requested(repository)).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void powerAdapterReadsRequestedIdsInOneBatch() {
        PowerRepository repository = mock(PowerRepository.class);
        PowerEntityMapper mapper = mock(PowerEntityMapper.class);
        var entity = new de.paladinsinn.torg.codex.data.model.Power();
        var domain = mock(de.paladinsinn.torg.codex.domain.model.Power.class);
        when(repository.findAllById(any())).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        var result = new JpaPowerPersistenceAdapter(repository, mapper).findAllById(List.of(first));

        assertThat(result).containsExactly(domain);
        assertThat(requested(repository)).containsExactly(first);
    }

    @Test
    void emptyRequestsDoNotQuery() {
        SpellRepository repository = mock(SpellRepository.class);

        assertThat(new JpaSpellPersistenceAdapter(repository, mock(SpellEntityMapper.class)).findAllById(List.of())).isEmpty();
        verify(repository, never()).findAllById(any());
    }

    private static List<UUID> requested(org.springframework.data.jpa.repository.JpaRepository<?, UUID> repository) {
        ArgumentCaptor<Iterable<UUID>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(repository, times(1)).findAllById(captor.capture());
        verify(repository, never()).findById(any());
        verify(repository, never()).findAll();
        List<UUID> ids = new java.util.ArrayList<>();
        captor.getValue().forEach(ids::add);
        return ids;
    }
}
