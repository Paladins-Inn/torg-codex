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
import de.paladinsinn.drivethru.resource.JsonApiCollectionDocument;
import de.paladinsinn.drivethru.resource.JsonApiDocument;
import de.paladinsinn.drivethru.token.DrivethruToken;
import de.paladinsinn.drivethru.token.NoValidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service layer for the DriveThruRPG vBeta REST and JSON:API endpoints.
 *
 * <p>Wraps the low-level {@link DriveThruRPGClient} and adds caching,
 * session refresh / re-authentication, and JSON:API pagination.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DriveThruRPGService {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int EXCLUDE_ARCHIVED = 0;
    private static final int NO_CHECKSUMS = 0;
    private static final int NO_FILTERS = 0;

    private record OrderProductPage(List<OwnedProduct> products, boolean hasNext) {
    }

    private final DriveThruRPGClient client;

    // -------------------------------------------------------------------------
    // Token
    // -------------------------------------------------------------------------

    /**
     * Retrieves a token for the given API key without an application-wide credential cache.
     *
     * @param apiKey DriveThruRPG Application Key
     * @return a valid {@link DrivethruToken}
     * @throws NoValidTokenException if the token cannot be obtained
     */
    public DrivethruToken getToken(final String apiKey) throws NoValidTokenException {
        return requestToken(apiKey, "token-exchange");
    }

    private DrivethruToken requestToken(final String apiKey, final String operation) throws NoValidTokenException {
        log.atDebug()
                .addKeyValue("operation", operation)
                .addKeyValue("endpoint", "/auth_key")
                .addKeyValue("httpMethod", "POST")
                .log("DriveThruRPG token request started.");
        if (apiKey == null || apiKey.isBlank()) {
            log.atDebug()
                    .addKeyValue("operation", operation)
                    .addKeyValue("reason", "missing-api-key")
                    .log("DriveThruRPG token request skipped: API key is missing or blank.");
            throw new NoValidTokenException();
        }
        final DrivethruToken token;
        try {
            token = client.authenticate(apiKey);
        } catch (final HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            logUpstreamFailure(operation, "/auth_key", null, e);
            throw new NoValidTokenException();
        } catch (final RestClientException e) {
            logUpstreamFailure(operation, "/auth_key", null, e);
            logServiceWarning("DriveThruRPG token request failed.", e);
            throw new AuthenticationServiceException("DriveThruRPG token service unavailable.");
        }
        validateAccessToken(token, operation, "/auth_key");
        token.setApplicationKey(apiKey);
        log.debug("Obtained DriveThruRPG access token.");
        return token;
    }

    private Optional<DrivethruToken> tryRefreshToken(final DrivethruToken currentToken) {
        String refreshToken = currentToken.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            return Optional.empty();
        }
        if (currentToken.isRefreshTokenExpiredAt(Instant.now())) {
            log.atDebug()
                    .addKeyValue("operation", "token-refresh")
                    .addKeyValue("reason", "refresh-token-expired")
                    .log("DriveThruRPG refresh token expired; skipping cookie refresh.");
            return Optional.empty();
        }
        log.atDebug()
                .addKeyValue("operation", "token-refresh")
                .addKeyValue("endpoint", "/auth/refresh")
                .addKeyValue("httpMethod", "POST")
                .log("DriveThruRPG token refresh started.");
        final DrivethruToken refreshed;
        try {
            refreshed = client.refreshToken(refreshToken);
        } catch (final HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            logUpstreamFailure("token-refresh", "/auth/refresh", null, e);
            log.atDebug()
                    .addKeyValue("operation", "token-refresh")
                    .addKeyValue("reason", "refresh-rejected")
                    .log("DriveThruRPG refresh token rejected; falling back to application key exchange.");
            return Optional.empty();
        } catch (final RestClientException e) {
            logUpstreamFailure("token-refresh", "/auth/refresh", null, e);
            logServiceWarning("DriveThruRPG token refresh request failed.", e);
            throw new AuthenticationServiceException("DriveThruRPG token service unavailable.");
        }
        validateAccessToken(refreshed, "token-refresh", "/auth/refresh");
        return Optional.of(refreshed);
    }

    private void validateAccessToken(
            final DrivethruToken token,
            final String operation,
            final String endpoint) {
        boolean accessTokenPresent = token != null && token.getAccessToken() != null
                && !token.getAccessToken().isBlank();
        log.atDebug()
                .addKeyValue("operation", operation)
                .addKeyValue("endpoint", endpoint)
                .addKeyValue("responsePresent", token != null)
                .addKeyValue("accessTokenPresent", accessTokenPresent)
                .addKeyValue("refreshTokenPresent", token != null && token.getRefreshToken() != null
                        && !token.getRefreshToken().isBlank())
                .log("DriveThruRPG token response received.");
        if (!accessTokenPresent) {
            log.warn("DriveThruRPG token response did not contain an access token.");
            throw new AuthenticationServiceException("Invalid DriveThruRPG token response.");
        }
    }

    // -------------------------------------------------------------------------
    // Products
    // -------------------------------------------------------------------------

    /**
     * Retrieves (and caches) data for a single catalog product.
     *
     * @param productId DriveThruRPG product id
     * @return the product, or empty if not found
     */
    @Cacheable("drivethru.product")
    public Optional<Product> getProduct(final String productId) {
        log.trace("Retrieving product. productId={}", productId);
        try {
            final Optional<Product> result = Product.fromDocument(client.getProductDocument(productId));
            log.debug("Retrieved product. productId={}, present={}", productId, result.isPresent());
            return result;
        } catch (final HttpClientErrorException.NotFound e) {
            log.debug("DriveThruRPG product not found. productId={}", productId);
            return Optional.empty();
        }
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
    @SuppressWarnings("unused")
    @Cacheable("drivethru.publisher")
    public Optional<Publisher> getPublisher(final String publisherId) {
        log.trace("Retrieving publisher. publisherId={}", publisherId);
        try {
            final JsonApiDocument<Publisher> document = client.getPublisherDocument(publisherId);
            final Optional<Publisher> result = document != null ? document.getAttributes() : Optional.empty();
            log.debug("Retrieved publisher. publisherId={}, present={}", publisherId, result.isPresent());
            return result;
        } catch (final HttpClientErrorException.NotFound e) {
            log.debug("DriveThruRPG publisher not found. publisherId={}", publisherId);
            return Optional.empty();
        }
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
     * @throws NoValidTokenException if the session token cannot be obtained
     */
    @SuppressWarnings("unused")
    public List<OwnedProduct> getOwnedProducts(
            final String apiKey, final int page, final int pageSize, final int archived)
            throws NoValidTokenException {
        log.trace("Retrieving owned products. page={}, pageSize={}, archived={}",
                page, pageSize, archived);
        final DrivethruToken token = getToken(apiKey);
        return getOwnedProducts(token, page, pageSize, archived);
    }

    /**
     * Retrieves a single page of owned products using a pre-obtained token.
     *
     * @param token     valid DriveThruRPG session token
     * @param page      1-based page number
     * @param pageSize  items per page
     * @param archived  {@code 0} = exclude archived, {@code 1} = include archived
     * @return list of owned products on the requested page
     */
    public List<OwnedProduct> getOwnedProducts(
            final DrivethruToken token, final int page, final int pageSize, final int archived) {
        try {
            return fetchOrderProductPage(token, page, pageSize, archived).products();
        } catch (final HttpClientErrorException.Unauthorized e) {
            try {
                renewToken(token);
                return fetchOrderProductPage(token, page, pageSize, archived).products();
            } catch (final NoValidTokenException | HttpClientErrorException.Unauthorized rejected) {
                if (rejected instanceof NoValidTokenException) {
                    log.atDebug()
                            .addKeyValue("operation", "token-renewal")
                            .addKeyValue("reason", "renewal-rejected")
                            .log("DriveThruRPG token renewal was rejected.");
                } else {
                    logRenewalExhausted(page);
                }
                throw new BadCredentialsException("DriveThruRPG session is no longer valid.");
            }
        }
    }

    /**
     * Retrieves the complete list of owned product ids, iterating
     * through all JSON:API pages automatically.
     *
     * @param token valid DriveThruRPG session token
     * @return ids of all products owned by the token holder
     * @throws NoValidTokenException if no valid token is available
     */
    public List<Integer> getIdsOfOwnedProducts(final DrivethruToken token)
            throws NoValidTokenException {
        final List<Integer> result = new ArrayList<>(DEFAULT_PAGE_SIZE);

        int page = 1;
        boolean renewed = false;
        while (true) {
            final OrderProductPage currentPage;
            try {
                currentPage = fetchOrderProductPage(token, page, DEFAULT_PAGE_SIZE, EXCLUDE_ARCHIVED);
            } catch (final HttpClientErrorException.Unauthorized e) {
                if (renewed) {
                    logRenewalExhausted(page);
                    throw new NoValidTokenException();
                }
                renewToken(token);
                renewed = true;
                continue;
            }
            currentPage.products().stream()
                    .map(OwnedProduct::getId)
                    .filter(id -> id > 0)
                    .forEach(result::add);
            if (!currentPage.hasNext()) {
                break;
            }
            page++;
        }
        log.debug("Collected all owned product ids. total={}", result.size());
        return result;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private OrderProductPage fetchOrderProductPage(
            final DrivethruToken token, final int page, final int pageSize, final int archived) {
        log.atDebug()
                .addKeyValue("operation", "library-fetch")
                .addKeyValue("endpoint", "/order_products")
                .addKeyValue("httpMethod", "GET")
                .addKeyValue("page", page)
                .addKeyValue("pageSize", pageSize)
                .addKeyValue("archived", archived)
                .log("DriveThruRPG library request started.");
        final JsonApiCollectionDocument<OwnedProduct> document;
        try {
            document = client.getOrderProducts(
                    token.getBearerToken(), page, pageSize, archived, NO_CHECKSUMS, NO_FILTERS);
        } catch (final HttpClientErrorException.Unauthorized e) {
            logUpstreamFailure("library-fetch", "/order_products", page, e);
            throw e;
        } catch (final HttpClientErrorException.Forbidden e) {
            logUpstreamFailure("library-fetch", "/order_products", page, e);
            throw new BadCredentialsException("DriveThruRPG library access denied.");
        } catch (final RestClientException e) {
            logUpstreamFailure("library-fetch", "/order_products", page, e);
            logServiceWarning("DriveThruRPG library request failed.", e);
            throw new AuthenticationServiceException("DriveThruRPG library service unavailable.");
        }
        if (document == null || document.getData() == null) {
            log.atDebug()
                    .addKeyValue("operation", "library-fetch")
                    .addKeyValue("page", page)
                    .addKeyValue("reason", "missing-product-array")
                    .log("DriveThruRPG library response is missing.");
            log.warn("DriveThruRPG library response did not contain a JSON:API data array.");
            throw new AuthenticationServiceException("Invalid DriveThruRPG library response.");
        }
        List<OwnedProduct> products = document.getAttributesList();
        if (token.getCustomerId() == null) {
            products.stream()
                    .map(OwnedProduct::getCustomerId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .ifPresent(id -> token.setCustomerId(String.valueOf(id)));
        }
        boolean hasNext = !products.isEmpty()
                && (document.getLinks() != null ? document.getLinks().hasNext() : products.size() >= pageSize);
        log.atDebug()
                .addKeyValue("operation", "library-fetch")
                .addKeyValue("page", page)
                .addKeyValue("productCount", products.size())
                .addKeyValue("hasNext", hasNext)
                .log("DriveThruRPG library response received.");
        return new OrderProductPage(products, hasNext);
    }

    private void renewToken(final DrivethruToken token) throws NoValidTokenException {
        // Prefer the cookie-based /auth/refresh endpoint when an unexpired refresh token exists;
        // otherwise or upon rejection, bypass the token cache and re-exchange the application key.
        DrivethruToken replacement = tryRefreshToken(token).orElse(null);
        if (replacement == null) {
            replacement = requestToken(token.getApplicationKey(), "token-renewal");
        }
        token.setAccessToken(replacement.getAccessToken());
        if (replacement.getRefreshToken() != null && !replacement.getRefreshToken().isBlank()) {
            token.setRefreshToken(replacement.getRefreshToken());
        }
        if (replacement.getRefreshTokenTTL() != null) {
            token.setRefreshTokenTTL(replacement.getRefreshTokenTTL());
        }
    }

    private void logRenewalExhausted(final int page) {
        log.atDebug()
                .addKeyValue("operation", "library-fetch")
                .addKeyValue("page", page)
                .addKeyValue("reason", "renewal-exhausted")
                .log("DriveThruRPG library access still unauthorized after token renewal.");
    }

    private void logServiceWarning(final String message, final RestClientException failure) {
        // Upstream exceptions can contain credentials in their URL or response body; emit only class names and status.
        log.atWarn()
                .addKeyValue("upstream", "drivethrurpg")
                .addKeyValue("statusCode",
                        failure instanceof RestClientResponseException http ? http.getStatusCode().value() : null)
                .addKeyValue("exceptionClass", failure.getClass().getName())
                .addKeyValue("causeClass", failure.getMostSpecificCause().getClass().getName())
                .log(message);
    }

    private void logUpstreamFailure(
            final String operation, final String endpoint, final Integer page, final RestClientException failure) {
        LoggingEventBuilder event = log.atDebug()
                .addKeyValue("upstream", "drivethrurpg")
                .addKeyValue("operation", operation)
                .addKeyValue("endpoint", endpoint)
                .addKeyValue("exceptionClass", failure.getClass().getName())
                .addKeyValue("causeClass", failure.getMostSpecificCause().getClass().getName());
        if (page != null) {
            event = event.addKeyValue("page", page);
        }
        if (failure instanceof RestClientResponseException response) {
            event = event.addKeyValue("statusCode", response.getStatusCode().value())
                    .addKeyValue("responseBodyBytes", response.getResponseBodyAsByteArray().length);
        }
        // Exception messages, stack traces, headers and bodies can contain credentials.
        event.log("DriveThruRPG HTTP request failed.");
    }
}
