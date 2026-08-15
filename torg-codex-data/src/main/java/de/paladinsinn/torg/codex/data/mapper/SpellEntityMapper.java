package de.paladinsinn.torg.codex.data.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between the {@code Spell} JPA entity and the framework-independent
 * {@code Spell} domain model. Censored text is sourced from the raw (un-censored)
 * accessors so the domain model carries the exact persisted value.
 */
@Mapper(componentModel = "spring", uses = ValueObjectMapper.class)
public interface SpellEntityMapper {

    @Mapping(target = "text", source = "rawText")
    de.paladinsinn.torg.codex.domain.model.Spell toDomain(de.paladinsinn.torg.codex.data.model.Spell entity);

    @Mapping(target = "censor", ignore = true)
    de.paladinsinn.torg.codex.data.model.Spell toEntity(de.paladinsinn.torg.codex.domain.model.Spell model);
}
