package de.paladinsinn.torg.codex.api.security;

import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.data.model.TorgEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Primary-adapter decorator that applies the request's access policy to query results.
 *
 * @param <T> the queried codex entity type
 */
public final class CensoringCatalogQuery<T extends TorgEntity> implements CatalogQuery<T> {

    private final CatalogQuery<T> delegate;
    private final CurrentUserCensorFactory censorFactory;

    public CensoringCatalogQuery(CatalogQuery<T> delegate, CurrentUserCensorFactory censorFactory) {
        this.delegate = delegate;
        this.censorFactory = censorFactory;
    }

    @Override
    public List<T> findAll() {
        return censor(delegate.findAll());
    }

    @Override
    public Optional<T> findById(UUID id) {
        return delegate.findById(id).map(this::censor);
    }

    @Override
    public List<T> findByCosm(String cosm) {
        return censor(delegate.findByCosm(cosm));
    }

    private List<T> censor(List<T> entries) {
        final var censor = censorFactory.create();
        entries.forEach(entry -> entry.withCensor(censor));
        return entries;
    }

    private T censor(T entry) {
        return entry.withCensor(censorFactory.create());
    }
}
