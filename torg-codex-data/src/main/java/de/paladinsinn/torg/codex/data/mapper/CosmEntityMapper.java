package de.paladinsinn.torg.codex.data.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between the {@code Cosm} JPA entity and the framework-independent
 * {@code Cosm} domain model. Censored text is sourced from the raw (un-censored)
 * accessors so the domain model carries the exact persisted value.
 */
@Mapper(componentModel = "spring", uses = ValueObjectMapper.class)
public interface CosmEntityMapper {

    @Mapping(target = "text", source = "rawText")
    @Mapping(target = "worldLaws", source = "rawWorldLaws")
    de.paladinsinn.torg.codex.domain.model.Cosm toDomain(de.paladinsinn.torg.codex.data.model.Cosm entity);

    @Mapping(target = "censor", ignore = true)
    de.paladinsinn.torg.codex.data.model.Cosm toEntity(de.paladinsinn.torg.codex.domain.model.Cosm model);
}
