package de.paladinsinn.torg.codex.data.application.port.in;

import java.util.UUID;

/**
 * Framework-independent representation of a codex entry reference.
 *
 * @param id stable entry identifier
 * @param name display name
 */
public record CatalogReference(UUID id, String name) {
}
