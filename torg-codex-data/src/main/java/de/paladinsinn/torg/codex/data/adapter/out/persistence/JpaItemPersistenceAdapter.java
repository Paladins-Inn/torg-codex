package de.paladinsinn.torg.codex.data.adapter.out.persistence;

import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.data.mapper.ItemEntityMapper;
import de.paladinsinn.torg.codex.data.repository.ItemRepository;
import de.paladinsinn.torg.codex.domain.model.Item;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence adapter exposing {@link Item} domain models backed by the
 * {@code Item} JPA entity via {@link ItemEntityMapper}.
 */
public final class JpaItemPersistenceAdapter implements CatalogPersistencePort<Item> {

    private final ItemRepository repository;
    private final ItemEntityMapper mapper;

    public JpaItemPersistenceAdapter(ItemRepository repository, ItemEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Item> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Item> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Item> findByCosm(String cosm) {
        return repository.findByCosm(cosm).stream().map(mapper::toDomain).toList();
    }
}
