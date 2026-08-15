package de.paladinsinn.torg.codex.data.adapter.out.persistence;

import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.data.mapper.TagEntityMapper;
import de.paladinsinn.torg.codex.data.repository.TagRepository;
import de.paladinsinn.torg.codex.domain.model.Tag;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence adapter exposing {@link Tag} domain models backed by the
 * {@code Tag} JPA entity via {@link TagEntityMapper}.
 */
public final class JpaTagPersistenceAdapter implements CatalogPersistencePort<Tag> {

    private final TagRepository repository;
    private final TagEntityMapper mapper;

    public JpaTagPersistenceAdapter(TagRepository repository, TagEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Tag> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Tag> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Tag> findByCosm(String cosm) {
        throw new UnsupportedOperationException("Tags cannot be filtered by cosm.");
    }
}
