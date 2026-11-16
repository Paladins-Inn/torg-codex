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

package de.paladinsinn.security;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable session state tracking the active DriveThruRPG library synchronization generation
 * and monotonic batch progress (ADR-023).
 *
 * @param syncGenerationId unique identifier of the current login/reload synchronization run
 * @param status           current lifecycle status
 * @param startedAt        run start timestamp
 * @param appliedBatches   monotonically increasing number of batches applied in this generation
 */
public record DriveThruSessionSyncState(
        UUID syncGenerationId,
        SyncStatus status,
        Instant startedAt,
        int appliedBatches) implements Serializable {

    public static final String SYNC_STATE_KEY = DriveThruSessionSyncState.class.getName() + ".state";
    public static final String LOGGED_OUT_KEY = DriveThruSessionSyncState.class.getName() + ".loggedOut";

    /**
     * Validates generation, status, timestamp, and non-negative batch count invariants.
     *
     * @param syncGenerationId generation UUID
     * @param status           synchronization status
     * @param startedAt        start instant
     * @param appliedBatches   applied batch count
     */
    public DriveThruSessionSyncState {
        Objects.requireNonNull(syncGenerationId, "syncGenerationId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(startedAt, "startedAt");
        if (appliedBatches < 0) {
            throw new IllegalArgumentException("appliedBatches must be >= 0.");
        }
    }

    public static DriveThruSessionSyncState running(final UUID generationId, final Instant startedAt) {
        return new DriveThruSessionSyncState(generationId, SyncStatus.RUNNING, startedAt, 0);
    }

    public static DriveThruSessionSyncState idle(final UUID generationId, final Instant startedAt) {
        return new DriveThruSessionSyncState(generationId, SyncStatus.IDLE, startedAt, 0);
    }

    public DriveThruSessionSyncState withBatchApplied() {
        return new DriveThruSessionSyncState(syncGenerationId, status, startedAt, appliedBatches + 1);
    }

    public DriveThruSessionSyncState withStatus(final SyncStatus nextStatus) {
        return new DriveThruSessionSyncState(syncGenerationId, nextStatus, startedAt, appliedBatches);
    }
}
