package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.ShardDetailDto;
import de.paladinsinn.torg.codex.api.dto.ShardSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.ShardMapper;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.api.security.CurrentUserCensorFactory;
import de.paladinsinn.torg.codex.data.markup.Censor;
import de.paladinsinn.torg.codex.domain.model.Shard;
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
    private final CurrentUserCensorFactory censorFactory;
    @GetMapping
    public List<ShardSummaryDto> list(@RequestParam(required = false) String cosm) {
        final var results = cosm != null ? catalogQuery.findByCosm(cosm) : catalogQuery.findAll();
        return results.stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<ShardDetailDto> getById(@PathVariable UUID id) {
        final Censor censor = censorFactory.create();
        return catalogQuery.findById(id)
                .map(e -> mapper.toDetail(e, censor))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
