package de.paladinsinn.torg.codex.data.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Driven port for retrieving codex entries from persistence.
 *
 * @param <T> the entry type
 */
public interface CatalogPersistencePort<T> {

    List<T> findAll();

    Optional<T> findById(UUID id);

    List<T> findByCosm(String cosm);
}
