package de.paladinsinn.torg.codex.data.adapter.out.persistence;

import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.data.mapper.ThreatEntityMapper;
import de.paladinsinn.torg.codex.data.repository.ThreatRepository;
import de.paladinsinn.torg.codex.domain.model.Threat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence adapter exposing {@link Threat} domain models backed by the
 * {@code Threat} JPA entity via {@link ThreatEntityMapper}.
 */
public final class JpaThreatPersistenceAdapter implements CatalogPersistencePort<Threat> {

    private final ThreatRepository repository;
    private final ThreatEntityMapper mapper;

    public JpaThreatPersistenceAdapter(ThreatRepository repository, ThreatEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Threat> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Threat> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Threat> findByCosm(String cosm) {
        return repository.findByCosm(cosm).stream().map(mapper::toDomain).toList();
    }
}
