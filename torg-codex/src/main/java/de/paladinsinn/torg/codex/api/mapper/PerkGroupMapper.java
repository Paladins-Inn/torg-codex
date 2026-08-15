package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.PerkGroupDetailDto;
import de.paladinsinn.torg.codex.api.dto.PerkGroupSummaryDto;
import de.paladinsinn.torg.codex.domain.model.PerkGroup;
import de.paladinsinn.torg.codex.data.markup.Censor;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface PerkGroupMapper {
    @Mapping(target = "publications", source = "products")
    PerkGroupSummaryDto toSummary(PerkGroup perkGroup);
    @Mapping(target = "publications", source = "products")
    @Mapping(target = "infos", qualifiedByName = "censorText")
    @Mapping(target = "text", qualifiedByName = "censorText")
    PerkGroupDetailDto toDetail(PerkGroup perkGroup, @Context Censor censor);
}
