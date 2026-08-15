package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.SpellDetailDto;
import de.paladinsinn.torg.codex.api.dto.SpellSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.SpellMapper;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.data.model.Spell;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/spells")
@RequiredArgsConstructor
public class SpellController {
    private final CatalogQuery<Spell> catalogQuery;
    private final SpellMapper mapper;
    @GetMapping
    public List<SpellSummaryDto> list() {
        return catalogQuery.findAll().stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<SpellDetailDto> getById(@PathVariable UUID id) {
        return catalogQuery.findById(id).map(mapper::toDetail).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
