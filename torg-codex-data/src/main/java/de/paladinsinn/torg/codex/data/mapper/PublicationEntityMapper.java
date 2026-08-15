package de.paladinsinn.torg.codex.data.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between the {@code Publication} JPA entity and the framework-independent
 * {@code Publication} domain model. Censored text is sourced from the raw (un-censored)
 * accessors so the domain model carries the exact persisted value.
 */
@Mapper(componentModel = "spring", uses = ValueObjectMapper.class)
public interface PublicationEntityMapper {

    de.paladinsinn.torg.codex.domain.model.Publication toDomain(de.paladinsinn.torg.codex.data.model.Publication entity);

    de.paladinsinn.torg.codex.data.model.Publication toEntity(de.paladinsinn.torg.codex.domain.model.Publication model);
}
