package de.paladinsinn.torg.codex.data.adapter.out.persistence;

import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.data.mapper.PowerListEntityMapper;
import de.paladinsinn.torg.codex.data.repository.PowerListRepository;
import de.paladinsinn.torg.codex.domain.model.PowerList;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence adapter exposing {@link PowerList} domain models backed by the
 * {@code PowerList} JPA entity via {@link PowerListEntityMapper}.
 */
public final class JpaPowerListPersistenceAdapter implements CatalogPersistencePort<PowerList> {

    private final PowerListRepository repository;
    private final PowerListEntityMapper mapper;

    public JpaPowerListPersistenceAdapter(PowerListRepository repository, PowerListEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<PowerList> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<PowerList> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<PowerList> findByCosm(String cosm) {
        return repository.findByCosm(cosm).stream().map(mapper::toDomain).toList();
    }
}
