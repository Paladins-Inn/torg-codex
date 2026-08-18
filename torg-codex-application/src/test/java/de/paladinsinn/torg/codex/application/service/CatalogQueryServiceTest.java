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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogQueryServiceTest {

    private final UUID id = UUID.randomUUID();
    private final TestEntry entry = new TestEntry(id, "Aysle");
    private final CatalogQueryService<TestEntry> service =
            new CatalogQueryService<>(new InMemoryCatalogPersistencePort(entry));

    @Test
    void delegatesAllReadsToThePersistencePort() {
        assertThat(service.findAll()).containsExactly(entry);
        assertThat(service.findById(id)).contains(entry);
        assertThat(service.findByCosm("Aysle")).containsExactly(entry);
    }

    private record TestEntry(UUID id, String cosm) {
    }

    private record InMemoryCatalogPersistencePort(TestEntry entry)
                implements CatalogPersistencePort<TestEntry> {

        @Override
            public List<TestEntry> findAll() {
                return List.of(entry);
            }

            @Override
            public Optional<TestEntry> findById(UUID id) {
                return entry.id().equals(id) ? Optional.of(entry) : Optional.empty();
            }

            @Override
            public List<TestEntry> findByCosm(String cosm) {
                return entry.cosm().equals(cosm) ? List.of(entry) : List.of();
            }
        }
}
