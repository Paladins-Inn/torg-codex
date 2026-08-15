package de.paladinsinn.torg.codex.data.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between the {@code Perk} JPA entity and the framework-independent
 * {@code Perk} domain model. Censored text is sourced from the raw (un-censored)
 * accessors so the domain model carries the exact persisted value.
 */
@Mapper(componentModel = "spring", uses = ValueObjectMapper.class)
public interface PerkEntityMapper {

    @Mapping(target = "prerequisites", source = "rawPrerequisites")
    @Mapping(target = "text", source = "rawText")
    de.paladinsinn.torg.codex.domain.model.Perk toDomain(de.paladinsinn.torg.codex.data.model.Perk entity);

    @Mapping(target = "censor", ignore = true)
    de.paladinsinn.torg.codex.data.model.Perk toEntity(de.paladinsinn.torg.codex.domain.model.Perk model);
}
