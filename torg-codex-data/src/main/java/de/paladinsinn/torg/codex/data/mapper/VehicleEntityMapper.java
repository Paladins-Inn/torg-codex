package de.paladinsinn.torg.codex.data.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between the {@code Vehicle} JPA entity and the framework-independent
 * {@code Vehicle} domain model. Censored text is sourced from the raw (un-censored)
 * accessors so the domain model carries the exact persisted value.
 */
@Mapper(componentModel = "spring", uses = ValueObjectMapper.class)
public interface VehicleEntityMapper {

    @Mapping(target = "text", source = "rawText")
    de.paladinsinn.torg.codex.domain.model.Vehicle toDomain(de.paladinsinn.torg.codex.data.model.Vehicle entity);

    @Mapping(target = "censor", ignore = true)
    de.paladinsinn.torg.codex.data.model.Vehicle toEntity(de.paladinsinn.torg.codex.domain.model.Vehicle model);
}
