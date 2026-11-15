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

package de.paladinsinn.torg.codex.application.port.out;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogPersistencePortTest {

    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();
    private final RecordingPort port = new RecordingPort(Map.of(first, "first", second, "second"));

    @Test
    void defaultBatchReadReturnsOnlyRequestedExistingEntries() {
        assertThat(port.findAllById(List.of(second, UUID.randomUUID(), first))).containsExactly("second", "first");
    }

    @Test
    void defaultBatchReadLooksUpEveryDistinctIdOnce() {
        port.findAllById(List.of(first, first, second));

        assertThat(port.lookups).containsExactly(first, second);
    }

    @Test
    void defaultBatchReadOfNoIdsReadsNothing() {
        assertThat(port.findAllById(List.of())).isEmpty();
        assertThat(port.findAllById(null)).isEmpty();
        assertThat(port.lookups).isEmpty();
    }

    /** A port with entries of a type without id accessor, implementing only the mandatory reads. */
    static final class RecordingPort implements CatalogPersistencePort<String> {
        private final Map<UUID, String> entries;
        final List<UUID> lookups = new ArrayList<>();

        RecordingPort(Map<UUID, String> entries) {
            this.entries = entries;
        }

        @Override
        public List<String> findAll() {
            return List.copyOf(entries.values());
        }

        @Override
        public Optional<String> findById(UUID id) {
            lookups.add(id);
            return Optional.ofNullable(entries.get(id));
        }

        @Override
        public List<String> findByCosm(String cosm) {
            return List.of();
        }
    }
}
