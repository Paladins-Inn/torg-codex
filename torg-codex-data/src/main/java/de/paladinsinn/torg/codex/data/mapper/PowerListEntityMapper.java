package de.paladinsinn.torg.codex.data.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between the {@code PowerList} JPA entity and the framework-independent
 * {@code PowerList} domain model. Censored text is sourced from the raw (un-censored)
 * accessors so the domain model carries the exact persisted value.
 */
@Mapper(componentModel = "spring", uses = ValueObjectMapper.class)
public interface PowerListEntityMapper {

    @Mapping(target = "notes", source = "rawNotes")
    @Mapping(target = "text", source = "rawText")
    de.paladinsinn.torg.codex.domain.model.PowerList toDomain(de.paladinsinn.torg.codex.data.model.PowerList entity);

    @Mapping(target = "censor", ignore = true)
    de.paladinsinn.torg.codex.data.model.PowerList toEntity(de.paladinsinn.torg.codex.domain.model.PowerList model);
}
