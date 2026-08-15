package de.paladinsinn.torg.codex.data.adapter.out.persistence;

import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.data.mapper.SpellListEntityMapper;
import de.paladinsinn.torg.codex.data.repository.SpellListRepository;
import de.paladinsinn.torg.codex.domain.model.SpellList;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence adapter exposing {@link SpellList} domain models backed by the
 * {@code SpellList} JPA entity via {@link SpellListEntityMapper}.
 */
public final class JpaSpellListPersistenceAdapter implements CatalogPersistencePort<SpellList> {

    private final SpellListRepository repository;
    private final SpellListEntityMapper mapper;

    public JpaSpellListPersistenceAdapter(SpellListRepository repository, SpellListEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<SpellList> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<SpellList> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<SpellList> findByCosm(String cosm) {
        return repository.findByCosm(cosm).stream().map(mapper::toDomain).toList();
    }
}
