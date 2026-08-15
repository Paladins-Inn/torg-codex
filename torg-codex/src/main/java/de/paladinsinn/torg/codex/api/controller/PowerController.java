package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.PowerDetailDto;
import de.paladinsinn.torg.codex.api.dto.PowerSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.PowerMapper;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.api.security.CurrentUserCensorFactory;
import de.paladinsinn.torg.codex.data.markup.Censor;
import de.paladinsinn.torg.codex.domain.model.Power;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/powers")
@RequiredArgsConstructor
public class PowerController {
    private final CatalogQuery<Power> catalogQuery;
    private final PowerMapper mapper;
    private final CurrentUserCensorFactory censorFactory;
    @GetMapping
    public List<PowerSummaryDto> list() {
        return catalogQuery.findAll().stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<PowerDetailDto> getById(@PathVariable UUID id) {
        final Censor censor = censorFactory.create();
        return catalogQuery.findById(id)
                .map(e -> mapper.toDetail(e, censor))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
