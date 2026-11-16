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
import de.paladinsinn.drivethru.LibraryLoadCoordinator;
import de.paladinsinn.drivethru.SupersededSyncException;
import de.paladinsinn.drivethru.token.DrivethruToken;
import de.paladinsinn.drivethru.token.NoValidTokenException;
import de.paladinsinn.torg.codex.application.port.in.CatalogPublicationReference;
import de.paladinsinn.torg.codex.application.port.in.CatalogReferenceQuery;
import de.paladinsinn.torg.codex.application.port.in.NotificationAction;
import de.paladinsinn.torg.codex.application.port.in.UserNotificationRequested;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.util.WebUtils;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrates asynchronous DriveThruRPG library streaming for interactive browser sessions,
 * atomically expanding {@code ROLE_<codex-id>} authorities per batch and publishing generic
 * notification lifecycle events (FR-003, FR-004, FR-006, FR-007, FR-009, FR-018, ADR-023, ADR-024).
 */
@RequiredArgsConstructor
@Slf4j
public class DriveThruLibrarySynchronizer {

    public static final String SYNC_STARTED_TITLE = "Library Sync Started";
    public static final String SYNC_STARTED_MESSAGE =
            "You are signed in. Loading your purchased publications from DriveThruRPG in the background—"
            + "this could take several minutes until all publications become available.";
    public static final String SYNC_COMPLETE_TITLE = "Library Sync Complete";
    public static final String SYNC_COMPLETE_MESSAGE =
            "All purchased DriveThruRPG publications have been loaded and are now available.";
    public static final String SYNC_FAILED_TITLE = "Library Sync Failed";
    public static final String SYNC_FAILED_MESSAGE =
            "Could not finish loading your DriveThruRPG library. Use Reload Library to retry.";
    public static final String REAUTH_REQUIRED_TITLE = "Sign-in Required";
    public static final String REAUTH_REQUIRED_MESSAGE =
            "Your DriveThruRPG session has expired. Please sign in again to reload your library.";
    public static final String PUBLICATIONS_READY_TITLE = "Publications Ready";
    public static final String PUBLICATIONS_READY_MESSAGE =
            "You are signed in and your DriveThruRPG publications are unlocked.";

    /**
     * Resolved publication codex IDs and customer ID derived from a cached library snapshot.
     *
     * @param ownedCodexIds resolved catalog publication codex IDs
     * @param customerId    cached upstream customer ID, or {@code null}
     */
    public record CachedLibraryResolution(List<String> ownedCodexIds, String customerId) {
    }

    private final DriveThruRPGService driveThruRPGService;
    private final CatalogReferenceQuery catalogReferenceQuery;
    private final TaskExecutor syncExecutor;
    private final ApplicationEventPublisher eventPublisher;
    private final ObservationRegistry observationRegistry;

    @Setter
    private LibraryLoadCoordinator libraryLoadCoordinator;

    /**
     * Probes the shared 12-hour library cache for {@code fingerprint} and resolves owned
     * publications in a single bulk query on a cache hit.
     *
     * @param fingerprint normalized API-key fingerprint
     * @return resolved publications when cached, or empty on cache miss
     */
    public Optional<CachedLibraryResolution> resolveCachedLibrary(final ApiKeyFingerprint fingerprint) {
        if (libraryLoadCoordinator == null || fingerprint == null) {
            return Optional.empty();
        }
        return libraryLoadCoordinator.findCached(fingerprint).map(snapshot -> {
            List<String> codexIds = snapshot.productIds().isEmpty()
                    ? List.of()
                    : catalogReferenceQuery.findPublicationsByProductIds(Set.copyOf(snapshot.productIds())).stream()
                            .map(CatalogPublicationReference::codexId)
                            .distinct()
                            .toList();
            return new CachedLibraryResolution(codexIds, snapshot.customerId());
        });
    }

    /**
     * Publishes the single ready confirmation toast for a cache-hit login (FR-006).
     *
     * @param channelId opaque session notification channel ID
     */
    public void notifyCachedReady(final String channelId) {
        publishIfPresent(channelId, UserNotificationRequested.success(
                channelId, PUBLICATIONS_READY_TITLE, PUBLICATIONS_READY_MESSAGE, null));
    }

    /**
     * Starts a forced asynchronous manual library reload using only the session's stored
     * {@link ApiKeyFingerprint} and key-free {@link DrivethruToken} copy (FR-018).
     *
     * @param session           active HTTP session
     * @param channelId         session notification channel ID
     * @param contextRepository generation-aware security context repository
     * @param request           current HTTP request
     * @param response          current HTTP response
     * @return {@code true} if the session holds a valid DriveThru principal and reload started
     */
    public boolean startManualReload(
            final HttpSession session,
            final String channelId,
            final GenerationAwareSecurityContextRepository contextRepository,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        if (session == null) {
            return false;
        }
        final ApiKeyFingerprint fingerprint;
        final DrivethruToken sessionToken;
        final UUID generationId = UUID.randomUUID();
        synchronized (WebUtils.getSessionMutex(session)) {
            if (!(session.getAttribute(ApiKeySessionAuthentication.KEY_FINGERPRINT_ATTR)
                    instanceof ApiKeyFingerprint storedFp)
                    || !(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)
                            instanceof SecurityContext context)
                    || !(context.getAuthentication() instanceof ApiKeyAuthenticationToken auth)
                    || !(auth.getPrincipal() instanceof DriveThruUserDetails details)
                    || details.token() == null) {
                return false;
            }
            fingerprint = storedFp;
            sessionToken = details.token().sessionCopy();
            DriveThruSessionSyncState syncState = DriveThruSessionSyncState.running(generationId, Instant.now());
            contextRepository.saveExplicitAuthentication(
                    ApiKeyAuthenticationToken.authenticated(
                            new DriveThruUserDetails(sessionToken, details.ownedCodexIds())),
                    syncState,
                    fingerprint,
                    request,
                    response);
        }
        startInitialSync(session, channelId, fingerprint, sessionToken, generationId);
        return true;
    }

    /**
     * Starts an uncached background library synchronization for an active session generation.
     *
     * @param session      authenticated HTTP session
     * @param channelId    session notification channel identifier, or {@code null}
     * @param fingerprint  normalized API-key digest
     * @param sessionToken key-free session token
     * @param generationId current synchronization generation identifier
     */
    public void startInitialSync(
            final HttpSession session,
            final String channelId,
            final ApiKeyFingerprint fingerprint,
            final DrivethruToken sessionToken,
            final UUID generationId) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(fingerprint, "fingerprint");
        Objects.requireNonNull(sessionToken, "sessionToken");
        Objects.requireNonNull(generationId, "generationId");

        DrivethruToken workerToken = sessionToken.sessionCopy();
        publishIfPresent(channelId, UserNotificationRequested.info(
                channelId, SYNC_STARTED_TITLE, SYNC_STARTED_MESSAGE));

        try {
            syncExecutor.execute(() -> runSync(session, channelId, fingerprint, workerToken, generationId));
        } catch (TaskRejectedException rejected) {
            handleTaskRejected(session, channelId, fingerprint, generationId, rejected);
        }
    }

    private void runSync(
            final HttpSession session,
            final String channelId,
            final ApiKeyFingerprint fingerprint,
            final DrivethruToken workerToken,
            final UUID generationId) {
        Observation observation = Observation.createNotStarted("drivethru.library.sync", observationRegistry)
                .contextualName("drivethru library sync")
                .start();
        try (Observation.Scope ignored = observation.openScope()) {
            ensureGenerationActive(session, fingerprint, generationId);
            final DriveThruLibrarySnapshot snapshot;
            if (libraryLoadCoordinator != null) {
                snapshot = libraryLoadCoordinator.reloadCoordinated(
                        fingerprint,
                        workerToken,
                        batch -> applyBatch(session, fingerprint, workerToken, generationId, batch),
                        () -> ensureGenerationActive(session, fingerprint, generationId));
            } else {
                snapshot = driveThruRPGService.streamOwnedProductBatches(
                        workerToken,
                        batch -> applyBatch(session, fingerprint, workerToken, generationId, batch));
            }
            completeGeneration(session, channelId, fingerprint, workerToken, generationId, snapshot);
            observation.lowCardinalityKeyValue("outcome", "success");
        } catch (SupersededSyncException | IllegalStateException superseded) {
            observation.lowCardinalityKeyValue("outcome", "superseded");
            log.atDebug()
                    .addKeyValue("event.action", "drivethru-sync-superseded")
                    .log("DriveThruRPG background library sync detached after supersession or logout.");
        } catch (NoValidTokenException unrenewable) {
            observation.lowCardinalityKeyValue("outcome", "reauth-required");
            observation.error(new IllegalStateException("DriveThruRPG library sync reauth-required"));
            markReauthRequired(session, channelId, fingerprint, generationId);
        } catch (RuntimeException failure) {
            observation.lowCardinalityKeyValue("outcome", "error");
            observation.error(new IllegalStateException("DriveThruRPG library sync error"));
            markFailed(session, channelId, fingerprint, generationId, failure);
        } finally {
            observation.stop();
        }
    }

    private void applyBatch(
            final HttpSession session,
            final ApiKeyFingerprint fingerprint,
            final DrivethruToken workerToken,
            final UUID generationId,
            final DriveThruLibraryBatch batch) {
        ensureGenerationActive(session, fingerprint, generationId);
        List<String> batchCodexIds = batch.productIds().isEmpty()
                ? List.of()
                : catalogReferenceQuery.findPublicationsByProductIds(Set.copyOf(batch.productIds())).stream()
                        .map(CatalogPublicationReference::codexId)
                        .distinct()
                        .toList();

        try {
            synchronized (WebUtils.getSessionMutex(session)) {
                DriveThruSessionSyncState state = requireActiveStateLocked(session, fingerprint, generationId);
                SecurityContext currentContext = (SecurityContext) session.getAttribute(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                if (currentContext == null
                        || !(currentContext.getAuthentication() instanceof ApiKeyAuthenticationToken auth)
                        || !(auth.getPrincipal() instanceof DriveThruUserDetails currentDetails)) {
                    throw new SupersededSyncException("Session no longer holds DriveThru authentication.");
                }

                LinkedHashSet<String> unionedCodexIds = new LinkedHashSet<>(currentDetails.ownedCodexIds());
                unionedCodexIds.addAll(batchCodexIds);

                DrivethruToken updatedToken = workerToken.sessionCopy();
                if (updatedToken.getCustomerId() == null && batch.customerId() != null) {
                    updatedToken.setCustomerId(batch.customerId());
                }

                DriveThruUserDetails replacedDetails = new DriveThruUserDetails(
                        updatedToken, List.copyOf(unionedCodexIds));
                SecurityContext replacedContext = SecurityContextHolder.createEmptyContext();
                replacedContext.setAuthentication(ApiKeyAuthenticationToken.authenticated(replacedDetails));
                session.setAttribute(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, replacedContext);
                session.setAttribute(
                        DriveThruSessionSyncState.SYNC_STATE_KEY, state.withBatchApplied());
            }
        } catch (IllegalStateException invalidated) {
            throw new SupersededSyncException("Session invalidated during batch application.");
        }
    }

    private void completeGeneration(
            final HttpSession session,
            final String channelId,
            final ApiKeyFingerprint fingerprint,
            final DrivethruToken workerToken,
            final UUID generationId,
            final DriveThruLibrarySnapshot snapshot) {
        synchronized (WebUtils.getSessionMutex(session)) {
            DriveThruSessionSyncState state = requireActiveStateLocked(session, fingerprint, generationId);
            SecurityContext currentContext = (SecurityContext) session.getAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            if (currentContext != null
                    && currentContext.getAuthentication() instanceof ApiKeyAuthenticationToken auth
                    && auth.getPrincipal() instanceof DriveThruUserDetails currentDetails) {
                DrivethruToken refreshedCopy = workerToken.sessionCopy();
                if (refreshedCopy.getCustomerId() == null && snapshot.customerId() != null) {
                    refreshedCopy.setCustomerId(snapshot.customerId());
                }
                DriveThruUserDetails updatedDetails = new DriveThruUserDetails(
                        refreshedCopy, currentDetails.ownedCodexIds());
                SecurityContext updatedContext = SecurityContextHolder.createEmptyContext();
                updatedContext.setAuthentication(ApiKeyAuthenticationToken.authenticated(updatedDetails));
                session.setAttribute(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, updatedContext);
            }
            session.setAttribute(
                    DriveThruSessionSyncState.SYNC_STATE_KEY, state.withStatus(SyncStatus.COMPLETED));
        }

        log.atInfo()
                .addKeyValue("event.action", "drivethru-sync-completed")
                .addKeyValue("productCount", snapshot.productIds().size())
                .log("DriveThruRPG background library synchronization completed.");
        publishIfPresent(channelId, UserNotificationRequested.success(
                channelId,
                SYNC_COMPLETE_TITLE,
                SYNC_COMPLETE_MESSAGE,
                NotificationAction.reloadCurrentPage("Reload page")));
    }

    private void markReauthRequired(
            final HttpSession session,
            final String channelId,
            final ApiKeyFingerprint fingerprint,
            final UUID generationId) {
        if (!transitionIfActive(session, fingerprint, generationId, SyncStatus.REAUTH_REQUIRED)) {
            return;
        }
        log.atInfo()
                .addKeyValue("event.action", "drivethru-sync-reauth-required")
                .log("DriveThruRPG session token expired during background synchronization; re-sign-in required.");
        publishIfPresent(channelId, UserNotificationRequested.error(
                channelId,
                REAUTH_REQUIRED_TITLE,
                REAUTH_REQUIRED_MESSAGE,
                NotificationAction.navigateUrl("Sign in again", "/login")));
    }

    private void markFailed(
            final HttpSession session,
            final String channelId,
            final ApiKeyFingerprint fingerprint,
            final UUID generationId,
            final RuntimeException failure) {
        if (!transitionIfActive(session, fingerprint, generationId, SyncStatus.FAILED)) {
            return;
        }
        log.atWarn()
                .addKeyValue("event.action", "drivethru-sync-failed")
                .addKeyValue("exceptionClass", failure.getClass().getName())
                .log("DriveThruRPG background library synchronization failed.");
        publishIfPresent(channelId, UserNotificationRequested.error(
                channelId, SYNC_FAILED_TITLE, SYNC_FAILED_MESSAGE, null));
    }

    private void handleTaskRejected(
            final HttpSession session,
            final String channelId,
            final ApiKeyFingerprint fingerprint,
            final UUID generationId,
            final TaskRejectedException rejected) {
        if (!transitionIfActive(session, fingerprint, generationId, SyncStatus.FAILED)) {
            return;
        }
        log.atWarn()
                .addKeyValue("event.action", "drivethru-sync-rejected")
                .addKeyValue("exceptionClass", rejected.getClass().getName())
                .log("DriveThruRPG background library synchronization executor rejected task.");
        publishIfPresent(channelId, UserNotificationRequested.error(
                channelId, SYNC_FAILED_TITLE, SYNC_FAILED_MESSAGE, null));
    }

    private boolean transitionIfActive(
            final HttpSession session,
            final ApiKeyFingerprint fingerprint,
            final UUID generationId,
            final SyncStatus targetStatus) {
        try {
            synchronized (WebUtils.getSessionMutex(session)) {
                if (!isGenerationActiveLocked(session, fingerprint, generationId)) {
                    return false;
                }
                DriveThruSessionSyncState state = (DriveThruSessionSyncState) session.getAttribute(
                        DriveThruSessionSyncState.SYNC_STATE_KEY);
                session.setAttribute(DriveThruSessionSyncState.SYNC_STATE_KEY, state.withStatus(targetStatus));
                return true;
            }
        } catch (IllegalStateException invalidated) {
            return false;
        }
    }

    private static void ensureGenerationActive(
            final HttpSession session,
            final ApiKeyFingerprint fingerprint,
            final UUID generationId) {
        try {
            synchronized (WebUtils.getSessionMutex(session)) {
                requireActiveStateLocked(session, fingerprint, generationId);
            }
        } catch (IllegalStateException invalidated) {
            throw new SupersededSyncException("Session invalidated.");
        }
    }

    private static DriveThruSessionSyncState requireActiveStateLocked(
            final HttpSession session,
            final ApiKeyFingerprint fingerprint,
            final UUID generationId) {
        if (!isGenerationActiveLocked(session, fingerprint, generationId)) {
            throw new SupersededSyncException("Synchronization generation superseded.");
        }
        return (DriveThruSessionSyncState) session.getAttribute(DriveThruSessionSyncState.SYNC_STATE_KEY);
    }

    private static boolean isGenerationActiveLocked(
            final HttpSession session,
            final ApiKeyFingerprint fingerprint,
            final UUID generationId) {
        if (Boolean.TRUE.equals(session.getAttribute(DriveThruSessionSyncState.LOGGED_OUT_KEY))) {
            return false;
        }
        if (!(session.getAttribute(ApiKeySessionAuthentication.KEY_FINGERPRINT_ATTR) instanceof ApiKeyFingerprint currentFp)
                || !fingerprint.equals(currentFp)) {
            return false;
        }
        if (!(session.getAttribute(DriveThruSessionSyncState.SYNC_STATE_KEY) instanceof DriveThruSessionSyncState state)) {
            return false;
        }
        return generationId.equals(state.syncGenerationId());
    }

    private void publishIfPresent(final String channelId, final UserNotificationRequested event) {
        if (channelId != null && !channelId.isBlank() && eventPublisher != null) {
            eventPublisher.publishEvent(event);
        }
    }
}
