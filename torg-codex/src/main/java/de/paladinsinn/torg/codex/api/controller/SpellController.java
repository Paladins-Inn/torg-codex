package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.SpellDetailDto;
import de.paladinsinn.torg.codex.api.dto.SpellSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.SpellMapper;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.api.security.CurrentUserCensorFactory;
import de.paladinsinn.torg.codex.data.markup.Censor;
import de.paladinsinn.torg.codex.domain.model.Spell;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/spells")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpellController {
    private final CatalogQuery<Spell> catalogQuery;
    private final SpellMapper mapper;
    private final CurrentUserCensorFactory censorFactory;
    @GetMapping
    public List<SpellSummaryDto> list() {
        return catalogQuery.findAll().stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<SpellDetailDto> getById(@PathVariable UUID id) {
        final Censor censor = censorFactory.create();
        return catalogQuery.findById(id)
                .map(e -> mapper.toDetail(e, censor))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
