package de.paladinsinn.torg.codex.api.dto;
import de.paladinsinn.torg.codex.data.model.ClearanceLevel;
import java.util.List;
import de.paladinsinn.torg.codex.data.model.ClearanceLevel;
import java.util.UUID;
public record VehicleDetailDto(UUID id, String name, CosmRefDto cosm, ClearanceLevel clearanceLevel, List<PublicationRefDto> publications, String type, String axiomTech, boolean unique, String speed, String speedValue, String speedMod, String size, String passengers, String maneuverRating, String wounds, String tough, String price, List<VehicleWeaponDto> weaponry, String text) {}
