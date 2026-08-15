package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.ThreatDetailDto;
import de.paladinsinn.torg.codex.api.dto.ThreatSummaryDto;
import de.paladinsinn.torg.codex.data.model.Threat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface ThreatMapper {
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    ThreatSummaryDto toSummary(Threat threat);
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    ThreatDetailDto toDetail(Threat threat);
}
