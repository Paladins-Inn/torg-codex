package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.MiracleListDetailDto;
import de.paladinsinn.torg.codex.api.dto.MiracleListSummaryDto;
import de.paladinsinn.torg.codex.data.model.MiracleList;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface MiracleListMapper {
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    MiracleListSummaryDto toSummary(MiracleList miracleList);
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    MiracleListDetailDto toDetail(MiracleList miracleList);
}
