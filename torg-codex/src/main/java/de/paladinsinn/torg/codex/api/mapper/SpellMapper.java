package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.SpellDetailDto;
import de.paladinsinn.torg.codex.api.dto.SpellSummaryDto;
import de.paladinsinn.torg.codex.domain.model.Spell;
import de.paladinsinn.torg.codex.data.markup.Censor;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface SpellMapper {
    @Mapping(target = "publications", source = "products")
    SpellSummaryDto toSummary(Spell spell);
    @Mapping(target = "publications", source = "products")
    @Mapping(target = "dn", source = "dn")
    @Mapping(target = "text", qualifiedByName = "censorText")
    SpellDetailDto toDetail(Spell spell, @Context Censor censor);
}
