package de.paladinsinn.torg.codex.api.controller;
import de.paladinsinn.torg.codex.api.dto.ItemDetailDto;
import de.paladinsinn.torg.codex.api.dto.ItemSummaryDto;
import de.paladinsinn.torg.codex.api.mapper.ItemMapper;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.data.model.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {
    private final CatalogQuery<Item> catalogQuery;
    private final ItemMapper mapper;
    @GetMapping
    public List<ItemSummaryDto> list(@RequestParam(required = false) String cosm) {
        final var entities = cosm != null ? catalogQuery.findByCosm(cosm) : catalogQuery.findAll();
        return entities.stream().map(mapper::toSummary).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<ItemDetailDto> getById(@PathVariable UUID id) {
        return catalogQuery.findById(id).map(mapper::toDetail).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
