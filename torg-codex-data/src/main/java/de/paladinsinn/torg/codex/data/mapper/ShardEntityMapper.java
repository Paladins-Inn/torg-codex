package de.paladinsinn.torg.codex.data.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between the {@code Shard} JPA entity and the framework-independent
 * {@code Shard} domain model. Censored text is sourced from the raw (un-censored)
 * accessors so the domain model carries the exact persisted value.
 */
@Mapper(componentModel = "spring", uses = ValueObjectMapper.class)
public interface ShardEntityMapper {

    @Mapping(target = "powers", source = "rawPowers")
    @Mapping(target = "purpose", source = "rawPurpose")
    @Mapping(target = "restrictions", source = "rawRestrictions")
    @Mapping(target = "text", source = "rawText")
    de.paladinsinn.torg.codex.domain.model.Shard toDomain(de.paladinsinn.torg.codex.data.model.Shard entity);

    @Mapping(target = "censor", ignore = true)
    de.paladinsinn.torg.codex.data.model.Shard toEntity(de.paladinsinn.torg.codex.domain.model.Shard model);
}
