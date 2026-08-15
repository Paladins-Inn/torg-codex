package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.SpellListDetailDto;
import de.paladinsinn.torg.codex.api.dto.SpellListSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.SpellListMapper;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.data.model.SpellList;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/spell-lists")
@RequiredArgsConstructor
public class SpellListController {
    private final CatalogQuery<SpellList> catalogQuery;
    private final SpellListMapper mapper;
    @GetMapping
    public List<SpellListSummaryDto> list(@RequestParam(required = false) String cosm) {
        final var entities = cosm != null ? catalogQuery.findByCosm(cosm) : catalogQuery.findAll();
        return entities.stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<SpellListDetailDto> getById(@PathVariable UUID id) {
        return catalogQuery.findById(id).map(mapper::toDetail).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
