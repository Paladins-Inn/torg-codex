package de.paladinsinn.torg.codex.application.service;

import de.paladinsinn.torg.codex.application.port.in.CatalogReference;
import de.paladinsinn.torg.codex.application.port.in.CatalogPublicationReference;
import de.paladinsinn.torg.codex.application.port.in.CatalogReferenceQuery;
import de.paladinsinn.torg.codex.application.port.out.CatalogReferencePersistencePort;

import java.util.List;
import java.util.Optional;

/**
 * Framework-independent implementation of the catalog reference use case.
 */
public final class CatalogReferenceQueryService implements CatalogReferenceQuery {

    private final CatalogReferencePersistencePort persistence;

    public CatalogReferenceQueryService(CatalogReferencePersistencePort persistence) {
        this.persistence = persistence;
    }

    @Override
    public Optional<CatalogReference> findCosmByName(String name) {
        return persistence.findCosmByName(name);
    }

    @Override
    public Optional<CatalogReference> findPublicationByCodexId(String codexId) {
        return persistence.findPublicationByCodexId(codexId);
    }

    @Override
    public List<CatalogPublicationReference> findPublicationsByProductId(int productId) {
        return persistence.findPublicationsByProductId(productId);
    }
}
