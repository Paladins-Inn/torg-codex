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
import de.paladinsinn.torg.codex.application.port.in.NotificationTarget;
import de.paladinsinn.torg.codex.application.port.in.UserNotification;
import de.paladinsinn.torg.codex.application.port.in.UserNotificationRequested;
import de.paladinsinn.torg.codex.application.port.out.UserNotificationStorePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Framework-free unit tests for {@link UserNotificationService}.
 */
class UserNotificationServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-11-16T09:00:00Z");
    private static final int MAX_BATCH = 20;
    private static final int BACKLOG_TOTAL = 25;
    private static final int REMAINING_AFTER_FIRST_ACK = 5;
    private static final int MAX_LABEL_LENGTH = 40;

    private FakeStore store;
    private UserNotificationService service;

    @BeforeEach
    void setUp() {
        store = new FakeStore();
        service = new UserNotificationService(store, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
    }

    @Test
    void supportsAllSeveritiesDismissalModesAndOptionalActions() {
        assertThat(NotificationSeverity.INFO.autoDismiss()).isTrue();
        assertThat(NotificationSeverity.SUCCESS.autoDismiss()).isTrue();
        assertThat(NotificationSeverity.WARNING.autoDismiss()).isFalse();
        assertThat(NotificationSeverity.ERROR.autoDismiss()).isFalse();

        UserNotification info = service.notifyInfo("channel-a", "Started", "Sync running");
        UserNotification success = service.notifySuccess(
                "channel-a", "Done", "All books loaded", NotificationAction.reloadCurrentPage("Reload page"));
        UserNotification warning = service.notifyWarning("channel-a", "Degraded", "Slow upstream");
        UserNotification error = service.notifyError(
                "channel-a", "Expired", "Sign in again", NotificationAction.navigateUrl("Sign in again", "/login"));

        List<UserNotification> pending = service.pending("channel-a");
        assertThat(pending).containsExactly(info, success, warning, error);
        assertThat(success.action()).isEqualTo(
                new NotificationAction("Reload page", NotificationActionKind.RELOAD_CURRENT_PAGE, null));
        assertThat(error.action()).isEqualTo(
                new NotificationAction("Sign in again", NotificationActionKind.NAVIGATE_URL, "/login"));
        assertThat(info.createdAt()).isEqualTo(FIXED_INSTANT);
    }

    @Test
    void pendingReadsNonDestructivelyUpToTwentyInFifoOrderWithoutLosingBacklog() {
        List<UUID> createdIds = new ArrayList<>();
        for (int i = 1; i <= BACKLOG_TOTAL; i++) {
            createdIds.add(service.notifyInfo("channel-fifo", "Title " + i, "Message " + i).id());
        }

        List<UserNotification> firstRead = service.pending("channel-fifo");
        List<UserNotification> secondRead = service.pending("channel-fifo");
        assertThat(firstRead).hasSize(MAX_BATCH);
        assertThat(secondRead).containsExactlyElementsOf(firstRead);
        assertThat(firstRead).extracting(UserNotification::id)
                .containsExactlyElementsOf(createdIds.subList(0, MAX_BATCH));

        service.acknowledge("channel-fifo", Set.copyOf(createdIds.subList(0, MAX_BATCH)));

        List<UserNotification> remaining = service.pending("channel-fifo");
        assertThat(remaining).hasSize(REMAINING_AFTER_FIRST_ACK);
        assertThat(remaining).extracting(UserNotification::id)
                .containsExactlyElementsOf(createdIds.subList(MAX_BATCH, BACKLOG_TOTAL));
    }

    @Test
    void isolatesChannelsAndTreatsAcknowledgmentsIdempotently() {
        UserNotification alpha = service.notifyInfo("channel-alpha", "Alpha", "First channel");
        UserNotification beta = service.notifyError("channel-beta", "Beta", "Second channel");

        service.acknowledge("channel-alpha", Set.of(beta.id(), UUID.randomUUID()));
        assertThat(service.pending("channel-alpha")).containsExactly(alpha);
        assertThat(service.pending("channel-beta")).containsExactly(beta);

        service.acknowledge("channel-alpha", Set.of(alpha.id()));
        service.acknowledge("channel-alpha", Set.of(alpha.id()));
        assertThat(service.pending("channel-alpha")).isEmpty();
        assertThat(service.pending("channel-beta")).containsExactly(beta);
    }

    @Test
    void dispatchesSessionAndAuthenticatedBroadcastTargetsWithIndependentAcknowledgments() {
        store.registerAuthenticatedChannel("auth-reader-1");
        store.registerAuthenticatedChannel("auth-reader-2");
        service.notifyInfo("anonymous-login-page", "Pre-login", "Anonymous session only");

        UserNotificationRequested broadcast = UserNotificationRequested.broadcastWarning(
                "Scheduled Maintenance", "Codex maintenance starts in 15 minutes.");
        List<UserNotification> delivered = service.dispatch(
                broadcast.target(),
                broadcast.severity(),
                broadcast.title(),
                broadcast.message(),
                broadcast.action());

        assertThat(delivered).hasSize(2);
        assertThat(delivered.get(0).id()).isNotEqualTo(delivered.get(1).id());
        assertThat(service.pending("auth-reader-1")).containsExactly(delivered.get(0));
        assertThat(service.pending("auth-reader-2")).containsExactly(delivered.get(1));
        assertThat(service.pending("anonymous-login-page"))
                .extracting(UserNotification::title)
                .containsExactly("Pre-login");

        service.acknowledge("auth-reader-1", Set.of(delivered.get(0).id()));
        assertThat(service.pending("auth-reader-1")).isEmpty();
        assertThat(service.pending("auth-reader-2")).containsExactly(delivered.get(1));

        store.clear("auth-reader-2");
        assertThat(service.dispatch(
                NotificationTarget.allAuthenticatedUsers(),
                NotificationSeverity.INFO,
                "Follow-up",
                "Only reader 1 remains logged in",
                null)).hasSize(1);
        assertThat(service.pending("auth-reader-1")).hasSize(1);
        assertThat(service.pending("auth-reader-2")).isEmpty();
    }

    @Test
    void rejectsExternalSchemeRelativeBackslashControlCharacterAndMalformedTargets() {
        assertThatThrownBy(() -> service.notifySession(null, NotificationSeverity.INFO, "T", "M", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.notifySession(" ", NotificationSeverity.INFO, "T", "M", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.notifySession("ch", null, "T", "M", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.notifySession("ch", NotificationSeverity.INFO, " ", "M", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.notifySession("ch", NotificationSeverity.INFO, "T", " ", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.notifySession(
                "ch", NotificationSeverity.INFO, "T", "M",
                new NotificationAction("Reload", NotificationActionKind.RELOAD_CURRENT_PAGE, "/unexpected")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.notifySession(
                "ch", NotificationSeverity.ERROR, "T", "M",
                NotificationAction.navigateUrl("x".repeat(MAX_LABEL_LENGTH + 1), "/login")))
                .isInstanceOf(IllegalArgumentException.class);

        for (String unsafeTarget : List.of(
                "https://evil.example/login",
                "javascript:alert(1)",
                "//evil.example/login",
                "/\\evil.example",
                "login",
                "/login\r\nSet-Cookie: bad=1",
                "/login with spaces")) {
            assertThatThrownBy(() -> service.notifySession(
                    "ch", NotificationSeverity.ERROR, "T", "M",
                    NotificationAction.navigateUrl("Sign in", unsafeTarget)))
                    .as("Target %s must be rejected", unsafeTarget)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    /** Minimal deterministic in-memory store for pure application-service testing. */
    private static final class FakeStore implements UserNotificationStorePort {

        private final Map<String, Map<UUID, UserNotification>> byChannel = new LinkedHashMap<>();
        private final Set<String> authenticated = new LinkedHashSet<>();

        @Override
        public void enqueue(final String channelId, final UserNotification notification) {
            byChannel.computeIfAbsent(channelId, ignored -> new LinkedHashMap<>())
                    .put(notification.id(), notification);
        }

        @Override
        public List<UserNotification> pending(final String channelId, final int limit) {
            Map<UUID, UserNotification> queue = byChannel.get(channelId);
            if (queue == null || queue.isEmpty()) {
                return List.of();
            }
            return queue.values().stream().limit(limit).toList();
        }

        @Override
        public void acknowledge(final String channelId, final Set<UUID> notificationIds) {
            Map<UUID, UserNotification> queue = byChannel.get(channelId);
            if (queue != null) {
                notificationIds.forEach(queue::remove);
            }
        }

        @Override
        public void registerAuthenticatedChannel(final String channelId) {
            authenticated.add(channelId);
        }

        @Override
        public Set<String> authenticatedChannels() {
            return Set.copyOf(authenticated);
        }

        @Override
        public void clear(final String channelId) {
            byChannel.remove(channelId);
            authenticated.remove(channelId);
        }
    }
}
