package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.VehicleDetailDto;
import de.paladinsinn.torg.codex.api.dto.VehicleSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.VehicleMapper;
import de.paladinsinn.torg.codex.data.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {
    private final VehicleRepository repository;
    private final VehicleMapper mapper;
    @GetMapping
    public List<VehicleSummaryDto> list(@RequestParam(required = false) String cosm) {
        final var entities = cosm != null ? repository.findByCosm(cosm) : repository.findAll();
        return entities.stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<VehicleDetailDto> getById(@PathVariable UUID id) {
        return repository.findById(id).map(mapper::toDetail).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
