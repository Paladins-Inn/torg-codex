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

package de.paladinsinn.torg.codex.api.configuration;

import de.paladinsinn.drivethru.DriveThruRPGService;
import de.paladinsinn.drivethru.LibraryLoadCoordinator;
import de.paladinsinn.security.DriveThruLibrarySynchronizer;
import de.paladinsinn.torg.codex.application.port.in.CatalogReferenceQuery;
import de.paladinsinn.torg.codex.application.port.in.UserNotificationCommand;
import de.paladinsinn.torg.codex.application.port.in.UserNotificationQuery;
import de.paladinsinn.torg.codex.application.port.in.UserNotificationRequested;
import de.paladinsinn.torg.codex.application.port.out.UserNotificationStorePort;
import de.paladinsinn.torg.codex.application.service.UserNotificationService;
import de.paladinsinn.torg.codex.web.notification.InMemoryUserNotificationStoreAdapter;
import de.paladinsinn.torg.codex.web.notification.NotificationSessionListener;
import de.paladinsinn.torg.codex.web.notification.SseNotificationBroadcaster;
import de.paladinsinn.torg.codex.web.notification.ToastViewModelMapper;
import de.paladinsinn.torg.codex.web.notification.WebNotificationSupport;
import gg.jte.TemplateEngine;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Spring bean wiring for bounded asynchronous DriveThruRPG library synchronization and generic
 * event-driven SSE user notifications (ADR-023, ADR-024).
 */
@Configuration
public class NotificationConfiguration {

    private static final int DEFAULT_SYNC_CORE_THREADS = 4;
    private static final int DEFAULT_SYNC_QUEUE_CAPACITY = 100;

    /**
     * Spring Events listener bridging framework-free {@link UserNotificationRequested} events
     * (both single-session and all-authenticated broadcast targets) into {@link UserNotificationCommand}.
     */
    @RequiredArgsConstructor
    public static class NotificationEventBridge {

        private final UserNotificationCommand notificationCommand;

        @EventListener
        public void onNotificationRequested(final UserNotificationRequested event) {
            if (event == null || event.target() == null) {
                return;
            }
            notificationCommand.dispatch(
                    event.target(),
                    event.severity(),
                    event.title(),
                    event.message(),
                    event.action());
        }
    }

    @Bean
    public UserNotificationStorePort userNotificationStorePort(final ApplicationEventPublisher eventPublisher) {
        return new InMemoryUserNotificationStoreAdapter(eventPublisher);
    }

    @Bean
    public UserNotificationService userNotificationService(final UserNotificationStorePort storePort) {
        return new UserNotificationService(storePort);
    }

    @Bean
    public NotificationEventBridge notificationEventBridge(final UserNotificationCommand notificationCommand) {
        return new NotificationEventBridge(notificationCommand);
    }

    @Bean
    public SseNotificationBroadcaster sseNotificationBroadcaster(
            final UserNotificationQuery notificationQuery,
            final ToastViewModelMapper toastMapper,
            final TemplateEngine templateEngine,
            @Value("${torg.codex.notifications.sse-timeout:30m}") final Duration sseTimeout) {
        return new SseNotificationBroadcaster(notificationQuery, toastMapper, templateEngine, sseTimeout);
    }

    @Bean
    public NotificationSessionListener notificationSessionListener(
            final UserNotificationStorePort storePort,
            final SseNotificationBroadcaster sseBroadcaster) {
        return new NotificationSessionListener(storePort, sseBroadcaster);
    }

    @Bean
    public WebNotificationSupport webNotificationSupport(
            final UserNotificationQuery notificationQuery,
            final UserNotificationStorePort storePort,
            final ToastViewModelMapper toastMapper,
            final SseNotificationBroadcaster sseBroadcaster) {
        return new WebNotificationSupport(notificationQuery, storePort, toastMapper, sseBroadcaster);
    }

    /**
     * Bounded background executor for streaming DriveThruRPG library batches with Micrometer/MDC
     * context propagation and explicit {@link java.util.concurrent.RejectedExecutionException} failure.
     *
     * @param coreSize      configured core/max worker threads
     * @param queueCapacity configured bounded queue capacity
     * @return configured task executor
     */
    @Bean(name = "drivethruSyncExecutor")
    public ThreadPoolTaskExecutor drivethruSyncExecutor(
            @Value("${drivethru.library.sync.executor.core-size:" + DEFAULT_SYNC_CORE_THREADS + "}") final int coreSize,
            @Value("${drivethru.library.sync.executor.queue-capacity:" + DEFAULT_SYNC_QUEUE_CAPACITY + "}") final int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int effectiveThreads = Math.max(1, coreSize);
        executor.setCorePoolSize(effectiveThreads);
        executor.setMaxPoolSize(effectiveThreads);
        executor.setQueueCapacity(Math.max(1, queueCapacity));
        executor.setThreadNamePrefix("drivethru-sync-");
        executor.setTaskDecorator(new ContextPropagatingTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }

    /**
     * Wires the interactive background library synchronizer.
     *
     * @param driveThruRPGService   DriveThruRPG HTTP service
     * @param catalogReferenceQuery publication lookup query port
     * @param syncExecutor          bounded synchronization executor
     * @param eventPublisher        Spring application event publisher
     * @param observationRegistry   optional observation registry
     * @return configured synchronizer
     */
    @Bean
    public DriveThruLibrarySynchronizer driveThruLibrarySynchronizer(
            final DriveThruRPGService driveThruRPGService,
            final CatalogReferenceQuery catalogReferenceQuery,
            @Qualifier("drivethruSyncExecutor") final TaskExecutor syncExecutor,
            final ApplicationEventPublisher eventPublisher,
            final ObjectProvider<ObservationRegistry> observationRegistry,
            final ObjectProvider<LibraryLoadCoordinator> libraryLoadCoordinator) {
        DriveThruLibrarySynchronizer synchronizer = new DriveThruLibrarySynchronizer(
                driveThruRPGService,
                catalogReferenceQuery,
                syncExecutor,
                eventPublisher,
                observationRegistry.getIfAvailable(() -> ObservationRegistry.NOOP));
        synchronizer.setLibraryLoadCoordinator(libraryLoadCoordinator.getIfAvailable());
        return synchronizer;
    }
}
