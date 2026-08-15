package de.paladinsinn.torg.codex.domain.model;

import lombok.Value;

import java.util.UUID;

@Value
public class PublicationReference {
    UUID id;
    String name;
}
