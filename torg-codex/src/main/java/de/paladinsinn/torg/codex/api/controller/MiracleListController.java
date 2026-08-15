package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.MiracleListDetailDto;
import de.paladinsinn.torg.codex.api.dto.MiracleListSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.MiracleListMapper;
import de.paladinsinn.torg.codex.data.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.data.model.MiracleList;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/miracle-lists")
@RequiredArgsConstructor
public class MiracleListController {
    private final CatalogQuery<MiracleList> catalogQuery;
    private final MiracleListMapper mapper;
    @GetMapping
    public List<MiracleListSummaryDto> list(@RequestParam(required = false) String cosm) {
        final var entities = cosm != null ? catalogQuery.findByCosm(cosm) : catalogQuery.findAll();
        return entities.stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<MiracleListDetailDto> getById(@PathVariable UUID id) {
        return catalogQuery.findById(id).map(mapper::toDetail).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
