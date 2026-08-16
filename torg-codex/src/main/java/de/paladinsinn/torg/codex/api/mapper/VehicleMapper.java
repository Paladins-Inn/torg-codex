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
