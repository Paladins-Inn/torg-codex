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
import de.paladinsinn.torg.codex.api.dto.MiracleDetailDto;
import de.paladinsinn.torg.codex.api.dto.MiracleSummaryDto;
import de.paladinsinn.torg.codex.domain.model.Miracle;
import de.paladinsinn.torg.codex.domain.markup.Censor;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface MiracleMapper {
    @Mapping(target = "publications", source = "products")
    MiracleSummaryDto toSummary(Miracle miracle);
    @Mapping(target = "publications", source = "products")
    @Mapping(target = "dn", source = "dn")
    @Mapping(target = "text", qualifiedByName = "censorText")
    MiracleDetailDto toDetail(Miracle miracle, @Context Censor censor);
}
