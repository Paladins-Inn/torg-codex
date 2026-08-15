package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.ItemDetailDto;
import de.paladinsinn.torg.codex.api.dto.ItemSummaryDto;
import de.paladinsinn.torg.codex.data.model.Item;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface ItemMapper {
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    ItemSummaryDto toSummary(Item item);
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    ItemDetailDto toDetail(Item item);
}
