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

package de.paladinsinn.security;

import de.paladinsinn.drivethru.ApiKeyFingerprint;
import de.paladinsinn.drivethru.DriveThruLibraryBatch;
import de.paladinsinn.drivethru.DriveThruLibrarySnapshot;
import de.paladinsinn.drivethru.DriveThruRPGService;
import de.paladinsinn.drivethru.LibraryBatchListener;
import de.paladinsinn.drivethru.token.DrivethruToken;
import de.paladinsinn.drivethru.token.NoValidTokenException;
import de.paladinsinn.torg.codex.application.port.in.CatalogPublicationReference;
import de.paladinsinn.torg.codex.application.port.in.CatalogReferenceQuery;
import de.paladinsinn.torg.codex.application.port.in.NotificationSeverity;
import de.paladinsinn.torg.codex.application.port.in.UserNotificationRequested;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DriveThruLibrarySynchronizer} covering monotonic authority union,
 * immutable principal/authentication/context replacement, partial failure retention, empty
 * libraries, supersession/logout detachment, token-copy isolation, and executor rejection.
 */
class DriveThruLibrarySynchronizerTest {

    private static final ApiKeyFingerprint FINGERPRINT = ApiKeyFingerprint.of("sync-unit-test-key");
    private static final String CHANNEL_ID = "channel-sync-test";
    private static final int PRODUCT_AYSLE = 284210;
    private static final int PRODUCT_LIVING_LAND = 247246;
    private static final int TIMEOUT_SECONDS = 5;

    private final DriveThruRPGService driveThruService = mock(DriveThruRPGService.class);
    private final CatalogReferenceQuery referenceQuery = mock(CatalogReferenceQuery.class);
    private final GenerationAwareSecurityContextRepository contextRepository =
            new GenerationAwareSecurityContextRepository();
    private final List<UserNotificationRequested> publishedEvents = new ArrayList<>();
    private final ApplicationEventPublisher eventPublisher = event -> {
        if (event instanceof UserNotificationRequested notification) {
            synchronized (publishedEvents) {
                publishedEvents.add(notification);
            }
        }
    };

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void streamsBatchesMonotonicallyReplacingImmutableAuthenticationAndCompletingCleanly() throws Exception {
        TaskExecutor directExecutor = Runnable::run;
        DriveThruLibrarySynchronizer synchronizer = new DriveThruLibrarySynchronizer(
                driveThruService, referenceQuery, directExecutor, eventPublisher, ObservationRegistry.NOOP);

        when(referenceQuery.findPublicationsByProductIds(Set.of(PRODUCT_AYSLE))).thenReturn(List.of(
                new CatalogPublicationReference(UUID.randomUUID(), "Aysle Sourcebook", "sourcebook-aysle")));
        when(referenceQuery.findPublicationsByProductIds(Set.of(PRODUCT_LIVING_LAND))).thenReturn(List.of(
                new CatalogPublicationReference(UUID.randomUUID(), "Living Land", "sourcebook-living-land")));

        MockHttpSession session = new MockHttpSession();
        UUID generationId = UUID.randomUUID();
        DrivethruToken sourceToken = new DrivethruToken();
        sourceToken.setAccessToken("initial-jwt");
        sourceToken.setApplicationKey("raw-secret-must-be-stripped");
        Authentication initialAuth = seedFreeTierSession(session, sourceToken, generationId);

        List<Authentication> observedAuthentications = new ArrayList<>();
        when(driveThruService.streamOwnedProductBatches(any(), any())).thenAnswer(invocation -> {
            DrivethruToken workerToken = invocation.getArgument(0);
            LibraryBatchListener listener = invocation.getArgument(1);
            assertThat(workerToken).isNotSameAs(sourceToken);
            assertThat(workerToken.getApplicationKey()).isNull();

            workerToken.setAccessToken("renewed-worker-jwt");
            listener.onBatch(new DriveThruLibraryBatch(1, List.of(PRODUCT_AYSLE), "424242"));
            SecurityContext afterBatchOne = currentContext(session);
            observedAuthentications.add(afterBatchOne.getAuthentication());
            assertThat(afterBatchOne.getAuthentication()).isNotSameAs(initialAuth);
            assertThat(principal(afterBatchOne).ownedCodexIds()).containsExactly("sourcebook-aysle");
            assertThat(afterBatchOne.getAuthentication().getAuthorities())
                    .extracting(Object::toString)
                    .contains("ROLE_DRIVETHRU_USER", "ROLE_sourcebook-aysle");
            assertThat(syncState(session).appliedBatches()).isEqualTo(1);

            listener.onBatch(new DriveThruLibraryBatch(2, List.of(PRODUCT_LIVING_LAND), "424242"));
            SecurityContext afterBatchTwo = currentContext(session);
            observedAuthentications.add(afterBatchTwo.getAuthentication());
            assertThat(afterBatchTwo.getAuthentication()).isNotSameAs(observedAuthentications.get(0));
            assertThat(principal(afterBatchTwo).ownedCodexIds())
                    .containsExactly("sourcebook-aysle", "sourcebook-living-land");
            assertThat(syncState(session).appliedBatches()).isEqualTo(2);

            return DriveThruLibrarySnapshot.of(
                    List.of(PRODUCT_AYSLE, PRODUCT_LIVING_LAND), "424242", Instant.now());
        });

        synchronizer.startInitialSync(session, CHANNEL_ID, FINGERPRINT, sourceToken.sessionCopy(), generationId);

        assertThat(syncState(session).status()).isEqualTo(SyncStatus.COMPLETED);
        assertThat(principal(currentContext(session)).getCustomerId()).isEqualTo("424242");
        assertThat(principal(currentContext(session)).token().getAccessToken()).isEqualTo("renewed-worker-jwt");
        assertThat(principal(currentContext(session)).token().getApplicationKey()).isNull();
        assertThat(publishedEvents).extracting(UserNotificationRequested::severity)
                .containsExactly(NotificationSeverity.INFO, NotificationSeverity.SUCCESS);
    }

    @Test
    void retainsAlreadyVerifiedBatchesOnPartialFailureAndHandlesEmptyLibrariesAndTaskRejection() throws Exception {
        TaskExecutor directExecutor = Runnable::run;
        DriveThruLibrarySynchronizer synchronizer = new DriveThruLibrarySynchronizer(
                driveThruService, referenceQuery, directExecutor, eventPublisher, ObservationRegistry.NOOP);

        when(referenceQuery.findPublicationsByProductIds(Set.of(PRODUCT_AYSLE))).thenReturn(List.of(
                new CatalogPublicationReference(UUID.randomUUID(), "Aysle", "sourcebook-aysle")));
        when(driveThruService.streamOwnedProductBatches(any(), any())).thenAnswer(invocation -> {
            LibraryBatchListener listener = invocation.getArgument(1);
            listener.onBatch(new DriveThruLibraryBatch(1, List.of(PRODUCT_AYSLE), "424242"));
            throw new AuthenticationServiceException("upstream 503 with secret-payload");
        });

        MockHttpSession partialSession = new MockHttpSession();
        UUID partialGen = UUID.randomUUID();
        DrivethruToken token = new DrivethruToken();
        seedFreeTierSession(partialSession, token, partialGen);

        synchronizer.startInitialSync(partialSession, CHANNEL_ID, FINGERPRINT, token.sessionCopy(), partialGen);

        assertThat(syncState(partialSession).status()).isEqualTo(SyncStatus.FAILED);
        assertThat(syncState(partialSession).appliedBatches()).isEqualTo(1);
        assertThat(principal(currentContext(partialSession)).ownedCodexIds()).containsExactly("sourcebook-aysle");
        assertThat(publishedEvents).last().satisfies(err -> {
            assertThat(err.severity()).isEqualTo(NotificationSeverity.ERROR);
            assertThat(err.message()).doesNotContain("secret-payload");
        });

        // Empty library completes cleanly with 0 applied batches
        publishedEvents.clear();
        reset(driveThruService);
        when(driveThruService.streamOwnedProductBatches(any(), any())).thenReturn(
                DriveThruLibrarySnapshot.of(List.of(), null, Instant.now()));
        MockHttpSession emptySession = new MockHttpSession();
        UUID emptyGen = UUID.randomUUID();
        seedFreeTierSession(emptySession, token, emptyGen);

        synchronizer.startInitialSync(emptySession, CHANNEL_ID, FINGERPRINT, token.sessionCopy(), emptyGen);

        assertThat(syncState(emptySession).status()).isEqualTo(SyncStatus.COMPLETED);
        assertThat(syncState(emptySession).appliedBatches()).isZero();
        assertThat(publishedEvents).extracting(UserNotificationRequested::severity)
                .containsExactly(NotificationSeverity.INFO, NotificationSeverity.SUCCESS);

        // Executor rejection marks FAILED and emits sticky error toast without crashing login caller
        publishedEvents.clear();
        TaskExecutor rejectingExecutor = task -> {
            throw new TaskRejectedException("queue full");
        };
        DriveThruLibrarySynchronizer rejectingSync = new DriveThruLibrarySynchronizer(
                driveThruService, referenceQuery, rejectingExecutor, eventPublisher, ObservationRegistry.NOOP);
        MockHttpSession rejectedSession = new MockHttpSession();
        UUID rejectedGen = UUID.randomUUID();
        seedFreeTierSession(rejectedSession, token, rejectedGen);

        rejectingSync.startInitialSync(rejectedSession, CHANNEL_ID, FINGERPRINT, token.sessionCopy(), rejectedGen);

        assertThat(syncState(rejectedSession).status()).isEqualTo(SyncStatus.FAILED);
        assertThat(publishedEvents).extracting(UserNotificationRequested::severity)
                .contains(NotificationSeverity.ERROR);
    }

    @Test
    void detachesCleanlyOnSupersessionOrLogoutAndMarksReauthRequiredWhenTokenUnrenewable() throws Exception {
        CountDownLatch inBatch = new CountDownLatch(1);
        CountDownLatch allowContinue = new CountDownLatch(1);
        CountDownLatch workerDone = new CountDownLatch(1);

        try (var pool = Executors.newSingleThreadExecutor()) {
            TaskExecutor asyncExecutor = command -> pool.execute(() -> {
                try {
                    command.run();
                } finally {
                    workerDone.countDown();
                }
            });
            DriveThruLibrarySynchronizer synchronizer = new DriveThruLibrarySynchronizer(
                    driveThruService, referenceQuery, asyncExecutor, eventPublisher, ObservationRegistry.NOOP);

            when(referenceQuery.findPublicationsByProductIds(Set.of(PRODUCT_AYSLE))).thenReturn(List.of(
                    new CatalogPublicationReference(UUID.randomUUID(), "Aysle", "sourcebook-aysle")));
            when(driveThruService.streamOwnedProductBatches(any(), any())).thenAnswer(invocation -> {
                LibraryBatchListener listener = invocation.getArgument(1);
                inBatch.countDown();
                assertThat(allowContinue.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
                listener.onBatch(new DriveThruLibraryBatch(1, List.of(PRODUCT_AYSLE), "424242"));
                return DriveThruLibrarySnapshot.of(List.of(PRODUCT_AYSLE), "424242", Instant.now());
            });

            MockHttpSession session = new MockHttpSession();
            UUID staleGen = UUID.randomUUID();
            UUID newerGen = UUID.randomUUID();
            DrivethruToken token = new DrivethruToken();
            seedFreeTierSession(session, token, staleGen);

            synchronizer.startInitialSync(session, CHANNEL_ID, FINGERPRINT, token.sessionCopy(), staleGen);
            assertThat(inBatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

            // Supersede with newer generation before stale worker delivers Batch 1
            seedFreeTierSession(session, token, newerGen);
            publishedEvents.clear();
            allowContinue.countDown();
            assertThat(workerDone.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

            assertThat(syncState(session).syncGenerationId()).isEqualTo(newerGen);
            assertThat(principal(currentContext(session)).ownedCodexIds()).isEmpty();
            assertThat(publishedEvents).isEmpty();
        }

        // Unrenewable refresh-only token marks REAUTH_REQUIRED and keeps existing rights
        reset(driveThruService);
        when(driveThruService.streamOwnedProductBatches(any(), any())).thenThrow(new NoValidTokenException());
        DriveThruLibrarySynchronizer syncDirect = new DriveThruLibrarySynchronizer(
                driveThruService, referenceQuery, Runnable::run, eventPublisher, ObservationRegistry.NOOP);
        MockHttpSession reauthSession = new MockHttpSession();
        UUID reauthGen = UUID.randomUUID();
        DrivethruToken token = new DrivethruToken();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setSession(reauthSession);
        contextRepository.saveExplicitAuthentication(
                ApiKeyAuthenticationToken.authenticated(
                        new DriveThruUserDetails(token.sessionCopy(), List.of("sourcebook-aysle"))),
                DriveThruSessionSyncState.running(reauthGen, Instant.now()),
                FINGERPRINT,
                req,
                new MockHttpServletResponse());

        syncDirect.startInitialSync(reauthSession, CHANNEL_ID, FINGERPRINT, token.sessionCopy(), reauthGen);

        assertThat(syncState(reauthSession).status()).isEqualTo(SyncStatus.REAUTH_REQUIRED);
        assertThat(principal(currentContext(reauthSession)).ownedCodexIds()).containsExactly("sourcebook-aysle");
    }

    private Authentication seedFreeTierSession(
            final MockHttpSession session,
            final DrivethruToken token,
            final UUID generationId) {
        Authentication auth = ApiKeyAuthenticationToken.authenticated(
                new DriveThruUserDetails(token.sessionCopy(), List.of()));
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setSession(session);
        contextRepository.saveExplicitAuthentication(
                auth,
                DriveThruSessionSyncState.running(generationId, Instant.now()),
                FINGERPRINT,
                req,
                new MockHttpServletResponse());
        return auth;
    }

    private static SecurityContext currentContext(final MockHttpSession session) {
        return (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    }

    private static DriveThruUserDetails principal(final SecurityContext context) {
        return (DriveThruUserDetails) context.getAuthentication().getPrincipal();
    }

    private static DriveThruSessionSyncState syncState(final MockHttpSession session) {
        return (DriveThruSessionSyncState) session.getAttribute(DriveThruSessionSyncState.SYNC_STATE_KEY);
    }
}
