package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.MiracleListDetailDto;
import de.paladinsinn.torg.codex.api.dto.MiracleListSummaryDto;
import de.paladinsinn.torg.codex.domain.model.MiracleList;
import de.paladinsinn.torg.codex.domain.markup.Censor;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface MiracleListMapper {
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    MiracleListSummaryDto toSummary(MiracleList miracleList);
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    @Mapping(target = "notes", qualifiedByName = "censorText")
    @Mapping(target = "text", qualifiedByName = "censorText")
    MiracleListDetailDto toDetail(MiracleList miracleList, @Context Censor censor);
}
