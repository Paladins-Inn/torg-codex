package de.paladinsinn.torg.codex.domain.model;

import lombok.Value;

/**
 * Framework-independent domain representation of a DriveThruRPG product as needed by the
 * catalog. Deliberately carries only the fields the domain/application layers use, decoupled
 * from the wire/HTTP DTO shape.
 */
@Value
public class CatalogProduct {
    int id;
    String name;
    String coverUrl;
}
