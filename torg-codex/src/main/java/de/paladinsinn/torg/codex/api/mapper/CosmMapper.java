package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.CosmDetailDto;
import de.paladinsinn.torg.codex.api.dto.CosmSummaryDto;
import de.paladinsinn.torg.codex.api.dto.CosmRefDto;
import de.paladinsinn.torg.codex.domain.model.Cosm;
import de.paladinsinn.torg.codex.data.markup.Censor;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface CosmMapper {
    @Mapping(target = "cosm", expression = "java(new CosmRefDto(cosm.getId(), cosm.getName()))")
    @Mapping(target = "publications", source = "products")
    CosmSummaryDto toSummary(Cosm cosm);
    @Mapping(target = "cosm", expression = "java(new CosmRefDto(cosm.getId(), cosm.getName()))")
    @Mapping(target = "publications", source = "products")
    @Mapping(target = "text", qualifiedByName = "censorText")
    @Mapping(target = "worldLaws", qualifiedByName = "censorText")
    CosmDetailDto toDetail(Cosm cosm, @Context Censor censor);
}
