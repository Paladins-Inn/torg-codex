package de.paladinsinn.torg.codex.data.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between the {@code PerkGroup} JPA entity and the framework-independent
 * {@code PerkGroup} domain model. Censored text is sourced from the raw (un-censored)
 * accessors so the domain model carries the exact persisted value.
 */
@Mapper(componentModel = "spring", uses = ValueObjectMapper.class)
public interface PerkGroupEntityMapper {

    @Mapping(target = "infos", source = "rawInfos")
    @Mapping(target = "text", source = "rawText")
    de.paladinsinn.torg.codex.domain.model.PerkGroup toDomain(de.paladinsinn.torg.codex.data.model.PerkGroup entity);

    @Mapping(target = "censor", ignore = true)
    de.paladinsinn.torg.codex.data.model.PerkGroup toEntity(de.paladinsinn.torg.codex.domain.model.PerkGroup model);
}
