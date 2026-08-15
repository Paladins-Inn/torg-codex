package de.paladinsinn.torg.codex.data.adapter.out.persistence;

import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Secondary adapter that translates the catalog persistence port to Spring Data JPA.
 *
 * @param <T> the JPA-backed entry type
 */
public final class JpaCatalogPersistenceAdapter<T> implements CatalogPersistencePort<T> {

    private final JpaRepository<T, UUID> repository;
    private final Function<String, List<T>> cosmFinder;

    public JpaCatalogPersistenceAdapter(
            JpaRepository<T, UUID> repository,
            Function<String, List<T>> cosmFinder) {
        this.repository = repository;
        this.cosmFinder = cosmFinder;
    }

    @Override
    public List<T> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<T> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<T> findByCosm(String cosm) {
        if (cosmFinder == null) {
            throw new UnsupportedOperationException("This catalog entry type cannot be filtered by cosm.");
        }
        return cosmFinder.apply(cosm);
    }
}
