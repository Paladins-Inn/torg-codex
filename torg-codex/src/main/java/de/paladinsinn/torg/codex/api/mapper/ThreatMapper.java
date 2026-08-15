package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.ThreatDetailDto;
import de.paladinsinn.torg.codex.api.dto.ThreatSummaryDto;
import de.paladinsinn.torg.codex.domain.model.Threat;
import de.paladinsinn.torg.codex.data.markup.Censor;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface ThreatMapper {
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    ThreatSummaryDto toSummary(Threat threat);
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    @Mapping(target = "quote", qualifiedByName = "censorText")
    @Mapping(target = "text", qualifiedByName = "censorText")
    @Mapping(target = "specialAbilities", qualifiedByName = "censorMap")
    ThreatDetailDto toDetail(Threat threat, @Context Censor censor);
}
