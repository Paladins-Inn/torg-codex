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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.cache.autoconfigure.CacheManagerCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configures the dedicated bounded 12-hour Caffeine cache {@value #LIBRARY_CACHE} and its
 * {@link ApiKeyFingerprint}-only key generator without altering existing product or publisher
 * cache semantics (FR-010, ADR-023).
 */
@Configuration
@EnableConfigurationProperties(DriveThruLibraryCacheConfig.DriveThruLibraryCacheProperties.class)
public class DriveThruLibraryCacheConfig {

    public static final String LIBRARY_CACHE = "drivethru.library";
    private static final long DEFAULT_TTL_HOURS = 12L;
    private static final long DEFAULT_MAX_SIZE = 1_000L;

    /**
     * Configuration properties for {@code drivethru.library.cache.*}.
     */
    @ConfigurationProperties(prefix = "drivethru.library.cache")
    @Getter
    @Setter
    public static class DriveThruLibraryCacheProperties {

        private Duration ttl = Duration.ofHours(DEFAULT_TTL_HOURS);
        private long maxSize = DEFAULT_MAX_SIZE;
    }

    /**
     * Registers {@value #LIBRARY_CACHE} as a bounded expiring custom Caffeine cache while leaving
     * {@code drivethru.product} and {@code drivethru.publisher} dynamic and non-expiring.
     *
     * @param properties library cache TTL and max-size settings
     * @param ticker     optional Caffeine ticker (system ticker in production)
     * @return customizer applied to {@link CaffeineCacheManager}
     */
    @Bean
    public CacheManagerCustomizer<CaffeineCacheManager> driveThruLibraryCacheCustomizer(
            final DriveThruLibraryCacheProperties properties,
            final ObjectProvider<Ticker> ticker) {
        return cacheManager -> registerLibraryCache(
                cacheManager,
                properties,
                ticker.getIfAvailable(Ticker::systemTicker));
    }

    /**
     * Fallback {@link CaffeineCacheManager} used when Spring Boot's cache auto-configuration is not
     * active (for example in standalone slice tests).
     *
     * @param properties library cache properties
     * @param ticker     optional Caffeine ticker
     * @return configured cache manager
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CaffeineCacheManager fallbackCaffeineCacheManager(
            final DriveThruLibraryCacheProperties properties,
            final ObjectProvider<Ticker> ticker) {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        registerLibraryCache(manager, properties, ticker.getIfAvailable(Ticker::systemTicker));
        return manager;
    }

    /**
     * Key generator accepting exclusively {@link ApiKeyFingerprint} as the first method argument so
     * cleartext API keys can never enter cache storage.
     *
     * @return fingerprint-only cache key generator
     */
    @Bean(name = "libraryCacheKeyGenerator")
    public KeyGenerator libraryCacheKeyGenerator() {
        return (target, method, params) -> {
            if (params.length == 0 || !(params[0] instanceof ApiKeyFingerprint fingerprint)) {
                throw new IllegalArgumentException(
                        "Library cache methods must accept ApiKeyFingerprint as their first parameter: "
                        + method.getName());
            }
            return fingerprint.value();
        };
    }

    private static void registerLibraryCache(
            final CaffeineCacheManager cacheManager,
            final DriveThruLibraryCacheProperties properties,
            final Ticker ticker) {
        Duration effectiveTtl = properties.getTtl() != null && !properties.getTtl().isNegative()
                ? properties.getTtl()
                : Duration.ofHours(DEFAULT_TTL_HOURS);
        long effectiveMaxSize = Math.max(1L, properties.getMaxSize());
        cacheManager.registerCustomCache(
                LIBRARY_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(effectiveTtl)
                        .maximumSize(effectiveMaxSize)
                        .ticker(ticker)
                        .build());
    }
}
