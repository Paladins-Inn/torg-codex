package de.paladinsinn.torg.codex.data.adapter.out.persistence;

import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.data.mapper.SpellEntityMapper;
import de.paladinsinn.torg.codex.data.repository.SpellRepository;
import de.paladinsinn.torg.codex.domain.model.Spell;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence adapter exposing {@link Spell} domain models backed by the
 * {@code Spell} JPA entity via {@link SpellEntityMapper}.
 */
public final class JpaSpellPersistenceAdapter implements CatalogPersistencePort<Spell> {

    private final SpellRepository repository;
    private final SpellEntityMapper mapper;

    public JpaSpellPersistenceAdapter(SpellRepository repository, SpellEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Spell> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Spell> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Spell> findByCosm(String cosm) {
        throw new UnsupportedOperationException("Spells cannot be filtered by cosm.");
    }
}
