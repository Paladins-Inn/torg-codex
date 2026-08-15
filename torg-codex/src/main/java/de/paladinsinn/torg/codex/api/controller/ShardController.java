package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.ShardDetailDto;
import de.paladinsinn.torg.codex.api.dto.ShardSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.ShardMapper;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.data.model.Shard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/shards")
@RequiredArgsConstructor
public class ShardController {
    private final CatalogQuery<Shard> catalogQuery;
    private final ShardMapper mapper;
    @GetMapping
    public List<ShardSummaryDto> list(@RequestParam(required = false) String cosm) {
        final var entities = cosm != null ? catalogQuery.findByCosm(cosm) : catalogQuery.findAll();
        return entities.stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<ShardDetailDto> getById(@PathVariable UUID id) {
        return catalogQuery.findById(id).map(mapper::toDetail).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
