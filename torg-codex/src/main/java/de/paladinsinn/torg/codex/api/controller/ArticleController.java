package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.ArticleDetailDto;
import de.paladinsinn.torg.codex.api.dto.ArticleSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.ArticleMapper;
import de.paladinsinn.torg.codex.api.security.CurrentUserCensorFactory;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.data.markup.Censor;
import de.paladinsinn.torg.codex.domain.model.Article;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleController {
    private final CatalogQuery<Article> catalogQuery;
    private final ArticleMapper mapper;
    private final CurrentUserCensorFactory censorFactory;
    @GetMapping
    public List<ArticleSummaryDto> list() {
        return catalogQuery.findAll().stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<ArticleDetailDto> getById(@PathVariable UUID id) {
        final Censor censor = censorFactory.create();
        return catalogQuery.findById(id).map(a -> mapper.toDetail(a, censor)).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
