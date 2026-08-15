package de.paladinsinn.torg.codex.api.dto;
import de.paladinsinn.torg.codex.data.model.ClearanceLevel;
import java.util.List;
import de.paladinsinn.torg.codex.data.model.ClearanceLevel;
import java.util.Map;
import de.paladinsinn.torg.codex.data.model.ClearanceLevel;
import java.util.UUID;
public record ThreatDetailDto(UUID id, String name, CosmRefDto cosm, ClearanceLevel clearanceLevel, List<PublicationRefDto> publications, boolean unique, String subName, String quote, String text, String charisma, String dexterity, String mind, String spirit, String strength, Map<String, String> skills, String moveWalk, String moveFly, String moveSwim, String tough, String shock, String wounds, List<String> equipment, List<String> perks, String possibilities, Map<String, String> specialAbilities) {}
