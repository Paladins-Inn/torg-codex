package de.paladinsinn.torg.codex.application.service;

import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Framework-independent implementation of the catalog reading use case.
 *
 * @param <T> the entry type
 */
public final class CatalogQueryService<T> implements CatalogQuery<T> {

    private final CatalogPersistencePort<T> persistence;

    public CatalogQueryService(CatalogPersistencePort<T> persistence) {
        this.persistence = persistence;
    }

    @Override
    public List<T> findAll() {
        return persistence.findAll();
    }

    @Override
    public Optional<T> findById(UUID id) {
        return persistence.findById(id);
    }

    @Override
    public List<T> findByCosm(String cosm) {
        return persistence.findByCosm(cosm);
    }
}
