package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.ArticleDetailDto;
import de.paladinsinn.torg.codex.api.dto.ArticleSummaryDto;
import de.paladinsinn.torg.codex.data.markup.Censor;
import de.paladinsinn.torg.codex.domain.model.Article;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface ArticleMapper {
    @Mapping(target = "publications", expression = "java(java.util.List.of())")
    ArticleSummaryDto toSummary(Article article);
    @Mapping(target = "publications", expression = "java(java.util.List.of())")
    @Mapping(target = "text", qualifiedByName = "censorText")
    ArticleDetailDto toDetail(Article article, @Context Censor censor);
}
