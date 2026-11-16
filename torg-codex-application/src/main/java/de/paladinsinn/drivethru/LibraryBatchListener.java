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

/**
 * Callback invoked sequentially as each page of owned DriveThruRPG products arrives.
 *
 * <p>Implementations may throw {@link SupersededSyncException} to abort an outdated or invalidated
 * background synchronization run before any cache commit occurs.</p>
 */
@FunctionalInterface
public interface LibraryBatchListener {

    /**
     * Handles a newly retrieved library batch prior to fetching the next upstream page.
     *
     * @param batch sequential page batch
     */
    void onBatch(DriveThruLibraryBatch batch);
}
