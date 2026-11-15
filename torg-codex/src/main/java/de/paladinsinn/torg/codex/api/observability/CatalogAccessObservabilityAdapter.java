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

package de.paladinsinn.torg.codex.api.observability;

import de.paladinsinn.torg.codex.application.port.out.CatalogAccessObserver;
import de.paladinsinn.torg.codex.domain.markup.EntityType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Driven adapter reporting catalog entries hidden by the DRM because they have no product
 * assignment (ADR-019, FR-016/FR-017).
 *
 * <p>Every call logs one {@code WARN} with the structured fields {@code catalogArea},
 * {@code entryId} and {@code entryName} and increments the counter
 * {@value #COUNTER_NAME} tagged only with the bounded {@code area}. No caller data is recorded.
 */
@Slf4j
@Component
public class CatalogAccessObservabilityAdapter implements CatalogAccessObserver {

    /** The Micrometer name of the counter of entries without product assignment. */
    public static final String COUNTER_NAME = "torg.codex.catalog.entries.without.products";

    private final Map<EntityType, Counter> counters = new EnumMap<>(EntityType.class);

    /**
     * Registers one counter per catalog area.
     *
     * @param registry the meter registry
     */
    public CatalogAccessObservabilityAdapter(final MeterRegistry registry) {
        for (EntityType area : EntityType.values()) {
            counters.put(area, Counter.builder(COUNTER_NAME)
                    .description("Catalog entries without product assignment encountered and hidden by the DRM")
                    .tag("area", area.tag())
                    .register(registry));
        }
    }

    @Override
    public void entryWithoutProducts(final EntityType area, final UUID id, final String name) {
        log.atWarn()
                .addKeyValue("catalogArea", area.tag())
                .addKeyValue("entryId", id)
                .addKeyValue("entryName", name)
                .log("Catalog entry without product assignment hidden by DRM: area={}, id={}, name={}", area.tag(), id, name);
        counters.get(area).increment();
    }
}
