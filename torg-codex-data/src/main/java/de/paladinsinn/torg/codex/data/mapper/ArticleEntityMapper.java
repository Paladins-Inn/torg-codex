package de.paladinsinn.torg.codex.data.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between the {@code Article} JPA entity and the framework-independent
 * {@code Article} domain model. Text is sourced from the raw (un-censored) accessor so the
 * domain model carries the exact persisted value.
 */
@Mapper(componentModel = "spring", uses = ValueObjectMapper.class)
public interface ArticleEntityMapper {

    @Mapping(target = "text", source = "rawText")
    de.paladinsinn.torg.codex.domain.model.Article toDomain(
            de.paladinsinn.torg.codex.data.model.Article entity);

    @Mapping(target = "censor", ignore = true)
    de.paladinsinn.torg.codex.data.model.Article toEntity(
            de.paladinsinn.torg.codex.domain.model.Article model);
}
