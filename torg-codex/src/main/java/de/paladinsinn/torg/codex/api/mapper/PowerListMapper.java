package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.PowerListDetailDto;
import de.paladinsinn.torg.codex.api.dto.PowerListSummaryDto;
import de.paladinsinn.torg.codex.data.model.PowerList;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface PowerListMapper {
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    PowerListSummaryDto toSummary(PowerList powerList);
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    PowerListDetailDto toDetail(PowerList powerList);
}
