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

import org.springframework.web.bind.annotation.PathVariable;
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

/**
 * Declarative HTTP interface for the DriveThruRPG REST API.
 *
 * <p>Backed by a Spring {@code RestClient} via {@code HttpServiceProxyFactory}.
 * No Feign dependency required.</p>
 */
@HttpExchange(contentType = "application/json", accept = "application/json")
public interface DriveThruRPGClient {

    /**
     * Retrieves a bearer token for the given API key.
     *
     * @param authorization {@code "Bearer <apiKey>"}
     * @return token response with {@code access_token} and {@code customers_id}
     */
    @PostExchange("/token")
    DriveThruMessage<LinkedHashMap<String, String>> getToken(
            @RequestHeader("Authorization") String authorization);

    /**
     * Retrieves a page of products owned by a customer.
     *
     * @param bearerToken  {@code "Bearer <accessToken>"}
     * @param customerId   DriveThruRPG customer id
     * @param page         1-based page number
     * @param pageSize     number of items per page
     * @param archived     {@code 0} = exclude archived, {@code 1} = include archived
     * @return list of owned products
     */
    @GetExchange("/customers/{customerId}/products")
    DriveThruMultiMessage<OwnedProduct> getOwnedProducts(
            @RequestHeader("Authorization") String bearerToken,
            @PathVariable("customerId") String customerId,
            @RequestParam("page") int page,
            @RequestParam("page_size") int pageSize,
            @RequestParam("include_archived") int archived);

    /**
     * Retrieves data for a single publisher.
     *
     * @param publisherId DriveThruRPG publisher id
     * @return publisher data
     */
    @GetExchange("/publishers/{publisherId}")
    DriveThruMessage<Publisher> getPublisher(
            @PathVariable("publisherId") String publisherId);

    /**
     * Retrieves data for a single product.
     *
     * @param productId DriveThruRPG product id
     * @return product data
     */
    @GetExchange("/products/{productId}")
    DriveThruMessage<Product> getProduct(
            @PathVariable("productId") String productId);
}

