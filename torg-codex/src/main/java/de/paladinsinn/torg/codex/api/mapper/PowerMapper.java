package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.PowerDetailDto;
import de.paladinsinn.torg.codex.api.dto.PowerSummaryDto;
import de.paladinsinn.torg.codex.domain.model.Power;
import de.paladinsinn.torg.codex.data.markup.Censor;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface PowerMapper {
    @Mapping(target = "publications", source = "products")
    PowerSummaryDto toSummary(Power power);
    @Mapping(target = "publications", source = "products")
    @Mapping(target = "dn", source = "dn")
    @Mapping(target = "enhancements", qualifiedByName = "censorText")
    @Mapping(target = "limitations", qualifiedByName = "censorText")
    @Mapping(target = "text", qualifiedByName = "censorText")
    PowerDetailDto toDetail(Power power, @Context Censor censor);
}
