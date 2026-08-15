package de.paladinsinn.torg.codex.application.port.in;

import java.util.UUID;

/**
 * Framework-independent publication reference including its stable codex identifier.
 *
 * @param id stable entry identifier
 * @param name display name
 * @param codexId stable product ownership identifier
 */
public record CatalogPublicationReference(UUID id, String name, String codexId) {
}
