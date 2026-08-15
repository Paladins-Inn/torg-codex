package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.PowerDetailDto;
import de.paladinsinn.torg.codex.api.dto.PowerSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.PowerMapper;
import de.paladinsinn.torg.codex.data.repository.PowerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/powers")
@RequiredArgsConstructor
public class PowerController {
    private final PowerRepository repository;
    private final PowerMapper mapper;
    @GetMapping
    public List<PowerSummaryDto> list() {
        return repository.findAll().stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<PowerDetailDto> getById(@PathVariable UUID id) {
        return repository.findById(id).map(mapper::toDetail).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
