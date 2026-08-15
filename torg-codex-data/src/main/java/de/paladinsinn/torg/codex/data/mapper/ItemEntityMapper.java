package de.paladinsinn.torg.codex.data.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between the {@code Item} JPA entity and the framework-independent
 * {@code Item} domain model. Censored text is sourced from the raw (un-censored)
 * accessors so the domain model carries the exact persisted value.
 */
@Mapper(componentModel = "spring", uses = ValueObjectMapper.class)
public interface ItemEntityMapper {

    @Mapping(target = "additionalFeatures", source = "rawAdditionalFeatures")
    @Mapping(target = "text", source = "rawText")
    de.paladinsinn.torg.codex.domain.model.Item toDomain(de.paladinsinn.torg.codex.data.model.Item entity);

    @Mapping(target = "censor", ignore = true)
    de.paladinsinn.torg.codex.data.model.Item toEntity(de.paladinsinn.torg.codex.domain.model.Item model);
}
