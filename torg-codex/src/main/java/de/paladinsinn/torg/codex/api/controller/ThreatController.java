package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.ThreatDetailDto;
import de.paladinsinn.torg.codex.api.dto.ThreatSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.ThreatMapper;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.data.model.Threat;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/threats")
@RequiredArgsConstructor
public class ThreatController {
    private final CatalogQuery<Threat> catalogQuery;
    private final ThreatMapper mapper;
    @GetMapping
    public List<ThreatSummaryDto> list(@RequestParam(required = false) String cosm) {
        final var entities = cosm != null ? catalogQuery.findByCosm(cosm) : catalogQuery.findAll();
        return entities.stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<ThreatDetailDto> getById(@PathVariable UUID id) {
        return catalogQuery.findById(id).map(mapper::toDetail).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
