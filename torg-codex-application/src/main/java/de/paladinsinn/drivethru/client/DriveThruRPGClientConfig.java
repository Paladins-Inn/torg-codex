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

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import de.paladinsinn.drivethru.products.OwnedProduct;
import de.paladinsinn.drivethru.products.Product;
import de.paladinsinn.drivethru.publishers.Publisher;
import de.paladinsinn.drivethru.resource.JsonApiCollectionDocument;
import de.paladinsinn.drivethru.resource.JsonApiDocument;
import de.paladinsinn.drivethru.token.DrivethruToken;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring configuration that wires up a {@link RestClient}-backed
 * {@link DriveThruRPGClient} proxy for the DriveThruRPG REST API.
 *
 * <p>Configuration is read from {@link DriveThruProperties}
 * ({@code drivethru.*} in {@code application.yaml}).</p>
 */
@Configuration
@EnableConfigurationProperties(DriveThruProperties.class)
@Slf4j
public class DriveThruRPGClientConfig {

    private record CachedDelegate(String baseUrl, DriveThruRPGClient delegate) {
    }

    /**
     * Creates the {@link DriveThruRPGClient} proxy backed by a {@link RestClient}.
     *
     * @param properties DriveThruRPG connection properties
     * @return the proxy instance
     */
    @Bean
    public DriveThruRPGClient driveThruRPGClient(final DriveThruProperties properties) {
        log.info("Configuring DriveThruRPG REST client. baseUrl={}, connectTimeout={}, readTimeout={}",
                properties.getBaseUrl(), properties.getConnectTimeout(), properties.getReadTimeout());

        final AtomicReference<CachedDelegate> cached = new AtomicReference<>(
                new CachedDelegate(properties.getBaseUrl(), buildDelegate(properties)));

        return new DriveThruRPGClient() {
            private DriveThruRPGClient current() {
                String activeUrl = properties.getBaseUrl();
                CachedDelegate existing = cached.get();
                if (Objects.equals(existing.baseUrl(), activeUrl)) {
                    return existing.delegate();
                }
                CachedDelegate updated = new CachedDelegate(activeUrl, buildDelegate(properties));
                cached.set(updated);
                return updated.delegate();
            }

            @Override
            public DrivethruToken authenticate(
                    final String applicationKey,
                    final Map<String, Object> body) {
                return current().authenticate(applicationKey, body);
            }

            @Override
            public DrivethruToken refreshSession(
                    final String refreshCookie,
                    final Map<String, Object> body) {
                return current().refreshSession(refreshCookie, body);
            }

            @Override
            public JsonApiCollectionDocument<OwnedProduct> getOrderProducts(
                    final String authorization,
                    final int page,
                    final int pageSize,
                    final int archived,
                    final int getChecksum,
                    final int getFilters) {
                return current().getOrderProducts(
                        authorization, page, pageSize, archived, getChecksum, getFilters);
            }

            @Override
            public JsonApiDocument<OwnedProduct> getOrderProduct(
                    final String authorization,
                    final long orderProductId) {
                return current().getOrderProduct(authorization, orderProductId);
            }

            @Override
            public JsonApiDocument<Publisher> getPublisherDocument(final String publisherId) {
                return current().getPublisherDocument(publisherId);
            }

            @Override
            public JsonApiDocument<Product> getProductDocument(final String productId) {
                return current().getProductDocument(productId);
            }
        };
    }

    private DriveThruRPGClient buildDelegate(final DriveThruProperties properties) {
        final SimpleClientHttpRequestFactory requestFactory = createRequestFactory(
                properties.getConnectTimeout(), properties.getReadTimeout());

        final RestClient restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();

        final HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        return factory.createClient(DriveThruRPGClient.class);
    }

    private SimpleClientHttpRequestFactory createRequestFactory(
            final Duration connectTimeout, final Duration readTimeout) {
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
