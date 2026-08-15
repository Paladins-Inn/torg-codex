package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.MiracleDetailDto;
import de.paladinsinn.torg.codex.api.dto.MiracleSummaryDto;
import de.paladinsinn.torg.codex.domain.model.Miracle;
import de.paladinsinn.torg.codex.data.markup.Censor;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface MiracleMapper {
    @Mapping(target = "publications", source = "products")
    MiracleSummaryDto toSummary(Miracle miracle);
    @Mapping(target = "publications", source = "products")
    @Mapping(target = "dn", source = "dn")
    @Mapping(target = "text", qualifiedByName = "censorText")
    MiracleDetailDto toDetail(Miracle miracle, @Context Censor censor);
}
