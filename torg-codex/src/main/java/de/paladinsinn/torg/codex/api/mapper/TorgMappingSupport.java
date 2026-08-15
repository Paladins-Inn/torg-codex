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

package de.paladinsinn.torg.codex.api.mapper;

import de.paladinsinn.torg.codex.api.dto.CosmRefDto;
import de.paladinsinn.torg.codex.api.dto.DifficultyNumberDto;
import de.paladinsinn.torg.codex.api.dto.PublicationRefDto;
import de.paladinsinn.torg.codex.application.port.in.CatalogReferenceQuery;
import de.paladinsinn.torg.codex.data.model.DifficultyNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Helper component used by all MapStruct mappers (via {@code uses = TorgMappingSupport.class}).
 *
 * <p>Provides type-conversion methods that require catalog reference lookups (cosm lookup,
 * publication resolution) as well as simple value-object mappings (difficulty number).</p>
 */
@Component
@RequiredArgsConstructor
public class TorgMappingSupport {

    private final CatalogReferenceQuery catalogReferenceQuery;

    /**
     * Converts a cosm name (String) held in an entity into a {@link CosmRefDto}.
     * Falls back to {@code CosmRefDto(null, cosmName)} if the cosm cannot be found.
     */
    public CosmRefDto toCosmRef(String cosmName) {
        if (cosmName == null || cosmName.isBlank()) return null;
        return catalogReferenceQuery.findCosmByName(cosmName)
                .map(c -> new CosmRefDto(c.id(), c.name()))
                .orElse(new CosmRefDto(null, cosmName));
    }

    /**
     * Converts a set of codex-IDs (product slugs) to a list of {@link PublicationRefDto}.
     */
    public List<PublicationRefDto> toPublicationRefs(Set<String> products) {
        if (products == null || products.isEmpty()) return List.of();
        final List<PublicationRefDto> result = new ArrayList<>();
        for (final String codexId : products) {
            catalogReferenceQuery.findPublicationByCodexId(codexId)
                    .ifPresent(p -> result.add(new PublicationRefDto(p.id(), p.name())));
        }
        return Collections.unmodifiableList(result);
    }

    /** Converts a {@link DifficultyNumber} embeddable to its DTO. */
    public DifficultyNumberDto toDifficultyNumberDto(DifficultyNumber dn) {
        if (dn == null) return null;
        return new DifficultyNumberDto(dn.getLevel(), dn.getText());
    }
}
