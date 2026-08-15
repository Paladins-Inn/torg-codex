package de.paladinsinn.torg.codex.data.adapter.out.persistence;

import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.data.mapper.PerkEntityMapper;
import de.paladinsinn.torg.codex.data.repository.PerkRepository;
import de.paladinsinn.torg.codex.domain.model.Perk;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence adapter exposing {@link Perk} domain models backed by the
 * {@code Perk} JPA entity via {@link PerkEntityMapper}.
 */
public final class JpaPerkPersistenceAdapter implements CatalogPersistencePort<Perk> {

    private final PerkRepository repository;
    private final PerkEntityMapper mapper;

    public JpaPerkPersistenceAdapter(PerkRepository repository, PerkEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Perk> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Perk> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Perk> findByCosm(String cosm) {
        return repository.findByCosm(cosm).stream().map(mapper::toDomain).toList();
    }
}
