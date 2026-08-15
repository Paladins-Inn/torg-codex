package de.paladinsinn.torg.codex.api.dto;
import de.paladinsinn.torg.codex.data.model.ClearanceLevel;
import java.util.List;
import de.paladinsinn.torg.codex.data.model.ClearanceLevel;
import java.util.UUID;
public record ItemDetailDto(UUID id, String name, CosmRefDto cosm, ClearanceLevel clearanceLevel, List<PublicationRefDto> publications, String type, String axiomTech, String axiomMagic, String price, String bonus, String ammo, String range, String features, String additionalFeatures, String text) {}
