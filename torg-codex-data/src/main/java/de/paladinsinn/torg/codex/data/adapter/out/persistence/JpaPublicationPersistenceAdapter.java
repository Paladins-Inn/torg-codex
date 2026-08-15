package de.paladinsinn.torg.codex.data.adapter.out.persistence;

import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.data.mapper.PublicationEntityMapper;
import de.paladinsinn.torg.codex.data.repository.PublicationRepository;
import de.paladinsinn.torg.codex.domain.model.Publication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence adapter exposing {@link Publication} domain models backed by the
 * {@code Publication} JPA entity via {@link PublicationEntityMapper}.
 */
public final class JpaPublicationPersistenceAdapter implements CatalogPersistencePort<Publication> {

    private final PublicationRepository repository;
    private final PublicationEntityMapper mapper;

    public JpaPublicationPersistenceAdapter(PublicationRepository repository, PublicationEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Publication> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Publication> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Publication> findByCosm(String cosm) {
        throw new UnsupportedOperationException("Publications cannot be filtered by cosm.");
    }
}
