package de.paladinsinn.torg.codex.data.adapter.out.persistence;

import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.data.mapper.MiracleEntityMapper;
import de.paladinsinn.torg.codex.data.repository.MiracleRepository;
import de.paladinsinn.torg.codex.domain.model.Miracle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence adapter exposing {@link Miracle} domain models backed by the
 * {@code Miracle} JPA entity via {@link MiracleEntityMapper}.
 */
public final class JpaMiraclePersistenceAdapter implements CatalogPersistencePort<Miracle> {

    private final MiracleRepository repository;
    private final MiracleEntityMapper mapper;

    public JpaMiraclePersistenceAdapter(MiracleRepository repository, MiracleEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Miracle> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Miracle> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Miracle> findByCosm(String cosm) {
        throw new UnsupportedOperationException("Miracles cannot be filtered by cosm.");
    }
}
