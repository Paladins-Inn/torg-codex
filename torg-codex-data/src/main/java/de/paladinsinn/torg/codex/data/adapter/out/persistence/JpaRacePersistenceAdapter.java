package de.paladinsinn.torg.codex.data.adapter.out.persistence;

import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.data.mapper.RaceEntityMapper;
import de.paladinsinn.torg.codex.data.repository.RaceRepository;
import de.paladinsinn.torg.codex.domain.model.Race;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence adapter exposing {@link Race} domain models backed by the
 * {@code Race} JPA entity via {@link RaceEntityMapper}.
 */
public final class JpaRacePersistenceAdapter implements CatalogPersistencePort<Race> {

    private final RaceRepository repository;
    private final RaceEntityMapper mapper;

    public JpaRacePersistenceAdapter(RaceRepository repository, RaceEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Race> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Race> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Race> findByCosm(String cosm) {
        throw new UnsupportedOperationException("Races cannot be filtered by cosm.");
    }
}
