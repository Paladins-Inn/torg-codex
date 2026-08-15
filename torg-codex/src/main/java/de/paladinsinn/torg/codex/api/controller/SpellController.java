package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.SpellDetailDto;
import de.paladinsinn.torg.codex.api.dto.SpellSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.SpellMapper;
import de.paladinsinn.torg.codex.data.repository.SpellRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/spells")
@RequiredArgsConstructor
public class SpellController {
    private final SpellRepository repository;
    private final SpellMapper mapper;
    @GetMapping
    public List<SpellSummaryDto> list() {
        return repository.findAll().stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<SpellDetailDto> getById(@PathVariable UUID id) {
        return repository.findById(id).map(mapper::toDetail).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
