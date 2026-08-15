package de.paladinsinn.torg.codex.data.adapter.out.persistence;

import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.data.mapper.MiracleListEntityMapper;
import de.paladinsinn.torg.codex.data.repository.MiracleListRepository;
import de.paladinsinn.torg.codex.domain.model.MiracleList;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence adapter exposing {@link MiracleList} domain models backed by the
 * {@code MiracleList} JPA entity via {@link MiracleListEntityMapper}.
 */
public final class JpaMiracleListPersistenceAdapter implements CatalogPersistencePort<MiracleList> {

    private final MiracleListRepository repository;
    private final MiracleListEntityMapper mapper;

    public JpaMiracleListPersistenceAdapter(MiracleListRepository repository, MiracleListEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<MiracleList> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<MiracleList> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<MiracleList> findByCosm(String cosm) {
        return repository.findByCosm(cosm).stream().map(mapper::toDomain).toList();
    }
}
