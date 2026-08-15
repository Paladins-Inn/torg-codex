package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.CosmDetailDto;
import de.paladinsinn.torg.codex.api.dto.CosmSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.CosmMapper;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.api.security.CurrentUserCensorFactory;
import de.paladinsinn.torg.codex.data.markup.Censor;
import de.paladinsinn.torg.codex.domain.model.Cosm;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/cosms")
@RequiredArgsConstructor
public class CosmController {
    private final CatalogQuery<Cosm> catalogQuery;
    private final CosmMapper mapper;
    private final CurrentUserCensorFactory censorFactory;
    @GetMapping
    public List<CosmSummaryDto> list() {
        return catalogQuery.findAll().stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<CosmDetailDto> getById(@PathVariable UUID id) {
        final Censor censor = censorFactory.create();
        return catalogQuery.findById(id)
                .map(e -> mapper.toDetail(e, censor))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
