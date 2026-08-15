package de.paladinsinn.torg.codex.api.dto;
import de.paladinsinn.torg.codex.data.model.ClearanceLevel;
import java.util.List;
import de.paladinsinn.torg.codex.data.model.ClearanceLevel;
import java.util.UUID;
public record MiracleListSummaryDto(UUID id, String name, CosmRefDto cosm, ClearanceLevel clearanceLevel, List<PublicationRefDto> publications) {}
