package de.paladinsinn.torg.codex.data.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between the {@code Tag} JPA entity and the framework-independent
 * {@code Tag} domain model. Censored text is sourced from the raw (un-censored)
 * accessors so the domain model carries the exact persisted value.
 */
@Mapper(componentModel = "spring", uses = ValueObjectMapper.class)
public interface TagEntityMapper {

    de.paladinsinn.torg.codex.domain.model.Tag toDomain(de.paladinsinn.torg.codex.data.model.Tag entity);

    @Mapping(target = "censor", ignore = true)
    de.paladinsinn.torg.codex.data.model.Tag toEntity(de.paladinsinn.torg.codex.domain.model.Tag model);
}
