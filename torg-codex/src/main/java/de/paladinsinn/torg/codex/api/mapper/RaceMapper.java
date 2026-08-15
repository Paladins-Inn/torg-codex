package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.RaceDetailDto;
import de.paladinsinn.torg.codex.api.dto.RaceSummaryDto;
import de.paladinsinn.torg.codex.data.model.Race;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface RaceMapper {
    @Mapping(target = "publications", source = "products")
    RaceSummaryDto toSummary(Race race);
    @Mapping(target = "publications", source = "products")
    RaceDetailDto toDetail(Race race);
}
