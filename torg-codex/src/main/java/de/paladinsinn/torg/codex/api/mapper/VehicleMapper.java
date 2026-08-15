package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.VehicleDetailDto;
import de.paladinsinn.torg.codex.api.dto.VehicleSummaryDto;
import de.paladinsinn.torg.codex.api.dto.VehicleWeaponDto;
import de.paladinsinn.torg.codex.data.model.Vehicle;
import de.paladinsinn.torg.codex.data.model.VehicleWeapon;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface VehicleMapper {
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    VehicleSummaryDto toSummary(Vehicle vehicle);
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    VehicleDetailDto toDetail(Vehicle vehicle);
    VehicleWeaponDto toDto(VehicleWeapon vehicleWeapon);
}
