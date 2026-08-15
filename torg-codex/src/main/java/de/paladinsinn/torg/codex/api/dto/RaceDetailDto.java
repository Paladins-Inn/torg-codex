package de.paladinsinn.torg.codex.api.dto;
import de.paladinsinn.torg.codex.data.model.ClearanceLevel;
import java.util.List;
import de.paladinsinn.torg.codex.data.model.ClearanceLevel;
import java.util.Map;
import de.paladinsinn.torg.codex.data.model.ClearanceLevel;
import java.util.UUID;
public record RaceDetailDto(UUID id, String name, ClearanceLevel clearanceLevel, List<PublicationRefDto> publications, boolean major, Map<String, Integer> attributeLimits, String abilities, String text, String perkText) {}
