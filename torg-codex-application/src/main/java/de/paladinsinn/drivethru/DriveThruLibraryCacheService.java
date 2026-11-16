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
import de.paladinsinn.drivethru.token.NoValidTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * Spring-proxied annotation-based library cache service storing complete, token-free
 * {@link DriveThruLibrarySnapshot} values keyed strictly by {@link ApiKeyFingerprint}
 * (FR-010, FR-012, FR-013, FR-018, ADR-023).
 */
@Service
@RequiredArgsConstructor
public class DriveThruLibraryCacheService {

    private final DriveThruRPGService driveThruRPGService;
    private final CacheManager cacheManager;

    /**
     * Loads the caller's complete DriveThruRPG library on a cache miss and caches the resulting
     * snapshot under {@code fingerprint}.
     *
     * @param fingerprint normalized API-key SHA-256 fingerprint
     * @param token       validated DriveThruRPG token
     * @return complete cached or freshly retrieved library snapshot
     * @throws NoValidTokenException if the token cannot be used or renewed
     */
    @Cacheable(
            cacheNames = DriveThruLibraryCacheConfig.LIBRARY_CACHE,
            keyGenerator = "libraryCacheKeyGenerator")
    public DriveThruLibrarySnapshot loadLibrary(
            final ApiKeyFingerprint fingerprint,
            final DrivethruToken token) throws NoValidTokenException {
        Objects.requireNonNull(fingerprint, "fingerprint");
        Objects.requireNonNull(token, "token");
        return driveThruRPGService.streamOwnedProductBatches(token, batch -> { });
    }

    /**
     * Streams batches from DriveThruRPG and updates the cached snapshot only upon normal completion.
     *
     * @param fingerprint normalized API-key SHA-256 fingerprint
     * @param token       validated DriveThruRPG token
     * @param listener    per-page batch listener
     * @return complete library snapshot written to the cache
     * @throws NoValidTokenException if the token expires and cannot be renewed
     */
    @CachePut(
            cacheNames = DriveThruLibraryCacheConfig.LIBRARY_CACHE,
            keyGenerator = "libraryCacheKeyGenerator")
    public DriveThruLibrarySnapshot reloadLibraryStreaming(
            final ApiKeyFingerprint fingerprint,
            final DrivethruToken token,
            final LibraryBatchListener listener) throws NoValidTokenException {
        return reloadLibraryStreaming(fingerprint, token, listener, null);
    }

    /**
     * Streams batches from DriveThruRPG and runs {@code preCommitValidator} immediately before
     * returning so superseded runs abort prior to {@code @CachePut} commit.
     *
     * @param fingerprint        normalized API-key SHA-256 fingerprint
     * @param token              validated DriveThruRPG token
     * @param listener           per-page batch listener
     * @param preCommitValidator optional validation hook invoked right before normal return
     * @return complete library snapshot written to the cache
     * @throws NoValidTokenException if the token expires and cannot be renewed
     */
    @CachePut(
            cacheNames = DriveThruLibraryCacheConfig.LIBRARY_CACHE,
            keyGenerator = "libraryCacheKeyGenerator")
    public DriveThruLibrarySnapshot reloadLibraryStreaming(
            final ApiKeyFingerprint fingerprint,
            final DrivethruToken token,
            final LibraryBatchListener listener,
            final Runnable preCommitValidator) throws NoValidTokenException {
        Objects.requireNonNull(fingerprint, "fingerprint");
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(listener, "listener");
        DriveThruLibrarySnapshot snapshot = driveThruRPGService.streamOwnedProductBatches(token, listener);
        if (preCommitValidator != null) {
            preCommitValidator.run();
        }
        return snapshot;
    }

    /**
     * Non-populating read-only lookup for an existing cached library snapshot.
     *
     * @param fingerprint normalized API-key SHA-256 fingerprint
     * @return cached snapshot if present and unexpired
     */
    public Optional<DriveThruLibrarySnapshot> findCachedLibrary(final ApiKeyFingerprint fingerprint) {
        if (fingerprint == null) {
            return Optional.empty();
        }
        Cache cache = cacheManager.getCache(DriveThruLibraryCacheConfig.LIBRARY_CACHE);
        if (cache == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(cache.get(fingerprint.value(), DriveThruLibrarySnapshot.class));
    }
}
