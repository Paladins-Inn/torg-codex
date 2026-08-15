package de.paladinsinn.torg.codex.application.port.out;

import de.paladinsinn.torg.codex.domain.model.CatalogProduct;

import java.util.Optional;

/**
 * Driven (outbound) port for retrieving DriveThruRPG product information using
 * framework-independent domain types. Adapters implement this against the concrete
 * DriveThruRPG HTTP client.
 */
public interface DriveThruRpgProductPort {

    /**
     * Retrieves a single product by its DriveThruRPG product id.
     *
     * @param productId the DriveThruRPG product id
     * @return the product, or {@link Optional#empty()} if it does not exist
     */
    Optional<CatalogProduct> findProduct(String productId);
}
