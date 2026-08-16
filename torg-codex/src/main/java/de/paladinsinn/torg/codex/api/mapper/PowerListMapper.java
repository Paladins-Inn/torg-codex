package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.PowerListDetailDto;
import de.paladinsinn.torg.codex.api.dto.PowerListSummaryDto;
import de.paladinsinn.torg.codex.domain.model.PowerList;
import de.paladinsinn.torg.codex.domain.markup.Censor;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface PowerListMapper {
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    PowerListSummaryDto toSummary(PowerList powerList);
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    @Mapping(target = "notes", qualifiedByName = "censorText")
    @Mapping(target = "text", qualifiedByName = "censorText")
    PowerListDetailDto toDetail(PowerList powerList, @Context Censor censor);
}
