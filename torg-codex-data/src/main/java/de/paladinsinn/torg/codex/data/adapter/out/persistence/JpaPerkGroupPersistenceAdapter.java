package de.paladinsinn.torg.codex.data.adapter.out.persistence;

import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.data.mapper.PerkGroupEntityMapper;
import de.paladinsinn.torg.codex.data.repository.PerkGroupRepository;
import de.paladinsinn.torg.codex.domain.model.PerkGroup;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence adapter exposing {@link PerkGroup} domain models backed by the
 * {@code PerkGroup} JPA entity via {@link PerkGroupEntityMapper}.
 */
public final class JpaPerkGroupPersistenceAdapter implements CatalogPersistencePort<PerkGroup> {

    private final PerkGroupRepository repository;
    private final PerkGroupEntityMapper mapper;

    public JpaPerkGroupPersistenceAdapter(PerkGroupRepository repository, PerkGroupEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<PerkGroup> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<PerkGroup> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<PerkGroup> findByCosm(String cosm) {
        throw new UnsupportedOperationException("PerkGroups cannot be filtered by cosm.");
    }
}
