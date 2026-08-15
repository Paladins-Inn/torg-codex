package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.SpellListDetailDto;
import de.paladinsinn.torg.codex.api.dto.SpellListSummaryDto;
import de.paladinsinn.torg.codex.domain.model.SpellList;
import de.paladinsinn.torg.codex.data.markup.Censor;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface SpellListMapper {
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    SpellListSummaryDto toSummary(SpellList spellList);
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    @Mapping(target = "notes", qualifiedByName = "censorText")
    @Mapping(target = "text", qualifiedByName = "censorText")
    SpellListDetailDto toDetail(SpellList spellList, @Context Censor censor);
}
