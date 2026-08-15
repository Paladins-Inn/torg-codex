package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.PerkGroupDetailDto;
import de.paladinsinn.torg.codex.api.dto.PerkGroupSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.PerkGroupMapper;
import de.paladinsinn.torg.codex.data.repository.PerkGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/perk-groups")
@RequiredArgsConstructor
public class PerkGroupController {
    private final PerkGroupRepository repository;
    private final PerkGroupMapper mapper;
    @GetMapping
    public List<PerkGroupSummaryDto> list() {
        return repository.findAll().stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<PerkGroupDetailDto> getById(@PathVariable UUID id) {
        return repository.findById(id).map(mapper::toDetail).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
