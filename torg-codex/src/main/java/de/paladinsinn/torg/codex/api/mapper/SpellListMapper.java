package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.SpellListDetailDto;
import de.paladinsinn.torg.codex.api.dto.SpellListSummaryDto;
import de.paladinsinn.torg.codex.data.model.SpellList;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface SpellListMapper {
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    SpellListSummaryDto toSummary(SpellList spellList);
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    SpellListDetailDto toDetail(SpellList spellList);
}
