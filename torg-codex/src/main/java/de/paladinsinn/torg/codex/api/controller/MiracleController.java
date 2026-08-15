package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.MiracleDetailDto;
import de.paladinsinn.torg.codex.api.dto.MiracleSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.MiracleMapper;
import de.paladinsinn.torg.codex.data.repository.MiracleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/miracles")
@RequiredArgsConstructor
public class MiracleController {
    private final MiracleRepository repository;
    private final MiracleMapper mapper;
    @GetMapping
    public List<MiracleSummaryDto> list() {
        return repository.findAll().stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<MiracleDetailDto> getById(@PathVariable UUID id) {
        return repository.findById(id).map(mapper::toDetail).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
