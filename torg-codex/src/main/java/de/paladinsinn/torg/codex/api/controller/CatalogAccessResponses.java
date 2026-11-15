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

package de.paladinsinn.torg.codex.api.controller;

import de.paladinsinn.torg.codex.api.mapper.EntryTeaserMapper;
import de.paladinsinn.torg.codex.domain.access.EntryAccess;
import de.paladinsinn.torg.codex.domain.model.ProductBoundEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * Maps the result of a restricted single-entry read to its HTTP response (ADR-019).
 *
 * <ul>
 *   <li>granted: {@code 200} with the existing detail DTO;</li>
 *   <li>teaser: {@code 403} with the closed purchase teaser;</li>
 *   <li>absent (missing or unassigned): the existing empty {@code 404}.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class CatalogAccessResponses {

    private final EntryTeaserMapper teaserMapper;

    /**
     * Converts an entry access result into a response.
     *
     * @param access       the access result
     * @param detailMapper maps a granted entry to its detail DTO
     * @param <T>          the entry type
     * @param <D>          the detail DTO type
     * @return the response
     */
    public <T extends ProductBoundEntry, D> ResponseEntity<?> toResponse(
            final EntryAccess<T> access, final Function<? super T, D> detailMapper) {
        return switch (access) {
            case EntryAccess.Granted<T> granted -> ResponseEntity.ok(detailMapper.apply(granted.entry()));
            case EntryAccess.Teaser<T> teaser -> ResponseEntity.status(HttpStatus.FORBIDDEN).body(teaserMapper.toDto(teaser));
            case EntryAccess.Absent<T> absent -> ResponseEntity.notFound().build();
        };
    }
}
