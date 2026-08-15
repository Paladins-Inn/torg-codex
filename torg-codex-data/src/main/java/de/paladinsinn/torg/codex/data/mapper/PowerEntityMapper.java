package de.paladinsinn.torg.codex.data.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between the {@code Power} JPA entity and the framework-independent
 * {@code Power} domain model. Censored text is sourced from the raw (un-censored)
 * accessors so the domain model carries the exact persisted value.
 */
@Mapper(componentModel = "spring", uses = ValueObjectMapper.class)
public interface PowerEntityMapper {

    @Mapping(target = "enhancements", source = "rawEnhancements")
    @Mapping(target = "limitations", source = "rawLimitations")
    @Mapping(target = "text", source = "rawText")
    de.paladinsinn.torg.codex.domain.model.Power toDomain(de.paladinsinn.torg.codex.data.model.Power entity);

    @Mapping(target = "censor", ignore = true)
    de.paladinsinn.torg.codex.data.model.Power toEntity(de.paladinsinn.torg.codex.domain.model.Power model);
}
