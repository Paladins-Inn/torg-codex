package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.TagDetailDto;
import de.paladinsinn.torg.codex.api.dto.TagSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.TagMapper;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.data.model.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {
    private final CatalogQuery<Tag> catalogQuery;
    private final TagMapper mapper;
    @GetMapping
    public List<TagSummaryDto> list() {
        return catalogQuery.findAll().stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<TagDetailDto> getById(@PathVariable UUID id) {
        return catalogQuery.findById(id).map(mapper::toDetail).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
