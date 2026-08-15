package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.MiracleDetailDto;
import de.paladinsinn.torg.codex.api.dto.MiracleSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.MiracleMapper;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.api.security.CurrentUserCensorFactory;
import de.paladinsinn.torg.codex.data.markup.Censor;
import de.paladinsinn.torg.codex.domain.model.Miracle;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/miracles")
@RequiredArgsConstructor
public class MiracleController {
    private final CatalogQuery<Miracle> catalogQuery;
    private final MiracleMapper mapper;
    private final CurrentUserCensorFactory censorFactory;
    @GetMapping
    public List<MiracleSummaryDto> list() {
        return catalogQuery.findAll().stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<MiracleDetailDto> getById(@PathVariable UUID id) {
        final Censor censor = censorFactory.create();
        return catalogQuery.findById(id)
                .map(e -> mapper.toDetail(e, censor))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
