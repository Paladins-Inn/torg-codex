package de.paladinsinn.torg.codex.data.adapter.out.http;

import de.paladinsinn.drivethru.DriveThruRPGService;
import de.paladinsinn.drivethru.products.Product;
import de.paladinsinn.torg.codex.application.port.out.DriveThruRpgProductPort;
import de.paladinsinn.torg.codex.domain.model.CatalogProduct;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * Outbound HTTP adapter implementing {@link DriveThruRpgProductPort} by delegating to the
 * existing {@link DriveThruRPGService} (which wraps the low-level DriveThruRPG REST client and
 * its caching/token handling), mapping the wire DTO to the framework-independent
 * {@link CatalogProduct} domain type.
 */
@RequiredArgsConstructor
public class DriveThruRpgProductAdapter implements DriveThruRpgProductPort {

    private final DriveThruRPGService service;

    @Override
    public Optional<CatalogProduct> findProduct(final String productId) {
        return service.getProduct(productId).map(this::toDomain);
    }

    private CatalogProduct toDomain(final Product product) {
        return new CatalogProduct(
                product.getProductsId(),
                product.getProductsName(),
                product.getCoverURL());
    }
}
