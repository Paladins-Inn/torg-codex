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

package de.paladinsinn.torg.codex.application.port.out;

import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import de.paladinsinn.torg.codex.domain.catalog.DetailAccessOutcome;
import de.paladinsinn.torg.codex.domain.catalog.GroupingDimension;

/**
 * Driven port reporting channel-agnostic catalog usage events from core use cases to the
 * observability adapter (FR-017, SC-008).
 *
 * <p>Implementations MUST NOT record caller identity, credentials, or owned-product sets.
 */
public interface CatalogUsageObserver {

    /**
     * No-op observer used by default constructors in isolated unit tests.
     */
    CatalogUsageObserver NO_OP = new CatalogUsageObserver() {
        @Override
        public void categoryAccessed(final CatalogArea area) {
        }

        @Override
        public void cosmAccessed(final String cosmSlug) {
        }

        @Override
        public void groupingAccessed(final GroupingDimension dimension, final String groupKey) {
        }

        @Override
        public void detailAccessed(final CatalogArea area, final String entrySlug, final DetailAccessOutcome outcome) {
        }

        @Override
        public void detailNotFound(final CatalogArea area) {
        }
    };

    void categoryAccessed(CatalogArea area);

    void cosmAccessed(String cosmSlug);

    void groupingAccessed(GroupingDimension dimension, String groupKey);

    void detailAccessed(CatalogArea area, String entrySlug, DetailAccessOutcome outcome);

    void detailNotFound(CatalogArea area);
}
