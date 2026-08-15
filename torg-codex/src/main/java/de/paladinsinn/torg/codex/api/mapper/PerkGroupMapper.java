package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.PerkGroupDetailDto;
import de.paladinsinn.torg.codex.api.dto.PerkGroupSummaryDto;
import de.paladinsinn.torg.codex.data.model.PerkGroup;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface PerkGroupMapper {
    @Mapping(target = "publications", source = "products")
    PerkGroupSummaryDto toSummary(PerkGroup perkGroup);
    @Mapping(target = "publications", source = "products")
    PerkGroupDetailDto toDetail(PerkGroup perkGroup);
}
