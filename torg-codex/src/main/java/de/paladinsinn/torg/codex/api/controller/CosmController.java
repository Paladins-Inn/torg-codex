package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.CosmDetailDto;
import de.paladinsinn.torg.codex.api.dto.CosmSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.CosmMapper;
import de.paladinsinn.torg.codex.data.repository.CosmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/cosms")
@RequiredArgsConstructor
public class CosmController {
    private final CosmRepository repository;
    private final CosmMapper mapper;
    @GetMapping
    public List<CosmSummaryDto> list() {
        return repository.findAll().stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<CosmDetailDto> getById(@PathVariable UUID id) {
        return repository.findById(id).map(mapper::toDetail).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
