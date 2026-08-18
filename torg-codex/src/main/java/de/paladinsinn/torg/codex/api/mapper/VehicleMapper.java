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
import de.paladinsinn.torg.codex.api.dto.VehicleDetailDto;
import de.paladinsinn.torg.codex.api.dto.VehicleSummaryDto;
import de.paladinsinn.torg.codex.api.dto.VehicleWeaponDto;
import de.paladinsinn.torg.codex.domain.model.VehicleWeapon;
import de.paladinsinn.torg.codex.domain.model.Vehicle;
import de.paladinsinn.torg.codex.domain.markup.Censor;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface VehicleMapper {
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    VehicleSummaryDto toSummary(Vehicle vehicle);
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    @Mapping(target = "text", qualifiedByName = "censorText")
    VehicleDetailDto toDetail(Vehicle vehicle, @Context Censor censor);
    VehicleWeaponDto toDto(VehicleWeapon vehicleWeapon);
}
