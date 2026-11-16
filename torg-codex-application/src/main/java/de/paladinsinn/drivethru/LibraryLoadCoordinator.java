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
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Serializes library cache fills and {@code @CachePut} streaming reloads per
 * {@link ApiKeyFingerprint} around the Spring-proxied {@link DriveThruLibraryCacheService} so
 * concurrent callers for the same API key do not duplicate upstream pagination or race on cache
 * commits, while distinct keys proceed concurrently (T045, ADR-023).
 */
@Component
@RequiredArgsConstructor
public class LibraryLoadCoordinator {

    private final DriveThruLibraryCacheService cacheService;
    private final Map<ApiKeyFingerprint, RefCountedLock> locks = new ConcurrentHashMap<>();

    /**
     * Reads an existing cached snapshot or serializes a single cache-populating load for
     * {@code fingerprint}.
     *
     * @param fingerprint normalized API-key SHA-256 fingerprint
     * @param token       validated DriveThruRPG token
     * @return complete library snapshot
     * @throws NoValidTokenException if the token cannot be used or renewed
     */
    public DriveThruLibrarySnapshot loadCoordinated(
            final ApiKeyFingerprint fingerprint,
            final DrivethruToken token) throws NoValidTokenException {
        Objects.requireNonNull(fingerprint, "fingerprint");
        Objects.requireNonNull(token, "token");

        Optional<DriveThruLibrarySnapshot> fastHit = cacheService.findCachedLibrary(fingerprint);
        if (fastHit.isPresent()) {
            return fastHit.get();
        }

        RefCountedLock entry = acquireLock(fingerprint);
        try {
            Optional<DriveThruLibrarySnapshot> rechecked = cacheService.findCachedLibrary(fingerprint);
            if (rechecked.isPresent()) {
                return rechecked.get();
            }
            return cacheService.loadLibrary(fingerprint, token);
        } finally {
            releaseLock(fingerprint, entry);
        }
    }

    /**
     * Serializes a forced streaming library reload for {@code fingerprint} through the proxy's
     * {@code @CachePut} commit boundary.
     *
     * @param fingerprint        normalized API-key SHA-256 fingerprint
     * @param token              key-free session token copy
     * @param listener           sequential batch callback
     * @param preCommitValidator optional active-generation check executed immediately before cache commit
     * @return refreshed complete snapshot
     * @throws NoValidTokenException if the token expires and cannot be renewed
     */
    public DriveThruLibrarySnapshot reloadCoordinated(
            final ApiKeyFingerprint fingerprint,
            final DrivethruToken token,
            final LibraryBatchListener listener,
            final Runnable preCommitValidator) throws NoValidTokenException {
        Objects.requireNonNull(fingerprint, "fingerprint");
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(listener, "listener");

        RefCountedLock entry = acquireLock(fingerprint);
        try {
            return cacheService.reloadLibraryStreaming(fingerprint, token, listener, preCommitValidator);
        } finally {
            releaseLock(fingerprint, entry);
        }
    }

    /**
     * Read-only lookup for an unexpired cached library snapshot.
     *
     * @param fingerprint normalized API-key SHA-256 fingerprint
     * @return cached snapshot if present
     */
    public Optional<DriveThruLibrarySnapshot> findCached(final ApiKeyFingerprint fingerprint) {
        return cacheService.findCachedLibrary(fingerprint);
    }

    int activeLockCount() {
        return locks.size();
    }

    private RefCountedLock acquireLock(final ApiKeyFingerprint fingerprint) {
        RefCountedLock entry = locks.compute(fingerprint, (key, existing) -> {
            RefCountedLock target = existing != null ? existing : new RefCountedLock();
            target.refCount++;
            return target;
        });
        entry.lock.lock();
        return entry;
    }

    private void releaseLock(final ApiKeyFingerprint fingerprint, final RefCountedLock entry) {
        try {
            locks.computeIfPresent(fingerprint, (key, current) -> {
                if (current == entry) {
                    current.refCount--;
                    return current.refCount <= 0 ? null : current;
                }
                return current;
            });
        } finally {
            entry.lock.unlock();
        }
    }

    private static final class RefCountedLock {
        private final ReentrantLock lock = new ReentrantLock();
        private int refCount;
    }
}
