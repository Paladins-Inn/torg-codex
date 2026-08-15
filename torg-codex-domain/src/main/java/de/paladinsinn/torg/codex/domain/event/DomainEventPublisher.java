package de.paladinsinn.torg.codex.domain.event;

public interface DomainEventPublisher {

    void publish(DomainEvent<?> event);

    default void publishAll(Iterable<? extends DomainEvent<?>> events) {
        for (DomainEvent<?> event : events) {
            publish(event);
        }
    }
}
