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

import com.github.benmanes.caffeine.cache.Ticker;
import de.paladinsinn.drivethru.products.Product;
import de.paladinsinn.drivethru.publishers.Publisher;
import de.paladinsinn.drivethru.token.DrivethruToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Spring cache-proxy integration tests for {@link DriveThruLibraryCacheConfig} and
 * {@link DriveThruLibraryCacheService} using a deterministic fake {@link Ticker} (US3, T039,
 * FR-010, FR-012, ADR-023).
 */
@SpringJUnitConfig(DriveThruLibraryCacheServiceTest.CacheProxyTestConfig.class)
@TestPropertySource(properties = "drivethru.library.cache.max-size=3")
class DriveThruLibraryCacheServiceTest {

    private static final ApiKeyFingerprint KEY_A = ApiKeyFingerprint.of("cache-test-key-alpha");
    private static final ApiKeyFingerprint KEY_B = ApiKeyFingerprint.of("cache-test-key-bravo");
    private static final ApiKeyFingerprint KEY_C = ApiKeyFingerprint.of("cache-test-key-charlie");
    private static final ApiKeyFingerprint KEY_D = ApiKeyFingerprint.of("cache-test-key-delta");
    private static final Duration DEFAULT_TTL = Duration.ofHours(12);
    private static final int PRODUCT_CORE = 216248;
    private static final int PRODUCT_AYSLE = 284210;
    private static final int PRODUCT_LIVING_LAND = 247246;
    private static final int CONFIGURED_MAX_SIZE = 3;
    private static final int EXTRA_DAYS_FOR_PRODUCT_CACHE = 30;

    @Autowired
    private DriveThruLibraryCacheService cacheService;

    @Autowired
    private DriveThruRPGService driveThruService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private KeyGenerator libraryCacheKeyGenerator;

    @Autowired
    private ControllableTicker ticker;

    @BeforeEach
    void setUp() {
        reset(driveThruService);
        Cache libCache = cacheManager.getCache(DriveThruLibraryCacheConfig.LIBRARY_CACHE);
        if (libCache != null) {
            libCache.clear();
        }
    }

    @Test
    void usesRealSpringProxyDigestOnlyKeysExactTwelveHourExpiryAndPreservesPriorEntryOnFailure()
            throws Exception {
        assertThat(AopUtils.isAopProxy(cacheService)).isTrue();
        assertThatThrownBy(() -> libraryCacheKeyGenerator.generate(cacheService,
                DriveThruLibraryCacheService.class.getMethods()[0], "raw-api-key-forbidden"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(libraryCacheKeyGenerator.generate(cacheService,
                DriveThruLibraryCacheService.class.getMethods()[0], KEY_A))
                .isEqualTo(KEY_A.value());

        DrivethruToken token = new DrivethruToken();
        token.setAccessToken("jwt-1");
        DriveThruLibrarySnapshot initial = DriveThruLibrarySnapshot.of(
                List.of(PRODUCT_CORE, PRODUCT_AYSLE), "424242", Instant.parse("2026-11-16T09:00:00Z"));
        when(driveThruService.streamOwnedProductBatches(any(), any())).thenReturn(initial);

        // First call populates cache; second call within 12h hits cache without calling service
        assertThat(cacheService.loadLibrary(KEY_A, token)).isEqualTo(initial);
        assertThat(cacheService.loadLibrary(KEY_A, token)).isEqualTo(initial);
        verify(driveThruService, times(1)).streamOwnedProductBatches(any(), any());

        // Key isolation: KEY_B misses while KEY_A hits
        DriveThruLibrarySnapshot emptySnapshot = DriveThruLibrarySnapshot.of(
                List.of(), null, Instant.parse("2026-11-16T09:01:00Z"));
        when(driveThruService.streamOwnedProductBatches(any(), any())).thenReturn(emptySnapshot);
        assertThat(cacheService.loadLibrary(KEY_B, token)).isEqualTo(emptySnapshot);
        assertThat(cacheService.loadLibrary(KEY_B, token)).isEqualTo(emptySnapshot);
        verify(driveThruService, times(2)).streamOwnedProductBatches(any(), any());

        // Advance to 12h - 1ns: still cached
        ticker.advance(DEFAULT_TTL.minusNanos(1));
        assertThat(cacheService.findCachedLibrary(KEY_A)).contains(initial);
        assertThat(cacheService.loadLibrary(KEY_A, token)).isEqualTo(initial);
        verify(driveThruService, times(2)).streamOwnedProductBatches(any(), any());

        // Advance +1ns (exact 12h boundary): expired!
        ticker.advance(Duration.ofNanos(1));
        assertThat(cacheService.findCachedLibrary(KEY_A)).isEmpty();

        // Repopulate KEY_A, then verify forced @CachePut reload replaces it on success and preserves it on failure
        when(driveThruService.streamOwnedProductBatches(any(), any())).thenReturn(initial);
        assertThat(cacheService.loadLibrary(KEY_A, token)).isEqualTo(initial);

        when(driveThruService.streamOwnedProductBatches(any(), any()))
                .thenThrow(new AuthenticationServiceException("upstream 503"));
        assertThatThrownBy(() -> cacheService.reloadLibraryStreaming(KEY_A, token, batch -> { }))
                .isInstanceOf(AuthenticationServiceException.class);
        assertThat(cacheService.findCachedLibrary(KEY_A)).contains(initial);

        reset(driveThruService);
        DriveThruLibrarySnapshot refreshed = DriveThruLibrarySnapshot.of(
                List.of(PRODUCT_CORE, PRODUCT_AYSLE, PRODUCT_LIVING_LAND),
                "424242",
                Instant.parse("2026-11-16T21:30:00Z"));
        when(driveThruService.streamOwnedProductBatches(any(), any())).thenReturn(refreshed);
        assertThat(cacheService.reloadLibraryStreaming(KEY_A, token, batch -> { })).isEqualTo(refreshed);
        assertThat(cacheService.findCachedLibrary(KEY_A)).contains(refreshed);
    }

    @Test
    void enforcesBoundedMaxSizeOnLibraryCacheWithoutExpiringProductOrPublisherCaches() throws Exception {
        DrivethruToken token = new DrivethruToken();
        when(driveThruService.streamOwnedProductBatches(any(), any())).thenAnswer(inv ->
                DriveThruLibrarySnapshot.of(List.of(PRODUCT_CORE), "424242", Instant.now()));

        cacheService.loadLibrary(KEY_A, token);
        cacheService.loadLibrary(KEY_B, token);
        cacheService.loadLibrary(KEY_C, token);
        cacheService.loadLibrary(KEY_D, token);

        com.github.benmanes.caffeine.cache.Cache<?, ?> nativeLibCache =
                (com.github.benmanes.caffeine.cache.Cache<?, ?>) cacheManager
                        .getCache(DriveThruLibraryCacheConfig.LIBRARY_CACHE)
                        .getNativeCache();
        nativeLibCache.cleanUp();
        assertThat(nativeLibCache.estimatedSize()).isLessThanOrEqualTo(CONFIGURED_MAX_SIZE);

        // Verify drivethru.product and drivethru.publisher remain non-expiring across 30 days
        Cache productCache = cacheManager.getCache("drivethru.product");
        Cache publisherCache = cacheManager.getCache("drivethru.publisher");
        assertThat(productCache).isNotNull();
        assertThat(publisherCache).isNotNull();
        productCache.put("216248", Optional.of(new Product()));
        publisherCache.put("3444", Optional.of(new Publisher()));

        ticker.advance(Duration.ofDays(EXTRA_DAYS_FOR_PRODUCT_CACHE));
        assertThat(productCache.get("216248")).isNotNull();
        assertThat(publisherCache.get("3444")).isNotNull();
    }

    /** Controllable monotonic nano-clock for deterministic Caffeine expiry assertions. */
    static final class ControllableTicker implements Ticker {

        private final AtomicLong nanos = new AtomicLong(1_000_000_000L);

        @Override
        public long read() {
            return nanos.get();
        }

        void advance(final Duration delta) {
            nanos.addAndGet(delta.toNanos());
        }
    }

    /** Minimal Spring caching configuration exercising real proxy interception (unannotated to avoid @EnableDrivethruRPG scan). */
    @EnableCaching
    @Import({DriveThruLibraryCacheConfig.class, DriveThruLibraryCacheService.class})
    static class CacheProxyTestConfig {

        @Bean
        ControllableTicker controllableTicker() {
            return new ControllableTicker();
        }

        @Bean
        DriveThruRPGService driveThruRPGService() {
            return mock(DriveThruRPGService.class);
        }
    }
}
