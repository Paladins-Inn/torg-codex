package de.paladinsinn.torg.codex.application.port.in;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Driving port for reading codex entries.
 *
 * @param <T> the entry type
 */
public interface CatalogQuery<T> {

    List<T> findAll();

    Optional<T> findById(UUID id);

    List<T> findByCosm(String cosm);
}
