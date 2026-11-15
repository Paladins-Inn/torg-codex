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

package de.paladinsinn.drivethru.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import de.paladinsinn.drivethru.products.OwnedProduct;
import de.paladinsinn.drivethru.products.Product;
import de.paladinsinn.drivethru.publishers.Publisher;
import de.paladinsinn.drivethru.resource.DriveThruMessage;
import de.paladinsinn.drivethru.resource.DriveThruMultiMessage;
import de.paladinsinn.drivethru.resource.JsonApiCollectionDocument;
import de.paladinsinn.drivethru.resource.JsonApiDocument;
import de.paladinsinn.drivethru.token.DrivethruToken;

/**
 * Declarative HTTP interface for the DriveThruRPG vBeta REST and JSON:API endpoints.
 *
 * <p>Backed by a Spring {@code RestClient} via {@code HttpServiceProxyFactory}.
 * No Feign dependency required.</p>
 */
@HttpExchange
public interface DriveThruRPGClient {

    /** JSON:API media type served by DriveThruRPG vBeta resource endpoints. */
    String JSON_API_MEDIA_TYPE = "application/vnd.api+json";

    /**
     * Compatibility adapter for callers expecting the former token envelope.
     *
     * @param authorization {@code "Bearer <apiKey>"}
     * @return token response with {@code access_token}; vBeta supplies no customer id or expiry timestamps
     */
    default DriveThruMessage<LinkedHashMap<String, String>> getToken(final String authorization) {
        String key = authorization.startsWith("Bearer ") ? authorization.substring("Bearer ".length()) : authorization;
        DrivethruToken token = authenticate(key);
        LinkedHashMap<String, String> data = new LinkedHashMap<>();
        data.put("access_token", token.getAccessToken());
        DriveThruMessage<LinkedHashMap<String, String>> response = new DriveThruMessage<>();
        response.setMessage(data);
        return response;
    }

    /**
     * Exchanges an Application Key for a DriveThruRPG vBeta session token ({@code AuthTokenResponse}).
     *
     * @param applicationKey the user's Application Key
     * @param body empty JSON object matching the OpenAPI {@code application/json} request schema
     * @return the access token, refresh token, and refresh token expiry timestamp
     */
    @PostExchange(
            value = "/auth_key",
            accept = MediaType.APPLICATION_JSON_VALUE,
            contentType = MediaType.APPLICATION_JSON_VALUE)
    DrivethruToken authenticate(
            @RequestParam("applicationKey") String applicationKey,
            @RequestBody Map<String, Object> body);

    /**
     * Exchanges an Application Key for a DriveThruRPG vBeta session token.
     *
     * @param applicationKey the user's Application Key
     * @return the access token, refresh token, and refresh token expiry timestamp
     */
    default DrivethruToken authenticate(final String applicationKey) {
        return authenticate(applicationKey, Map.of());
    }

    /**
     * Exchanges a {@code refreshToken} cookie for a renewed DriveThruRPG vBeta session token.
     *
     * @param refreshCookie {@code "refreshToken=<token>"} cookie header value
     * @param body empty JSON object payload
     * @return renewed access token, refresh token, and refresh token expiry timestamp
     */
    @PostExchange(
            value = "/auth/refresh",
            accept = MediaType.APPLICATION_JSON_VALUE,
            contentType = MediaType.APPLICATION_JSON_VALUE)
    DrivethruToken refreshSession(
            @RequestHeader(HttpHeaders.COOKIE) String refreshCookie,
            @RequestBody Map<String, Object> body);

    /**
     * Exchanges a refresh token string for a renewed DriveThruRPG vBeta session token.
     *
     * @param refreshToken raw refresh token returned by {@code /auth_key} or {@code /auth/refresh}
     * @return renewed access token, refresh token, and refresh token expiry timestamp
     */
    default DrivethruToken refreshToken(final String refreshToken) {
        return refreshSession("refreshToken=" + refreshToken, Map.of());
    }

    /**
     * Compatibility adapter for callers expecting the former product envelope.
     *
     * @param bearerToken  {@code "Bearer <accessToken>"}
     * @param customerId   unused legacy customer id; vBeta identifies the library by token
     * @param page         1-based page number
     * @param pageSize     number of items per page
     * @param archived     {@code 0} = exclude archived, {@code 1} = include archived
     * @return list of owned products
     */
    default DriveThruMultiMessage<OwnedProduct> getOwnedProducts(
            final String bearerToken, final String customerId,
            final int page, final int pageSize, final int archived) {
        DriveThruMultiMessage<OwnedProduct> response = new DriveThruMultiMessage<>();
        response.setMessage(getLibraryProducts(bearerToken, page, pageSize, archived));
        return response;
    }

    /**
     * Reads a paginated JSON:API {@code OrderProductListResponse} page from the authenticated user's library.
     *
     * @param authorization raw JWT token returned by {@code /auth_key} (without {@code "Bearer "} prefix)
     * @param page 1-based page number
     * @param pageSize maximum items per page
     * @param archived {@code 0} = exclude archived, {@code 1} = include archived
     * @param getChecksum {@code 0} = omit file checksums, {@code 1} = include file checksums
     * @param getFilters {@code 0} = omit taxonomy filters, {@code 1} = include taxonomy filters
     * @return paginated JSON:API collection document of ordered products
     */
    @GetExchange(value = "/order_products?library=true", accept = JSON_API_MEDIA_TYPE)
    JsonApiCollectionDocument<OwnedProduct> getOrderProducts(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam("page") int page,
            @RequestParam("pageSize") int pageSize,
            @RequestParam("archived") int archived,
            @RequestParam("getChecksum") int getChecksum,
            @RequestParam("getFilters") int getFilters);

    /**
     * Reads a single ordered product resource by its {@code orderProductId}.
     *
     * @param authorization raw JWT token returned by {@code /auth_key}
     * @param orderProductId DriveThruRPG order product identifier
     * @return JSON:API document containing the ordered product
     */
    @GetExchange(value = "/order_products/{orderProductId}", accept = JSON_API_MEDIA_TYPE)
    JsonApiDocument<OwnedProduct> getOrderProduct(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable("orderProductId") long orderProductId);

    /**
     * Reads a page of the authenticated user's library as unwrapped {@link OwnedProduct} attributes.
     *
     * @param authorization the token exactly as returned by {@code auth_key}
     * @param page 1-based page number
     * @param pageSize number of products per page
     * @param archived archived-product selector
     * @return unwrapped product list for the requested page
     */
    default List<OwnedProduct> getLibraryProducts(
            final String authorization,
            final int page,
            final int pageSize,
            final int archived) {
        JsonApiCollectionDocument<OwnedProduct> document =
                getOrderProducts(authorization, page, pageSize, archived, 0, 0);
        return document != null ? document.getAttributesList() : List.of();
    }

    /**
     * Retrieves the JSON:API document for a single publisher.
     *
     * @param publisherId DriveThruRPG publisher id
     * @return JSON:API publisher document
     */
    @GetExchange(value = "/publishers/{publisherId}", accept = JSON_API_MEDIA_TYPE)
    JsonApiDocument<Publisher> getPublisherDocument(
            @PathVariable("publisherId") String publisherId);

    /**
     * Retrieves data for a single publisher wrapped in the legacy compatibility envelope.
     *
     * @param publisherId DriveThruRPG publisher id
     * @return publisher data
     */
    default DriveThruMessage<Publisher> getPublisher(final String publisherId) {
        DriveThruMessage<Publisher> response = new DriveThruMessage<>();
        JsonApiDocument<Publisher> document = getPublisherDocument(publisherId);
        if (document != null) {
            document.getAttributes().ifPresent(response::setMessage);
        }
        return response;
    }

    /**
     * Retrieves the JSON:API document for a single catalog product.
     *
     * @param productId DriveThruRPG product id
     * @return JSON:API product document
     */
    @GetExchange(value = "/products/{productId}", accept = JSON_API_MEDIA_TYPE)
    JsonApiDocument<Product> getProductDocument(
            @PathVariable("productId") String productId);

    /**
     * Retrieves data for a single product wrapped in the legacy compatibility envelope.
     *
     * @param productId DriveThruRPG product id
     * @return product data
     */
    default DriveThruMessage<Product> getProduct(final String productId) {
        DriveThruMessage<Product> response = new DriveThruMessage<>();
        Product.fromDocument(getProductDocument(productId)).ifPresent(response::setMessage);
        return response;
    }
}
