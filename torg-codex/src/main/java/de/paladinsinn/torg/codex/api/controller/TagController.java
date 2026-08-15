package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.TagDetailDto;
import de.paladinsinn.torg.codex.api.dto.TagSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.TagMapper;
import de.paladinsinn.torg.codex.data.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {
    private final TagRepository repository;
    private final TagMapper mapper;
    @GetMapping
    public List<TagSummaryDto> list() {
        return repository.findAll().stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<TagDetailDto> getById(@PathVariable UUID id) {
        return repository.findById(id).map(mapper::toDetail).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
