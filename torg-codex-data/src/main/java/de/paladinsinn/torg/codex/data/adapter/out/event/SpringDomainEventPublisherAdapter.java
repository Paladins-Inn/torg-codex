package de.paladinsinn.torg.codex.data.adapter.out.event;

import de.paladinsinn.torg.codex.domain.event.DomainEvent;
import de.paladinsinn.torg.codex.domain.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Outbound event adapter bridging the framework-independent {@link DomainEventPublisher} port
 * to Spring's {@link ApplicationEventPublisher}. Each {@link DomainEvent} is forwarded as-is so
 * that existing and future Spring {@code @EventListener}/{@code ApplicationListener} beans observe
 * the exact same payload and metadata.
 */
@RequiredArgsConstructor
public class SpringDomainEventPublisherAdapter implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(final DomainEvent<?> event) {
        applicationEventPublisher.publishEvent(event);
    }
}
