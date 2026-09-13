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
import de.paladinsinn.torg.codex.domain.markup.Censor;
import de.paladinsinn.torg.codex.data.model.DifficultyNumber;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Context;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Helper component used by all MapStruct mappers (via {@code uses = TorgMappingSupport.class}).
 *
 * <p>Provides type-conversion methods that require catalog reference lookups (cosm lookup,
 * publication resolution) as well as simple value-object mappings (difficulty number).</p>
 */
@SuppressWarnings("unused")
@Component
@RequiredArgsConstructor
public class TorgMappingSupport {

    private final CatalogReferenceQuery catalogReferenceQuery;

    /**
     * Converts the cosm slug held in an entity into a {@link CosmRefDto} carrying the
     * cosm id and its display name.
     *
     * <p>Falls back to {@code CosmRefDto(null, cosmSlug)} if the slug cannot be resolved,
     * so an unknown reference still identifies itself.</p>
     */
    public CosmRefDto toCosmRef(String cosmSlug) {
        if (cosmSlug == null || cosmSlug.isBlank()) return null;
        return catalogReferenceQuery.findCosmBySlug(cosmSlug)
                .map(c -> new CosmRefDto(c.id(), c.name()))
                .orElse(new CosmRefDto(null, cosmSlug));
    }

    /**
     * Folds zero, one, or many cosm slugs into the existing single cosm response field.
     */
    public CosmRefDto toCosmRef(Set<String> cosmSlugs) {
        if (cosmSlugs == null || cosmSlugs.isEmpty()) return null;
        final var names = cosmSlugs.stream()
                .filter(slug -> slug != null && !slug.isBlank())
                .distinct()
                .map(slug -> catalogReferenceQuery.findCosmBySlug(slug)
                        .map(cosm -> new CosmRefDto(cosm.id(), cosm.name()))
                        .orElse(new CosmRefDto(null, slug)))
                .sorted(java.util.Comparator.comparing(CosmRefDto::name))
                .toList();
        if (names.isEmpty()) return null;
        if (names.size() == 1) return names.getFirst();
        return new CosmRefDto(null, names.stream()
                .filter(Objects::nonNull)
                .map(CosmRefDto::name)
                .collect(java.util.stream.Collectors.joining(", ")));
    }

    /** Folds stable reference slugs into the retained comma-separated API field. */
    @Named("foldSlugs")
    public String foldSlugs(Set<String> slugs) {
        if (slugs == null || slugs.isEmpty()) return null;
        final var value = slugs.stream()
                .filter(slug -> slug != null && !slug.isBlank())
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.joining(", "));
        return value.isEmpty() ? null : value;
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

    /** Converts a domain-model difficulty number to its DTO. */
    public DifficultyNumberDto toDifficultyNumberDto(
            de.paladinsinn.torg.codex.domain.model.DifficultyNumber dn) {
        if (dn == null) return null;
        return new DifficultyNumberDto(dn.level(), dn.text());
    }

    /**
     * Renders a raw (un-censored) text field held by a domain model into its presentation
     * form using the request's {@link Censor}, exactly mirroring the behavior the JPA entity
     * text getters previously performed at read time. {@code null} raw values pass through
     * unchanged (see {@link Censor#apply(String)}).
     */
    @Named("censorText")
    public String censorText(String rawText, @Context Censor censor) {
        return censor.apply(rawText);
    }

    /**
     * Renders every value of a raw (un-censored) text map held by a domain model using the
     * request's {@link Censor}, preserving insertion order and keys, exactly mirroring the
     * behavior the JPA entity map getters previously performed at read time.
     */
    @Named("censorMap")
    public java.util.Map<String, String> censorMap(
            java.util.Map<String, String> rawMap, @Context Censor censor) {
        if (rawMap == null) return null;
        return rawMap.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        java.util.Map.Entry::getKey,
                        e -> censor.apply(e.getValue()),
                        (_, b) -> b,
                        java.util.LinkedHashMap::new));
    }
}
