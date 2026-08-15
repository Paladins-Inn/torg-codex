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

import de.paladinsinn.torg.codex.data.model.ClearanceLevel;
import java.util.List;
import de.paladinsinn.torg.codex.data.model.ClearanceLevel;
import java.util.UUID;


/**
 * Summary representation used in all list endpoints ({@code GET /api/{entity}}).
 *
 * @param id             UUID of the entity
 * @param name           human-readable name
 * @param cosm           cosm/reality this entry belongs to ({@code id} + {@code name}), or {@code null} if not applicable
 * @param clearanceLevel product clearance level, or {@code null} if publicly accessible
 * @param publications   list of publication references ({@code id} + {@code title}) for all
 *                       products this entry was published in
 */
public record EntitySummaryDto(
        UUID id,
        String name,
        CosmRefDto cosm,
        ClearanceLevel clearanceLevel,
        List<PublicationRefDto> publications) {
}

