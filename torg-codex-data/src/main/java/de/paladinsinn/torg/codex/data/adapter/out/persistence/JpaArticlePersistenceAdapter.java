package de.paladinsinn.torg.codex.data.adapter.out.persistence;

import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.data.mapper.ArticleEntityMapper;
import de.paladinsinn.torg.codex.data.repository.ArticleRepository;
import de.paladinsinn.torg.codex.domain.model.Article;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence adapter exposing {@link Article} domain models backed by the
 * {@code Article} JPA entity via {@link ArticleEntityMapper}.
 */
public final class JpaArticlePersistenceAdapter implements CatalogPersistencePort<Article> {

    private final ArticleRepository repository;
    private final ArticleEntityMapper mapper;

    public JpaArticlePersistenceAdapter(ArticleRepository repository, ArticleEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Article> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Article> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Article> findByCosm(String cosm) {
        throw new UnsupportedOperationException("Articles cannot be filtered by cosm.");
    }
}
