package de.paladinsinn.torg.codex.data.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between the {@code Threat} JPA entity and the framework-independent
 * {@code Threat} domain model. Censored text is sourced from the raw (un-censored)
 * accessors so the domain model carries the exact persisted value.
 */
@Mapper(componentModel = "spring", uses = ValueObjectMapper.class)
public interface ThreatEntityMapper {

    @Mapping(target = "quote", source = "rawQuote")
    @Mapping(target = "specialAbilities", source = "rawSpecialAbilities")
    @Mapping(target = "text", source = "rawText")
    de.paladinsinn.torg.codex.domain.model.Threat toDomain(de.paladinsinn.torg.codex.data.model.Threat entity);

    @Mapping(target = "censor", ignore = true)
    de.paladinsinn.torg.codex.data.model.Threat toEntity(de.paladinsinn.torg.codex.domain.model.Threat model);
}
