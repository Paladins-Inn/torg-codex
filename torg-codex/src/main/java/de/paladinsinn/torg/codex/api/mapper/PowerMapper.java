package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.PowerDetailDto;
import de.paladinsinn.torg.codex.api.dto.PowerSummaryDto;
import de.paladinsinn.torg.codex.data.model.Power;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface PowerMapper {
    @Mapping(target = "publications", source = "products")
    PowerSummaryDto toSummary(Power power);
    @Mapping(target = "publications", source = "products")
    @Mapping(target = "dn", source = "dn")
    PowerDetailDto toDetail(Power power);
}
