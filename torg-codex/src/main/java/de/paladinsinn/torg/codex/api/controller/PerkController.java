package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.PerkDetailDto;
import de.paladinsinn.torg.codex.api.dto.PerkSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.PerkMapper;
import de.paladinsinn.torg.codex.data.repository.PerkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/perks")
@RequiredArgsConstructor
public class PerkController {
    private final PerkRepository repository;
    private final PerkMapper mapper;
    @GetMapping
    public List<PerkSummaryDto> list(@RequestParam(required = false) String cosm) {
        final var entities = cosm != null ? repository.findByCosm(cosm) : repository.findAll();
        return entities.stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<PerkDetailDto> getById(@PathVariable UUID id) {
        return repository.findById(id).map(mapper::toDetail).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
