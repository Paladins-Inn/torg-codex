package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.ShardDetailDto;
import de.paladinsinn.torg.codex.api.dto.ShardSummaryDto;
import de.paladinsinn.torg.codex.domain.model.Shard;
import de.paladinsinn.torg.codex.data.markup.Censor;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface ShardMapper {
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    ShardSummaryDto toSummary(Shard shard);
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    @Mapping(target = "powers", qualifiedByName = "censorText")
    @Mapping(target = "purpose", qualifiedByName = "censorText")
    @Mapping(target = "restrictions", qualifiedByName = "censorText")
    @Mapping(target = "text", qualifiedByName = "censorText")
    ShardDetailDto toDetail(Shard shard, @Context Censor censor);
}
