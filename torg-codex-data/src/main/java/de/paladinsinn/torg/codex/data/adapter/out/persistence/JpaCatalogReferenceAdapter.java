package de.paladinsinn.torg.codex.data.adapter.out.persistence;

import de.paladinsinn.torg.codex.data.application.port.in.CatalogReference;
import de.paladinsinn.torg.codex.data.application.port.in.CatalogPublicationReference;
import de.paladinsinn.torg.codex.data.application.port.out.CatalogReferencePersistencePort;
import de.paladinsinn.torg.codex.data.model.Cosm;
import de.paladinsinn.torg.codex.data.model.Publication;
import de.paladinsinn.torg.codex.data.repository.CosmRepository;
import de.paladinsinn.torg.codex.data.repository.PublicationRepository;

import java.util.List;
import java.util.Optional;

/**
 * Secondary adapter that resolves references through Spring Data JPA repositories.
 */
public final class JpaCatalogReferenceAdapter implements CatalogReferencePersistencePort {

    private final CosmRepository cosmRepository;
    private final PublicationRepository publicationRepository;

    public JpaCatalogReferenceAdapter(
            CosmRepository cosmRepository,
            PublicationRepository publicationRepository) {
        this.cosmRepository = cosmRepository;
        this.publicationRepository = publicationRepository;
    }

    @Override
    public Optional<CatalogReference> findCosmByName(String name) {
        return cosmRepository.findByNameIgnoreCase(name).map(this::toReference);
    }

    @Override
    public Optional<CatalogReference> findPublicationByCodexId(String codexId) {
        return publicationRepository.findByCodexId(codexId).map(this::toReference);
    }

    @Override
    public List<CatalogPublicationReference> findPublicationsByProductId(int productId) {
        return publicationRepository.findByProductId(productId).stream()
                .map(this::toPublicationReference)
                .toList();
    }

    private CatalogReference toReference(Cosm cosm) {
        return new CatalogReference(cosm.getId(), cosm.getName());
    }

    private CatalogReference toReference(Publication publication) {
        return new CatalogReference(publication.getId(), publication.getName());
    }

    private CatalogPublicationReference toPublicationReference(Publication publication) {
        return new CatalogPublicationReference(
                publication.getId(), publication.getName(), publication.getCodexId());
    }
}
