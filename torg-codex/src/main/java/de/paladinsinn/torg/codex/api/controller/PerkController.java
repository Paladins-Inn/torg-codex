package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.PerkDetailDto;
import de.paladinsinn.torg.codex.api.dto.PerkSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.PerkMapper;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.api.security.CurrentUserCensorFactory;
import de.paladinsinn.torg.codex.data.markup.Censor;
import de.paladinsinn.torg.codex.domain.model.Perk;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/perks")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerkController {
    private final CatalogQuery<Perk> catalogQuery;
    private final PerkMapper mapper;
    private final CurrentUserCensorFactory censorFactory;
    @GetMapping
    public List<PerkSummaryDto> list(@RequestParam(required = false) String cosm) {
        final var results = cosm != null ? catalogQuery.findByCosm(cosm) : catalogQuery.findAll();
        return results.stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<PerkDetailDto> getById(@PathVariable UUID id) {
        final Censor censor = censorFactory.create();
        return catalogQuery.findById(id)
                .map(e -> mapper.toDetail(e, censor))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
