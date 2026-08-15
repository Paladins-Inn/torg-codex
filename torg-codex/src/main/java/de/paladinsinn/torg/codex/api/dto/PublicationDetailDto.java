package de.paladinsinn.torg.codex.api.dto;
import java.util.Set;
import java.util.UUID;
public record PublicationDetailDto(UUID id, String name, String codexId, int primaryProductId, String thirdParty, Set<Integer> productIds, String coverURL) {}
