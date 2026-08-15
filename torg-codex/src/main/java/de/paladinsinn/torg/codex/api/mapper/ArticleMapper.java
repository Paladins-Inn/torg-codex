package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.ArticleDetailDto;
import de.paladinsinn.torg.codex.api.dto.ArticleSummaryDto;
import de.paladinsinn.torg.codex.data.model.Article;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface ArticleMapper {
    @Mapping(target = "publications", expression = "java(java.util.List.of())")
    ArticleSummaryDto toSummary(Article article);
    @Mapping(target = "publications", expression = "java(java.util.List.of())")
    ArticleDetailDto toDetail(Article article);
}
