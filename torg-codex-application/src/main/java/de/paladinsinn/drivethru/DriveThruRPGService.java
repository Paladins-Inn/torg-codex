/*
 * Copyright (c) 2026.  Roland T. Lichti <rlichti@kaiserpfalz-edv.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * ERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * You may contact me via email rlichti@kaiserpfalz-edv.de or via mail
 *
 * Kaiserpfalz EDV-Service
 * Roland T. Lichti
 * Darmstädter Str. 12
 * 64625 Bensheim
 * GERMANY
 */

package de.paladinsinn.drivethru;

import de.paladinsinn.drivethru.client.DriveThruRPGClient;
import de.paladinsinn.drivethru.products.OwnedProduct;
import de.paladinsinn.drivethru.products.Product;
import de.paladinsinn.drivethru.publishers.Publisher;
import de.paladinsinn.drivethru.resource.DriveThruMultiMessage;
import de.paladinsinn.drivethru.token.DrivethruToken;
import de.paladinsinn.drivethru.token.NoValidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for the DriveThruRPG REST API.
 *
 * <p>Wraps the low-level {@link DriveThruRPGClient} and adds caching,
 * token management, and pagination helpers.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DriveThruRPGService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int DEFAULT_PAGE_SIZE = 1000;

    private final DriveThruRPGClient client;

    // -------------------------------------------------------------------------
    // Token
    // -------------------------------------------------------------------------

    /**
     * Retrieves (and caches) a bearer token for the given API key.
     *
     * @param apiKey DriveThruRPG API key
     * @return a valid {@link DrivethruToken}
     * @throws NoValidTokenException if the token cannot be obtained
     */
    @Cacheable("drivethru.token")
    public DrivethruToken getToken(final String apiKey) throws NoValidTokenException {
        log.trace("Loading access token. apiKey='{}'", apiKey);

        final LinkedHashMap<String, String> response =
            client.getToken("Bearer " + apiKey)
                    .getData()
                    .orElseThrow(() -> new NoValidTokenException(apiKey))
            ;

        final LocalDateTime serverTime = parse(response.get("server_time"));
        final LocalDateTime expireTime = parse(response.get("expires"));
        final LocalDateTime localTime  = LocalDateTime.now();
        final long expiresSeconds      = Duration.between(serverTime, expireTime).getSeconds();

        final DrivethruToken token = new DrivethruToken();
        token.setAccessToken(response.get("access_token"));
        token.setCustomerId(response.get("customers_id"));
        token.setExpireTime(expireTime);
        token.setServerTime(serverTime);
        token.setLocalTime(localTime);
        token.setExpires(expiresSeconds);

        log.debug("Obtained DriveThruRPG token. token={}", token);
        return token;
    }

    // -------------------------------------------------------------------------
    // Products
    // -------------------------------------------------------------------------

    /**
     * Retrieves (and caches) data for a single product.
     *
     * @param productId DriveThruRPG product id
     * @return the product, or empty if not found
     */
    @Cacheable("drivethru.product")
    public Optional<Product> getProduct(final String productId) {
        log.trace("Retrieving product. productId={}", productId);
        final Optional<Product> result = client.getProduct(productId).getData();
        log.debug("Retrieved product. product={}", result);
        return result;
    }

    // -------------------------------------------------------------------------
    // Publishers
    // -------------------------------------------------------------------------

    /**
     * Retrieves (and caches) data for a single publisher.
     *
     * @param publisherId DriveThruRPG publisher id
     * @return the publisher, or empty if not found
     */
    @Cacheable("drivethru.publisher")
    public Optional<Publisher> getPublisher(final String publisherId) {
        log.trace("Retrieving publisher. publisherId={}", publisherId);
        final Optional<Publisher> result = client.getPublisher(publisherId).getData();
        log.debug("Retrieved publisher. publisher={}", result);
        return result;
    }

    // -------------------------------------------------------------------------
    // Owned products
    // -------------------------------------------------------------------------

    /**
     * Retrieves a single page of owned products for the given API key.
     *
     * @param apiKey    DriveThruRPG API key
     * @param page      1-based page number
     * @param pageSize  items per page
     * @param archived  {@code 0} = exclude archived, {@code 1} = include archived
     * @return list of owned products on the requested page
     * @throws NoValidTokenException if the bearer token cannot be obtained
     */
    public List<OwnedProduct> getOwnedProducts(
            final String apiKey, final int page, final int pageSize, final int archived)
            throws NoValidTokenException {
        log.trace("Retrieving owned products. apiKey={}, page={}, pageSize={}, archived={}",
                apiKey, page, pageSize, archived);
        final DrivethruToken token = getToken(apiKey);
        return getOwnedProducts(token, page, pageSize, archived);
    }

    /**
     * Retrieves a single page of owned products using a pre-obtained token.
     *
     * @param token     valid DriveThruRPG bearer token
     * @param page      1-based page number
     * @param pageSize  items per page
     * @param archived  {@code 0} = exclude archived, {@code 1} = include archived
     * @return list of owned products on the requested page
     */
    public List<OwnedProduct> getOwnedProducts(
            final DrivethruToken token, final int page, final int pageSize, final int archived) {
        log.trace("Retrieving owned products. customerId={}, page={}, pageSize={}, archived={}",
                token.getCustomerId(), page, pageSize, archived);
        final DriveThruMultiMessage<OwnedProduct> result =
                client.getOwnedProducts(token.getBearerToken(), token.getCustomerId(),
                        page, pageSize, archived);
        log.debug("Retrieved owned products. count={}", result.getData().size());
        return result.getData();
    }

    /**
     * Retrieves (and caches) the complete list of owned product ids, iterating
     * through all pages automatically.
     *
     * @param token valid DriveThruRPG bearer token
     * @return ids of all products owned by the token holder
     * @throws NoValidTokenException if no valid token is available
     */
    @Cacheable("drivethru.owned_product_ids")
    public List<Integer> getIdsOfOwnedProducts(final DrivethruToken token)
            throws NoValidTokenException {
        final List<Integer> result = new ArrayList<>(DEFAULT_PAGE_SIZE);

        int page = 1;
        DriveThruMultiMessage<OwnedProduct> data;
        do {
            data = client.getOwnedProducts(
                    token.getBearerToken(), token.getCustomerId(),
                    page, DEFAULT_PAGE_SIZE, 0);

            final List<Integer> ids = data.getData().stream()
                    .map(OwnedProduct::getId)
                    .toList();

            if (!result.addAll(ids)) {
                log.error("Failed to add owned product ids. customerId={}, page={}, current={}, failed={}",
                        token.getCustomerId(), page, result.size(), ids.size());
            }

            log.trace("Accumulated owned product ids. customerId={}, page={}, total={}",
                    token.getCustomerId(), page, result.size());
            page++;
        } while (data.getData().size() >= DEFAULT_PAGE_SIZE);

        log.info("Collected all owned product ids. customerId={}, total={}",
                token.getCustomerId(), result.size());
        return result;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private LocalDateTime parse(final String raw) {
        return LocalDateTime.parse(raw, DATE_FORMATTER);
    }
}

