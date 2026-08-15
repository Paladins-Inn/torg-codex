package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.RaceDetailDto;
import de.paladinsinn.torg.codex.api.dto.RaceSummaryDto;
import de.paladinsinn.torg.codex.domain.model.Race;
import de.paladinsinn.torg.codex.data.markup.Censor;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface RaceMapper {
    @Mapping(target = "publications", source = "products")
    RaceSummaryDto toSummary(Race race);
    @Mapping(target = "publications", source = "products")
    @Mapping(target = "abilities", qualifiedByName = "censorText")
    @Mapping(target = "perkText", qualifiedByName = "censorText")
    @Mapping(target = "text", qualifiedByName = "censorText")
    RaceDetailDto toDetail(Race race, @Context Censor censor);
}
