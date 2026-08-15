package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.PowerListDetailDto;
import de.paladinsinn.torg.codex.api.dto.PowerListSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.PowerListMapper;
import de.paladinsinn.torg.codex.data.repository.PowerListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/power-lists")
@RequiredArgsConstructor
public class PowerListController {
    private final PowerListRepository repository;
    private final PowerListMapper mapper;
    @GetMapping
    public List<PowerListSummaryDto> list(@RequestParam(required = false) String cosm) {
        final var entities = cosm != null ? repository.findByCosm(cosm) : repository.findAll();
        return entities.stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<PowerListDetailDto> getById(@PathVariable UUID id) {
        return repository.findById(id).map(mapper::toDetail).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
