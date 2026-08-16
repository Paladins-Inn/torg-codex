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

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

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

