package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.ShardDetailDto;
import de.paladinsinn.torg.codex.api.dto.ShardSummaryDto;
import de.paladinsinn.torg.codex.data.model.Shard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface ShardMapper {
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    ShardSummaryDto toSummary(Shard shard);
    @Mapping(target = "cosm", source = "cosm")
    @Mapping(target = "publications", source = "products")
    ShardDetailDto toDetail(Shard shard);
}
