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

package de.paladinsinn.torg.codex.api.dto;

import java.util.List;
import java.util.UUID;

/**
 * Purchase teaser returned with {@code 403 Forbidden} for a catalog entry the caller does not
 * own (ADR-019). It deliberately carries no protected entry data.
 *
 * @param id               the requested entry id
 * @param name             the entry display name
 * @param type             the catalog area tag (e.g. {@code spell}, {@code spell-list}, {@code group})
 * @param requiredProducts the products that grant access, sorted by codex id; never empty
 */
public record EntryTeaserDto(UUID id, String name, String type, List<RequiredProductDto> requiredProducts) {
}
