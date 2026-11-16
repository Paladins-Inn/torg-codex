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

package de.paladinsinn.torg.codex.application.service;

import de.paladinsinn.torg.codex.application.port.in.NotificationAction;
import de.paladinsinn.torg.codex.application.port.in.NotificationActionKind;
import de.paladinsinn.torg.codex.application.port.in.NotificationSeverity;
import de.paladinsinn.torg.codex.application.port.in.UserNotification;
import de.paladinsinn.torg.codex.application.port.in.UserNotificationCommand;
import de.paladinsinn.torg.codex.application.port.in.UserNotificationQuery;
import de.paladinsinn.torg.codex.application.port.out.UserNotificationStorePort;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Framework-free application service validating, dispatching, reading, and acknowledging user
 * notifications for both individual sessions and all authenticated users (FR-005, ADR-024).
 */
@RequiredArgsConstructor
public class UserNotificationService implements UserNotificationCommand, UserNotificationQuery {

    public static final int MAX_PENDING_BATCH = 20;
    public static final int MAX_ACTION_LABEL_LENGTH = 40;

    private final UserNotificationStorePort store;
    private final Clock clock;

    /**
     * Creates a service backed by the system UTC clock.
     *
     * @param store notification store port
     */
    public UserNotificationService(final UserNotificationStorePort store) {
        this(Objects.requireNonNull(store, "store"), Clock.systemUTC());
    }

    @Override
    public UserNotification notifySession(
            final String channelId,
            final NotificationSeverity severity,
            final String title,
            final String message,
            final NotificationAction action) {
        String normalizedChannel = requireNonBlankNoControl(channelId, "channelId");
        ValidatedPayload payload = validatePayload(severity, title, message, action);

        UserNotification notification = new UserNotification(
                UUID.randomUUID(),
                payload.severity(),
                payload.title(),
                payload.message(),
                clock.instant(),
                payload.action());
        store.enqueue(normalizedChannel, notification);
        return notification;
    }

    @Override
    public List<UserNotification> notifyAllAuthenticated(
            final NotificationSeverity severity,
            final String title,
            final String message,
            final NotificationAction action) {
        ValidatedPayload payload = validatePayload(severity, title, message, action);
        Set<String> recipients = store.authenticatedChannels();
        if (recipients == null || recipients.isEmpty()) {
            return List.of();
        }
        Instant now = clock.instant();
        List<String> sortedRecipients = recipients.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::strip)
                .distinct()
                .sorted()
                .toList();
        List<UserNotification> delivered = new ArrayList<>(sortedRecipients.size());
        for (String channelId : sortedRecipients) {
            UserNotification copy = new UserNotification(
                    UUID.randomUUID(),
                    payload.severity(),
                    payload.title(),
                    payload.message(),
                    now,
                    payload.action());
            store.enqueue(channelId, copy);
            delivered.add(copy);
        }
        return List.copyOf(delivered);
    }

    private record ValidatedPayload(
            NotificationSeverity severity,
            String title,
            String message,
            NotificationAction action) {
    }

    private static ValidatedPayload validatePayload(
            final NotificationSeverity severity,
            final String title,
            final String message,
            final NotificationAction action) {
        if (severity == null) {
            throw new IllegalArgumentException("severity must not be null.");
        }
        String normalizedTitle = requireNonBlank(title, "title");
        String normalizedMessage = requireNonBlank(message, "message");
        NotificationAction validatedAction = validateAction(action);
        return new ValidatedPayload(severity, normalizedTitle, normalizedMessage, validatedAction);
    }

    @Override
    public List<UserNotification> pending(final String channelId) {
        if (channelId == null || channelId.isBlank()) {
            return List.of();
        }
        List<UserNotification> items = store.pending(channelId.strip(), MAX_PENDING_BATCH);
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        if (items.size() <= MAX_PENDING_BATCH) {
            return List.copyOf(items);
        }
        return List.copyOf(items.subList(0, MAX_PENDING_BATCH));
    }

    @Override
    public void acknowledge(final String channelId, final Set<UUID> notificationIds) {
        if (channelId == null || channelId.isBlank() || notificationIds == null || notificationIds.isEmpty()) {
            return;
        }
        Set<UUID> validIds = notificationIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
        if (!validIds.isEmpty()) {
            store.acknowledge(channelId.strip(), validIds);
        }
    }

    private static NotificationAction validateAction(final NotificationAction action) {
        if (action == null) {
            return null;
        }
        String label = requireNonBlank(action.label(), "action.label");
        if (label.length() > MAX_ACTION_LABEL_LENGTH) {
            throw new IllegalArgumentException("action.label must be at most " + MAX_ACTION_LABEL_LENGTH + " characters.");
        }
        if (action.kind() == null) {
            throw new IllegalArgumentException("action.kind must not be null.");
        }
        return switch (action.kind()) {
            case RELOAD_CURRENT_PAGE -> {
                if (action.targetPath() != null) {
                    throw new IllegalArgumentException("RELOAD_CURRENT_PAGE action must not specify a targetPath.");
                }
                yield new NotificationAction(label, NotificationActionKind.RELOAD_CURRENT_PAGE, null);
            }
            case NAVIGATE_URL -> {
                String path = validateInternalTargetPath(action.targetPath());
                yield new NotificationAction(label, NotificationActionKind.NAVIGATE_URL, path);
            }
        };
    }

    private static String validateInternalTargetPath(final String targetPath) {
        if (targetPath == null || targetPath.isBlank()) {
            throw new IllegalArgumentException("NAVIGATE_URL action requires a targetPath.");
        }
        String trimmed = targetPath.strip();
        if (!trimmed.startsWith("/")
                || trimmed.startsWith("//")
                || trimmed.contains("\\")
                || trimmed.contains("://")
                || trimmed.chars().anyMatch(ch -> Character.isISOControl(ch) || Character.isWhitespace(ch))) {
            throw new IllegalArgumentException("Invalid internal navigation targetPath.");
        }
        return trimmed;
    }

    private static String requireNonBlank(final String raw, final String fieldName) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank.");
        }
        return raw.strip();
    }

    private static String requireNonBlankNoControl(final String raw, final String fieldName) {
        String normalized = requireNonBlank(raw, fieldName);
        if (normalized.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(fieldName + " must not contain control characters.");
        }
        return normalized;
    }
}
