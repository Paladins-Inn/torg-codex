package de.paladinsinn.torg.codex.application.port.out;

import de.paladinsinn.torg.codex.application.port.in.CatalogReference;
import de.paladinsinn.torg.codex.application.port.in.CatalogPublicationReference;

import java.util.List;
import java.util.Optional;

/**
 * Driven port for resolving codex references from persistence.
 */
public interface CatalogReferencePersistencePort {

    Optional<CatalogReference> findCosmByName(String name);

    Optional<CatalogReference> findPublicationByCodexId(String codexId);

    List<CatalogPublicationReference> findPublicationsByProductId(int productId);
}
