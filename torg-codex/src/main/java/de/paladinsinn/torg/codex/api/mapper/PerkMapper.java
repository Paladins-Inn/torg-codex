package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.PerkDetailDto;
import de.paladinsinn.torg.codex.api.dto.PerkSummaryDto;
import de.paladinsinn.torg.codex.data.model.Perk;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface PerkMapper {
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    PerkSummaryDto toSummary(Perk perk);
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    PerkDetailDto toDetail(Perk perk);
}
