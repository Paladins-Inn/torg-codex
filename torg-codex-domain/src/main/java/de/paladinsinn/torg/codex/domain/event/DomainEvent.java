package de.paladinsinn.torg.codex.domain.event;

import java.util.Map;

public record DomainEvent<T>(String type, T payload, Map<String, Object> metadata) {

    public DomainEvent(String type, T payload) {
        this(type, payload, Map.of());
    }
}
