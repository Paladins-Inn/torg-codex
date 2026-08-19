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
import de.paladinsinn.torg.codex.api.dto.CosmDetailDto;
import de.paladinsinn.torg.codex.api.dto.CosmSummaryDto;
import de.paladinsinn.torg.codex.domain.model.Cosm;
import de.paladinsinn.torg.codex.domain.markup.Censor;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface CosmMapper {
    @Mapping(target = "cosm", expression = "java(new CosmRefDto(cosm.id(), cosm.name()))")
    @Mapping(target = "publications", source = "products")
    CosmSummaryDto toSummary(Cosm cosm);
    @Mapping(target = "cosm", expression = "java(new CosmRefDto(cosm.id(), cosm.name()))")
    @Mapping(target = "publications", source = "products")
    @Mapping(target = "text", qualifiedByName = "censorText")
    @Mapping(target = "worldLaws", qualifiedByName = "censorText")
    CosmDetailDto toDetail(Cosm cosm, @Context Censor censor);
}
