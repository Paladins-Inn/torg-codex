package de.paladinsinn.torg.codex.data.application.service;

import de.paladinsinn.torg.codex.data.application.port.out.CatalogPersistencePort;
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

    private static final class InMemoryCatalogPersistencePort
            implements CatalogPersistencePort<TestEntry> {

        private final TestEntry entry;

        private InMemoryCatalogPersistencePort(TestEntry entry) {
            this.entry = entry;
        }

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
