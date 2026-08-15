package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.PublicationDetailDto;
import de.paladinsinn.torg.codex.api.dto.PublicationSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.PublicationMapper;
import de.paladinsinn.torg.codex.data.repository.PublicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/publications")
@RequiredArgsConstructor
public class PublicationController {
    private final PublicationRepository repository;
    private final PublicationMapper mapper;
    @GetMapping
    public List<PublicationSummaryDto> list() {
        return repository.findAll().stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<PublicationDetailDto> getById(@PathVariable UUID id) {
        return repository.findById(id).map(mapper::toDetail).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
