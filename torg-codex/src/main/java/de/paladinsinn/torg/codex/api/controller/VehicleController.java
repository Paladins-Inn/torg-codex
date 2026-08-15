package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.VehicleDetailDto;
import de.paladinsinn.torg.codex.api.dto.VehicleSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.VehicleMapper;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.data.model.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {
    private final CatalogQuery<Vehicle> catalogQuery;
    private final VehicleMapper mapper;
    @GetMapping
    public List<VehicleSummaryDto> list(@RequestParam(required = false) String cosm) {
        final var entities = cosm != null ? catalogQuery.findByCosm(cosm) : catalogQuery.findAll();
        return entities.stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<VehicleDetailDto> getById(@PathVariable UUID id) {
        return catalogQuery.findById(id).map(mapper::toDetail).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
