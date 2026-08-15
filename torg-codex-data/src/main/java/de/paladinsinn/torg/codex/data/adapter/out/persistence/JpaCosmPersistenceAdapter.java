package de.paladinsinn.torg.codex.data.adapter.out.persistence;

import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.data.mapper.CosmEntityMapper;
import de.paladinsinn.torg.codex.data.repository.CosmRepository;
import de.paladinsinn.torg.codex.domain.model.Cosm;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence adapter exposing {@link Cosm} domain models backed by the
 * {@code Cosm} JPA entity via {@link CosmEntityMapper}.
 */
public final class JpaCosmPersistenceAdapter implements CatalogPersistencePort<Cosm> {

    private final CosmRepository repository;
    private final CosmEntityMapper mapper;

    public JpaCosmPersistenceAdapter(CosmRepository repository, CosmEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Cosm> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Cosm> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Cosm> findByCosm(String cosm) {
        throw new UnsupportedOperationException("Cosms cannot be filtered by cosm.");
    }
}
