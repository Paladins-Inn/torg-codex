package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.CosmDetailDto;
import de.paladinsinn.torg.codex.api.dto.CosmRefDto;
import de.paladinsinn.torg.codex.api.dto.CosmSummaryDto;
import de.paladinsinn.torg.codex.data.model.Cosm;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface CosmMapper {
    @Mapping(target = "cosm", expression = "java(new CosmRefDto(cosm.getId(), cosm.getName()))")
    @Mapping(target = "publications", source = "products")
    CosmSummaryDto toSummary(Cosm cosm);
    @Mapping(target = "cosm", expression = "java(new CosmRefDto(cosm.getId(), cosm.getName()))")
    @Mapping(target = "publications", source = "products")
    CosmDetailDto toDetail(Cosm cosm);
}
