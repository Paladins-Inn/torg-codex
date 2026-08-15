package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.ThreatDetailDto;
import de.paladinsinn.torg.codex.api.dto.ThreatSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.ThreatMapper;
import de.paladinsinn.torg.codex.data.repository.ThreatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/threats")
@RequiredArgsConstructor
public class ThreatController {
    private final ThreatRepository repository;
    private final ThreatMapper mapper;
    @GetMapping
    public List<ThreatSummaryDto> list(@RequestParam(required = false) String cosm) {
        final var entities = cosm != null ? repository.findByCosm(cosm) : repository.findAll();
        return entities.stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<ThreatDetailDto> getById(@PathVariable UUID id) {
        return repository.findById(id).map(mapper::toDetail).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
