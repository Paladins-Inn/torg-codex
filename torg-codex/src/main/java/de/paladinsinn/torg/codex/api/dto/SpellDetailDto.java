package de.paladinsinn.torg.codex.api.dto;
import de.paladinsinn.torg.codex.data.model.ClearanceLevel;
import java.util.List;
import de.paladinsinn.torg.codex.data.model.ClearanceLevel;
import java.util.Map;
import de.paladinsinn.torg.codex.data.model.ClearanceLevel;
import java.util.UUID;
public record SpellDetailDto(UUID id, String name, ClearanceLevel clearanceLevel, List<PublicationRefDto> publications, String axiom, String castingTime, DifficultyNumberDto dn, String range, String duration, Map<String, Integer> requiredSkills, String text) {}
