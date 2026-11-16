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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.paladinsinn.torg.codex.application.port.in.UserNotification;
import de.paladinsinn.torg.codex.application.port.out.UserNotificationStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concurrency-safe in-memory session-channel notification store emitting {@link UserNotificationPublished}
 * Spring events on enqueue while guarding against late-event channel resurrection after logout or
 * session destruction (ADR-024).
 */
@RequiredArgsConstructor
public class InMemoryUserNotificationStoreAdapter implements UserNotificationStorePort {

    private static final int DESTROYED_TOMBSTONE_CAPACITY = 10_000;

    private final ApplicationEventPublisher eventPublisher;

    private final Map<String, ChannelQueue> queues = new ConcurrentHashMap<>();
    private final Set<String> authenticatedChannels = ConcurrentHashMap.newKeySet();
    private final Cache<String, Boolean> destroyedChannels = Caffeine.newBuilder()
            .maximumSize(DESTROYED_TOMBSTONE_CAPACITY)
            .build();

    @Override
    public void enqueue(final String channelId, final UserNotification notification) {
        if (channelId == null || channelId.isBlank() || notification == null) {
            return;
        }
        String key = channelId.strip();
        if (Boolean.TRUE.equals(destroyedChannels.getIfPresent(key))) {
            return;
        }
        ChannelQueue queue = queues.computeIfAbsent(key, ignored -> new ChannelQueue());
        boolean stored = queue.putIfOpen(notification, () -> Boolean.TRUE.equals(destroyedChannels.getIfPresent(key)));
        if (!stored) {
            queues.remove(key, queue);
            return;
        }
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new UserNotificationPublished(key, notification));
        }
    }

    @Override
    public List<UserNotification> pending(final String channelId, final int limit) {
        if (channelId == null || channelId.isBlank() || limit <= 0) {
            return List.of();
        }
        ChannelQueue queue = queues.get(channelId.strip());
        return queue == null ? List.of() : queue.snapshot(limit);
    }

    @Override
    public void acknowledge(final String channelId, final Set<UUID> notificationIds) {
        if (channelId == null || channelId.isBlank() || notificationIds == null || notificationIds.isEmpty()) {
            return;
        }
        ChannelQueue queue = queues.get(channelId.strip());
        if (queue != null) {
            queue.removeAll(notificationIds);
        }
    }

    @Override
    public void registerAuthenticatedChannel(final String channelId) {
        if (channelId == null || channelId.isBlank()) {
            return;
        }
        String key = channelId.strip();
        if (!Boolean.TRUE.equals(destroyedChannels.getIfPresent(key))) {
            authenticatedChannels.add(key);
        }
    }

    @Override
    public Set<String> authenticatedChannels() {
        return Set.copyOf(authenticatedChannels);
    }

    @Override
    public void clear(final String channelId) {
        if (channelId == null || channelId.isBlank()) {
            return;
        }
        String key = channelId.strip();
        destroyedChannels.put(key, Boolean.TRUE);
        authenticatedChannels.remove(key);
        ChannelQueue removed = queues.remove(key);
        if (removed != null) {
            removed.closeAndClear();
        }
    }

    private static final class ChannelQueue {

        private final Map<UUID, UserNotification> entries = new LinkedHashMap<>();
        private boolean closed;

        synchronized boolean putIfOpen(
                final UserNotification notification,
                final java.util.function.BooleanSupplier destroyedCheck) {
            if (closed || destroyedCheck.getAsBoolean()) {
                closed = true;
                return false;
            }
            entries.put(notification.id(), notification);
            return true;
        }

        synchronized List<UserNotification> snapshot(final int limit) {
            if (closed || entries.isEmpty()) {
                return List.of();
            }
            return entries.values().stream().limit(limit).toList();
        }

        synchronized void removeAll(final Set<UUID> ids) {
            if (!closed) {
                ids.forEach(entries::remove);
            }
        }

        synchronized void closeAndClear() {
            closed = true;
            entries.clear();
        }
    }
}
