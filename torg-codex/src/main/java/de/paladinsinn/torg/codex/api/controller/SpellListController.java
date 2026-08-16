package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.SpellListDetailDto;
import de.paladinsinn.torg.codex.api.dto.SpellListSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.SpellListMapper;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.api.security.CurrentUserCensorFactory;
import de.paladinsinn.torg.codex.domain.markup.Censor;
import de.paladinsinn.torg.codex.domain.model.SpellList;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/spell-lists")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpellListController {
    private final CatalogQuery<SpellList> catalogQuery;
    private final SpellListMapper mapper;
    private final CurrentUserCensorFactory censorFactory;
    @GetMapping
    public List<SpellListSummaryDto> list(@RequestParam(required = false) String cosm) {
        final var results = cosm != null ? catalogQuery.findByCosm(cosm) : catalogQuery.findAll();
        return results.stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<SpellListDetailDto> getById(@PathVariable UUID id) {
        final Censor censor = censorFactory.create();
        return catalogQuery.findById(id)
                .map(e -> mapper.toDetail(e, censor))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
