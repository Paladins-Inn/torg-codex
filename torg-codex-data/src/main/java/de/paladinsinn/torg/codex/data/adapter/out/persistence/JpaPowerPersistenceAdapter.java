package de.paladinsinn.torg.codex.data.adapter.out.persistence;

import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.data.mapper.PowerEntityMapper;
import de.paladinsinn.torg.codex.data.repository.PowerRepository;
import de.paladinsinn.torg.codex.domain.model.Power;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence adapter exposing {@link Power} domain models backed by the
 * {@code Power} JPA entity via {@link PowerEntityMapper}.
 */
public final class JpaPowerPersistenceAdapter implements CatalogPersistencePort<Power> {

    private final PowerRepository repository;
    private final PowerEntityMapper mapper;

    public JpaPowerPersistenceAdapter(PowerRepository repository, PowerEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Power> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Power> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Power> findByCosm(String cosm) {
        throw new UnsupportedOperationException("Powers cannot be filtered by cosm.");
    }
}
