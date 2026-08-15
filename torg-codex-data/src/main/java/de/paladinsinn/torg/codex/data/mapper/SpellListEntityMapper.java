package de.paladinsinn.torg.codex.data.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between the {@code SpellList} JPA entity and the framework-independent
 * {@code SpellList} domain model. Censored text is sourced from the raw (un-censored)
 * accessors so the domain model carries the exact persisted value.
 */
@Mapper(componentModel = "spring", uses = ValueObjectMapper.class)
public interface SpellListEntityMapper {

    @Mapping(target = "notes", source = "rawNotes")
    @Mapping(target = "text", source = "rawText")
    de.paladinsinn.torg.codex.domain.model.SpellList toDomain(de.paladinsinn.torg.codex.data.model.SpellList entity);

    @Mapping(target = "censor", ignore = true)
    de.paladinsinn.torg.codex.data.model.SpellList toEntity(de.paladinsinn.torg.codex.domain.model.SpellList model);
}
