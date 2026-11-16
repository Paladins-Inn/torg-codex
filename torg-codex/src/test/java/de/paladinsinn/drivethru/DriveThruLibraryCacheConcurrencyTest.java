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

import de.paladinsinn.drivethru.token.DrivethruToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Latch-driven concurrency tests for {@link LibraryLoadCoordinator} and proxied
 * {@link DriveThruLibraryCacheService} commits (US3, T040, FR-010, FR-012, ADR-023).
 */
@SpringJUnitConfig(DriveThruLibraryCacheConcurrencyTest.ConcurrencyTestConfig.class)
class DriveThruLibraryCacheConcurrencyTest {

    private static final ApiKeyFingerprint KEY_ONE = ApiKeyFingerprint.of("concurrency-key-one");
    private static final ApiKeyFingerprint KEY_TWO = ApiKeyFingerprint.of("concurrency-key-two");
    private static final int TIMEOUT_SECONDS = 5;
    private static final int PRODUCT_OLD = 216248;
    private static final int PRODUCT_NEW = 284210;

    @Autowired
    private LibraryLoadCoordinator coordinator;

    @Autowired
    private DriveThruLibraryCacheService cacheService;

    @Autowired
    private DriveThruRPGService driveThruService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        reset(driveThruService);
        Cache cache = cacheManager.getCache(DriveThruLibraryCacheConfig.LIBRARY_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void deduplicatesConcurrentSameKeyMissesWithoutBlockingDistinctFingerprintsAndCleansLocks()
            throws Exception {
        CountDownLatch keyOneEntered = new CountDownLatch(1);
        CountDownLatch releaseKeyOne = new CountDownLatch(1);
        CountDownLatch bothCallersSubmitted = new CountDownLatch(2);
        AtomicInteger keyOneCalls = new AtomicInteger(0);

        DrivethruToken tokenOne = new DrivethruToken();
        tokenOne.setCustomerId("one");
        DrivethruToken tokenTwo = new DrivethruToken();
        tokenTwo.setCustomerId("two");

        when(driveThruService.streamOwnedProductBatches(any(), any())).thenAnswer(inv -> {
            DrivethruToken tok = inv.getArgument(0);
            if ("one".equals(tok.getCustomerId())) {
                keyOneCalls.incrementAndGet();
                keyOneEntered.countDown();
                assertThat(releaseKeyOne.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
                return DriveThruLibrarySnapshot.of(List.of(PRODUCT_OLD), "one", Instant.now());
            }
            return DriveThruLibrarySnapshot.of(List.of(PRODUCT_NEW), "two", Instant.now());
        });

        try (var pool = Executors.newFixedThreadPool(3)) {
            var first = pool.submit(() -> {
                bothCallersSubmitted.countDown();
                return coordinator.loadCoordinated(KEY_ONE, tokenOne);
            });
            assertThat(keyOneEntered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

            var secondSameKey = pool.submit(() -> {
                bothCallersSubmitted.countDown();
                return coordinator.loadCoordinated(KEY_ONE, tokenOne);
            });
            // Unrelated KEY_TWO completes immediately while KEY_ONE is still held on releaseKeyOne!
            var unrelatedKey = pool.submit(() -> coordinator.loadCoordinated(KEY_TWO, tokenTwo));
            assertThat(unrelatedKey.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).productIds())
                    .containsExactly(PRODUCT_NEW);

            assertThat(bothCallersSubmitted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
            releaseKeyOne.countDown();

            assertThat(first.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).productIds())
                    .containsExactly(PRODUCT_OLD);
            assertThat(secondSameKey.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).productIds())
                    .containsExactly(PRODUCT_OLD);
        }

        assertThat(keyOneCalls.get()).isEqualTo(1);
        assertThat(coordinator.activeLockCount()).isZero();
    }

    @Test
    void supersessionAtFinalCommitBoundaryPreventsStaleCacheWriteAndPreservesPriorEntry()
            throws Exception {
        DrivethruToken token = new DrivethruToken();
        DriveThruLibrarySnapshot priorValid = DriveThruLibrarySnapshot.of(
                List.of(PRODUCT_OLD), "424242", Instant.parse("2026-11-16T08:00:00Z"));
        when(driveThruService.streamOwnedProductBatches(any(), any())).thenReturn(priorValid);
        coordinator.loadCoordinated(KEY_ONE, token);
        assertThat(cacheService.findCachedLibrary(KEY_ONE)).contains(priorValid);

        reset(driveThruService);
        DriveThruLibrarySnapshot staleCandidate = DriveThruLibrarySnapshot.of(
                List.of(PRODUCT_OLD, PRODUCT_NEW), "424242", Instant.parse("2026-11-16T09:00:00Z"));
        when(driveThruService.streamOwnedProductBatches(any(), any())).thenReturn(staleCandidate);

        assertThatThrownBy(() -> coordinator.reloadCoordinated(
                KEY_ONE,
                token,
                batch -> { },
                () -> {
                    throw new SupersededSyncException("logout occurred right before commit");
                }))
                .isInstanceOf(SupersededSyncException.class);

        assertThat(cacheService.findCachedLibrary(KEY_ONE)).contains(priorValid);
        assertThat(coordinator.activeLockCount()).isZero();
    }

    /** Minimal caching context with real proxy + coordinator (unannotated to avoid @EnableDrivethruRPG scan). */
    @EnableCaching
    @Import({DriveThruLibraryCacheConfig.class, DriveThruLibraryCacheService.class, LibraryLoadCoordinator.class})
    static class ConcurrencyTestConfig {

        @Bean
        DriveThruRPGService driveThruRPGService() {
            return mock(DriveThruRPGService.class);
        }
    }
}
