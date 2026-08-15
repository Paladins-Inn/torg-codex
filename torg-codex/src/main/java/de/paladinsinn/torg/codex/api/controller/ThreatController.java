package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.ThreatDetailDto;
import de.paladinsinn.torg.codex.api.dto.ThreatSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.ThreatMapper;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.api.security.CurrentUserCensorFactory;
import de.paladinsinn.torg.codex.data.markup.Censor;
import de.paladinsinn.torg.codex.domain.model.Threat;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/threats")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThreatController {
    private final CatalogQuery<Threat> catalogQuery;
    private final ThreatMapper mapper;
    private final CurrentUserCensorFactory censorFactory;
    @GetMapping
    public List<ThreatSummaryDto> list(@RequestParam(required = false) String cosm) {
        final var results = cosm != null ? catalogQuery.findByCosm(cosm) : catalogQuery.findAll();
        return results.stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<ThreatDetailDto> getById(@PathVariable UUID id) {
        final Censor censor = censorFactory.create();
        return catalogQuery.findById(id)
                .map(e -> mapper.toDetail(e, censor))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
