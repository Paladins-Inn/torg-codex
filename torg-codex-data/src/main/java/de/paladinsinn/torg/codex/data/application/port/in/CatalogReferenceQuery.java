package de.paladinsinn.torg.codex.data.application.port.in;

import java.util.List;
import java.util.Optional;

/**
 * Driving port for resolving lightweight references used by codex use cases.
 */
public interface CatalogReferenceQuery {

    Optional<CatalogReference> findCosmByName(String name);

    Optional<CatalogReference> findPublicationByCodexId(String codexId);

    List<CatalogPublicationReference> findPublicationsByProductId(int productId);
}
