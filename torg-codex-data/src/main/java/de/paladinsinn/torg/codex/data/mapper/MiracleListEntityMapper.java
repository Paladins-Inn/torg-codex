package de.paladinsinn.torg.codex.data.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between the {@code MiracleList} JPA entity and the framework-independent
 * {@code MiracleList} domain model. Censored text is sourced from the raw (un-censored)
 * accessors so the domain model carries the exact persisted value.
 */
@Mapper(componentModel = "spring", uses = ValueObjectMapper.class)
public interface MiracleListEntityMapper {

    @Mapping(target = "notes", source = "rawNotes")
    @Mapping(target = "text", source = "rawText")
    de.paladinsinn.torg.codex.domain.model.MiracleList toDomain(de.paladinsinn.torg.codex.data.model.MiracleList entity);

    @Mapping(target = "censor", ignore = true)
    de.paladinsinn.torg.codex.data.model.MiracleList toEntity(de.paladinsinn.torg.codex.domain.model.MiracleList model);
}
