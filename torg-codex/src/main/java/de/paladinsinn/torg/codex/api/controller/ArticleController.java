package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.ArticleDetailDto;
import de.paladinsinn.torg.codex.api.dto.ArticleSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.ArticleMapper;
import de.paladinsinn.torg.codex.data.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {
    private final ArticleRepository repository;
    private final ArticleMapper mapper;
    @GetMapping
    public List<ArticleSummaryDto> list() {
        return repository.findAll().stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<ArticleDetailDto> getById(@PathVariable UUID id) {
        return repository.findById(id).map(mapper::toDetail).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
