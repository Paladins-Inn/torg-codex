/*
 * Copyright (c) 2026.  Roland T. Lichti <rlichti@kaiserpfalz-edv.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * You may contact me via email rlichti@kaiserpfalz-edv.de or via mail
 *
 * Kaiserpfalz EDV-Service
 * Roland T. Lichti
 * Darmstädter Str. 12
 * 64625 Bensheim
 * GERMANY
 */

package de.paladinsinn.torg.codex.web.notification;

import de.paladinsinn.torg.codex.application.port.in.UserNotification;
import de.paladinsinn.torg.codex.application.port.in.UserNotificationQuery;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Spring Event-driven Server-Sent Events broadcaster pushing rendered JTE toast fragments to open
 * reader and administrative browser tabs without periodic HTTP polling (ADR-024).
 */
@RequiredArgsConstructor
public class SseNotificationBroadcaster {

    private record EmitterRegistration(SseEmitter emitter, NotificationSurface surface) {
    }

    private final UserNotificationQuery notificationQuery;
    private final ToastViewModelMapper toastMapper;
    private final TemplateEngine templateEngine;
    private final Duration sseTimeout;

    private final Map<String, Set<EmitterRegistration>> registrationsByChannel = new ConcurrentHashMap<>();

    /**
     * Subscribes an open browser tab to live toast notifications for {@code channelId}, flushing up
     * to 20 unacknowledged pending notifications immediately upon connection.
     *
     * @param channelId opaque session notification channel ID
     * @param surface   target layout surface ({@link NotificationSurface#CODEX} or {@link NotificationSurface#ADMIN})
     * @return configured {@link SseEmitter}
     */
    public SseEmitter subscribe(final String channelId, final NotificationSurface surface) {
        String key = channelId.strip();
        SseEmitter emitter = new SseEmitter(sseTimeout.toMillis());
        EmitterRegistration registration = new EmitterRegistration(emitter, surface);
        Set<EmitterRegistration> channelSet = registrationsByChannel.computeIfAbsent(
                key, ignored -> new CopyOnWriteArraySet<>());
        channelSet.add(registration);

        Runnable cleanup = () -> removeRegistration(key, registration);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> {
            cleanup.run();
            emitter.complete();
        });
        emitter.onError(ignored -> cleanup.run());

        try {
            emitter.send(SseEmitter.event().comment("connected"));
            List<UserNotification> pending = notificationQuery.pending(key);
            if (!pending.isEmpty()) {
                String html = renderToasts(surface, toastMapper.toViewModels(pending));
                emitter.send(SseEmitter.event()
                        .id(pending.getLast().id().toString())
                        .name("toast")
                        .data(html));
            }
        } catch (IOException | IllegalStateException disconnected) {
            cleanup.run();
            emitter.complete();
        }
        return emitter;
    }

    /**
     * Immediately pushes newly published notifications to all active SSE emitters registered on the
     * target session channel.
     *
     * @param event Spring notification publication event
     */
    @EventListener
    public void onNotificationPublished(final UserNotificationPublished event) {
        if (event == null || event.channelId() == null || event.notification() == null) {
            return;
        }
        Set<EmitterRegistration> registrations = registrationsByChannel.get(event.channelId());
        if (registrations == null || registrations.isEmpty()) {
            return;
        }

        ToastViewModel viewModel = toastMapper.toViewModel(event.notification());
        Map<NotificationSurface, String> renderedBySurface = new EnumMap<>(NotificationSurface.class);
        String eventId = event.notification().id().toString();

        for (EmitterRegistration reg : registrations) {
            String html = renderedBySurface.computeIfAbsent(
                    reg.surface(), s -> renderToasts(s, List.of(viewModel)));
            try {
                reg.emitter().send(SseEmitter.event().id(eventId).name("toast").data(html));
            } catch (IOException | IllegalStateException stale) {
                removeRegistration(event.channelId(), reg);
            }
        }
    }

    /**
     * Completes and removes all active SSE emitters bound to {@code channelId} upon logout or
     * session invalidation.
     *
     * @param channelId opaque session notification channel ID
     */
    public void closeChannel(final String channelId) {
        if (channelId == null || channelId.isBlank()) {
            return;
        }
        Set<EmitterRegistration> removed = registrationsByChannel.remove(channelId.strip());
        if (removed != null) {
            for (EmitterRegistration reg : removed) {
                try {
                    reg.emitter().complete();
                } catch (IllegalStateException ignored) {
                    // Already completed.
                }
            }
        }
    }

    /**
     * Returns the number of active SSE emitters currently registered for {@code channelId}.
     *
     * @param channelId opaque session notification channel ID
     * @return active emitter count
     */
    public int activeEmitterCount(final String channelId) {
        if (channelId == null || channelId.isBlank()) {
            return 0;
        }
        Set<EmitterRegistration> set = registrationsByChannel.get(channelId.strip());
        return set == null ? 0 : set.size();
    }

    private void removeRegistration(final String channelId, final EmitterRegistration registration) {
        Set<EmitterRegistration> set = registrationsByChannel.get(channelId);
        if (set != null) {
            set.remove(registration);
            if (set.isEmpty()) {
                registrationsByChannel.remove(channelId, set);
            }
        }
    }

    private String renderToasts(final NotificationSurface surface, final List<ToastViewModel> toasts) {
        StringOutput output = new StringOutput();
        templateEngine.render(surface.templateName(), Map.of("toasts", toasts), output);
        return output.toString();
    }
}
