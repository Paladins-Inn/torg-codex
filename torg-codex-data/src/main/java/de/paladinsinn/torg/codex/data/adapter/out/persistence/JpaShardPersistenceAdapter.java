package de.paladinsinn.torg.codex.data.adapter.out.persistence;

import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.data.mapper.ShardEntityMapper;
import de.paladinsinn.torg.codex.data.repository.ShardRepository;
import de.paladinsinn.torg.codex.domain.model.Shard;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence adapter exposing {@link Shard} domain models backed by the
 * {@code Shard} JPA entity via {@link ShardEntityMapper}.
 */
public final class JpaShardPersistenceAdapter implements CatalogPersistencePort<Shard> {

    private final ShardRepository repository;
    private final ShardEntityMapper mapper;

    public JpaShardPersistenceAdapter(ShardRepository repository, ShardEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Shard> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Shard> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Shard> findByCosm(String cosm) {
        return repository.findByCosm(cosm).stream().map(mapper::toDomain).toList();
    }
}
