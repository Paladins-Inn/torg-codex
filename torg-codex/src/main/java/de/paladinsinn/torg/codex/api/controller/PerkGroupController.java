package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.PerkGroupDetailDto;
import de.paladinsinn.torg.codex.api.dto.PerkGroupSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.PerkGroupMapper;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.api.security.CurrentUserCensorFactory;
import de.paladinsinn.torg.codex.data.markup.Censor;
import de.paladinsinn.torg.codex.domain.model.PerkGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/perk-groups")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerkGroupController {
    private final CatalogQuery<PerkGroup> catalogQuery;
    private final PerkGroupMapper mapper;
    private final CurrentUserCensorFactory censorFactory;
    @GetMapping
    public List<PerkGroupSummaryDto> list() {
        return catalogQuery.findAll().stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<PerkGroupDetailDto> getById(@PathVariable UUID id) {
        final Censor censor = censorFactory.create();
        return catalogQuery.findById(id)
                .map(e -> mapper.toDetail(e, censor))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
