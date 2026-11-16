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

import de.paladinsinn.torg.codex.application.port.in.NotificationAction;
import de.paladinsinn.torg.codex.application.port.in.NotificationSeverity;
import de.paladinsinn.torg.codex.application.port.in.UserNotification;
import de.paladinsinn.torg.codex.application.port.in.UserNotificationCommand;
import de.paladinsinn.torg.codex.application.port.in.UserNotificationQuery;
import de.paladinsinn.torg.codex.application.port.in.UserNotificationRequested;
import jakarta.servlet.http.HttpSessionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for event-driven user notification storage, Spring Events bridging,
 * single-session vs. all-authenticated broadcast routing, SSE registration/closure, session-ID
 * rotation, concurrency, and destroyed-channel protection (US2, FR-005, ADR-024).
 */
@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest
class UserNotificationDeliveryTest {

    private static final int MAX_WINDOW = 20;
    private static final int BACKLOG_COUNT = 26;
    private static final int CONCURRENT_ITEMS = 30;
    private static final int THREAD_POOL_SIZE = 4;
    private static final int TIMEOUT_SECONDS = 5;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private UserNotificationCommand notificationCommand;

    @Autowired
    private UserNotificationQuery notificationQuery;

    @Autowired
    private WebNotificationSupport notificationSupport;

    @Autowired
    private SseNotificationBroadcaster sseBroadcaster;

    @Autowired
    private NotificationSessionListener sessionListener;

    @Test
    void genericSpringEventProducerDeliversToSessionAndBroadcastTargetsWhilePreservingRotatedSessions() {
        MockHttpSession readerOneSession = new MockHttpSession();
        MockHttpServletRequest reqOne = new MockHttpServletRequest();
        reqOne.setSession(readerOneSession);
        String channelOne = notificationSupport.getOrCreateChannelId(reqOne, true);
        readerOneSession.changeSessionId();
        assertThat(notificationSupport.existingChannelId(reqOne)).contains(channelOne);

        MockHttpSession readerTwoSession = new MockHttpSession();
        MockHttpServletRequest reqTwo = new MockHttpServletRequest();
        reqTwo.setSession(readerTwoSession);
        String channelTwo = notificationSupport.getOrCreateChannelId(reqTwo, true);

        MockHttpSession anonSession = new MockHttpSession();
        MockHttpServletRequest anonReq = new MockHttpServletRequest();
        anonReq.setSession(anonSession);
        String anonChannel = notificationSupport.getOrCreateChannelId(anonReq, false);

        // Generic non-DriveThru producer publishes session-scoped and broadcast events
        eventPublisher.publishEvent(UserNotificationRequested.info(
                channelOne, "Export Queued", "Character sheet PDF queued."));
        eventPublisher.publishEvent(UserNotificationRequested.broadcastWarning(
                "Maintenance Window", "Planned restart at 22:00 UTC."));

        List<UserNotification> onePending = notificationQuery.pending(channelOne);
        List<UserNotification> twoPending = notificationQuery.pending(channelTwo);
        List<UserNotification> anonPending = notificationQuery.pending(anonChannel);

        assertThat(onePending).extracting(UserNotification::title)
                .containsExactly("Export Queued", "Maintenance Window");
        assertThat(twoPending).extracting(UserNotification::title)
                .containsExactly("Maintenance Window");
        assertThat(anonPending).isEmpty();

        sessionListener.sessionDestroyed(new HttpSessionEvent(readerOneSession));
        sessionListener.sessionDestroyed(new HttpSessionEvent(readerTwoSession));
        sessionListener.sessionDestroyed(new HttpSessionEvent(anonSession));
    }

    @Test
    void preservesOverTwentyBacklogSupportsConcurrentEnqueueAckAndBlocksLateEventsAfterSessionDestroy()
            throws Exception {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setSession(session);
        String channelId = notificationSupport.getOrCreateChannelId(req, true);

        for (int i = 1; i <= BACKLOG_COUNT; i++) {
            notificationCommand.notifySession(
                    channelId,
                    NotificationSeverity.INFO,
                    "Item " + i,
                    "Body " + i,
                    NotificationAction.reloadCurrentPage("Reload"));
        }

        List<UserNotification> firstWindow = notificationQuery.pending(channelId);
        assertThat(firstWindow).hasSize(MAX_WINDOW);
        notificationQuery.acknowledge(
                channelId, Set.copyOf(firstWindow.stream().map(UserNotification::id).toList()));
        assertThat(notificationQuery.pending(channelId)).hasSize(BACKLOG_COUNT - MAX_WINDOW);
        notificationQuery.acknowledge(
                channelId,
                Set.copyOf(notificationQuery.pending(channelId).stream().map(UserNotification::id).toList()));
        assertThat(notificationQuery.pending(channelId)).isEmpty();

        // Concurrent enqueue + ack safety
        try (var pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE)) {
            List<Callable<UUID>> tasks = java.util.stream.IntStream.range(0, CONCURRENT_ITEMS)
                    .<Callable<UUID>>mapToObj(idx -> () -> notificationCommand.notifyInfo(
                            channelId, "Concurrent " + idx, "Message " + idx).id())
                    .toList();
            var futures = pool.invokeAll(tasks);
            for (var future : futures) {
                UUID id = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                notificationQuery.acknowledge(channelId, Set.of(id));
            }
        }
        assertThat(notificationQuery.pending(channelId)).isEmpty();

        // Register SSE emitter, then destroy session: emitter completes and late events do not recreate queue
        AtomicBoolean completed = new AtomicBoolean(false);
        SseEmitter emitter = sseBroadcaster.subscribe(channelId, NotificationSurface.CODEX);
        emitter.onCompletion(() -> completed.set(true));

        sessionListener.sessionDestroyed(new HttpSessionEvent(session));
        assertThat(sseBroadcaster.activeEmitterCount(channelId)).isZero();

        eventPublisher.publishEvent(UserNotificationRequested.info(
                channelId, "Late Event", "Must be discarded for destroyed channel"));
        assertThat(notificationQuery.pending(channelId)).isEmpty();
        assertThat(completed.get() || sseBroadcaster.activeEmitterCount(channelId) == 0).isTrue();
    }
}
