package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.RaceDetailDto;
import de.paladinsinn.torg.codex.api.dto.RaceSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.RaceMapper;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.data.model.Race;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/races")
@RequiredArgsConstructor
public class RaceController {
    private final CatalogQuery<Race> catalogQuery;
    private final RaceMapper mapper;
    @GetMapping
    public List<RaceSummaryDto> list() {
        return catalogQuery.findAll().stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<RaceDetailDto> getById(@PathVariable UUID id) {
        return catalogQuery.findById(id).map(mapper::toDetail).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
