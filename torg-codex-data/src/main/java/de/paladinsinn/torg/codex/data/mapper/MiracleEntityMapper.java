package de.paladinsinn.torg.codex.data.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between the {@code Miracle} JPA entity and the framework-independent
 * {@code Miracle} domain model. Censored text is sourced from the raw (un-censored)
 * accessors so the domain model carries the exact persisted value.
 */
@Mapper(componentModel = "spring", uses = ValueObjectMapper.class)
public interface MiracleEntityMapper {

    @Mapping(target = "text", source = "rawText")
    de.paladinsinn.torg.codex.domain.model.Miracle toDomain(de.paladinsinn.torg.codex.data.model.Miracle entity);

    @Mapping(target = "censor", ignore = true)
    de.paladinsinn.torg.codex.data.model.Miracle toEntity(de.paladinsinn.torg.codex.domain.model.Miracle model);
}
