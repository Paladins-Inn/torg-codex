package de.paladinsinn.torg.codex.data.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between the {@code Race} JPA entity and the framework-independent
 * {@code Race} domain model. Censored text is sourced from the raw (un-censored)
 * accessors so the domain model carries the exact persisted value.
 */
@Mapper(componentModel = "spring", uses = ValueObjectMapper.class)
public interface RaceEntityMapper {

    @Mapping(target = "abilities", source = "rawAbilities")
    @Mapping(target = "perkText", source = "rawPerkText")
    @Mapping(target = "text", source = "rawText")
    de.paladinsinn.torg.codex.domain.model.Race toDomain(de.paladinsinn.torg.codex.data.model.Race entity);

    @Mapping(target = "censor", ignore = true)
    de.paladinsinn.torg.codex.data.model.Race toEntity(de.paladinsinn.torg.codex.domain.model.Race model);
}
