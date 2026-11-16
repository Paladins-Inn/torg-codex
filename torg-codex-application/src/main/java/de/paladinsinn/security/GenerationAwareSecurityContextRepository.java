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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.util.WebUtils;

import java.util.Objects;
import java.util.UUID;

/**
 * {@link SecurityContextRepository} wrapper around {@link HttpSessionSecurityContextRepository}
 * that coordinates request-end context saves with concurrent background batch updates, newer
 * sign-ins, and logout invalidation (ADR-023).
 */
public class GenerationAwareSecurityContextRepository implements SecurityContextRepository {

    private static final String LOADED_REVISION_ATTR =
            GenerationAwareSecurityContextRepository.class.getName() + ".loadedRevision";

    private record LoadedRevision(
            String sessionId,
            UUID generationId,
            int appliedBatches,
            ApiKeyFingerprint fingerprint) {
    }

    private final HttpSessionSecurityContextRepository delegate = new HttpSessionSecurityContextRepository();

    @Override
    @SuppressWarnings("deprecation")
    public SecurityContext loadContext(final HttpRequestResponseHolder requestResponseHolder) {
        HttpServletRequest request = requestResponseHolder.getRequest();
        SecurityContext context = delegate.loadContext(requestResponseHolder);
        captureLoadedRevision(request);
        return context;
    }

    @Override
    public DeferredSecurityContext loadDeferredContext(final HttpServletRequest request) {
        DeferredSecurityContext deferred = delegate.loadDeferredContext(request);
        return new DeferredSecurityContext() {
            private boolean captured;

            @Override
            public SecurityContext get() {
                SecurityContext ctx = deferred.get();
                if (!captured) {
                    captured = true;
                    captureLoadedRevision(request);
                }
                return ctx;
            }

            @Override
            public boolean isGenerated() {
                return deferred.isGenerated();
            }
        };
    }

    @Override
    public void saveContext(
            final SecurityContext context,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }

        try {
            synchronized (WebUtils.getSessionMutex(session)) {
                if (Boolean.TRUE.equals(session.getAttribute(DriveThruSessionSyncState.LOGGED_OUT_KEY))) {
                    return;
                }
                if (isStaleComparedToSession(request, session)) {
                    return;
                }
                delegate.saveContext(context, request, response);
            }
        } catch (IllegalStateException ignored) {
            // Session was already invalidated concurrently (e.g. logout); never recreate or resurrect.
        }
    }

    @Override
    public boolean containsContext(final HttpServletRequest request) {
        return delegate.containsContext(request);
    }

    /**
     * Atomically writes an explicit login/header authentication, its fingerprint, and optional sync
     * state under the session mutex and updates the request's loaded revision baseline.
     *
     * @param authentication authenticated token (already stripped of cleartext API key)
     * @param syncState      new synchronization state, or {@code null} for synchronous header login
     * @param fingerprint    normalized API-key fingerprint
     * @param request        current HTTP request
     * @param response       current HTTP response
     */
    public void saveExplicitAuthentication(
            final Authentication authentication,
            final DriveThruSessionSyncState syncState,
            final ApiKeyFingerprint fingerprint,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        HttpSession session = request.getSession(true);
        synchronized (WebUtils.getSessionMutex(session)) {
            session.removeAttribute(DriveThruSessionSyncState.LOGGED_OUT_KEY);
            session.setAttribute(ApiKeySessionAuthentication.KEY_FINGERPRINT_ATTR, fingerprint);
            if (syncState != null) {
                session.setAttribute(DriveThruSessionSyncState.SYNC_STATE_KEY, syncState);
            } else {
                session.removeAttribute(DriveThruSessionSyncState.SYNC_STATE_KEY);
            }
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            delegate.saveContext(context, request, response);
            request.setAttribute(LOADED_REVISION_ATTR, new LoadedRevision(
                    session.getId(),
                    syncState != null ? syncState.syncGenerationId() : null,
                    syncState != null ? syncState.appliedBatches() : 0,
                    fingerprint));
        }
    }

    /**
     * Marks the request's session as logged out and detaches any active synchronization generation
     * before session invalidation completes.
     *
     * @param request current logout HTTP request
     */
    public void markLoggedOut(final HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        try {
            synchronized (WebUtils.getSessionMutex(session)) {
                session.setAttribute(DriveThruSessionSyncState.LOGGED_OUT_KEY, Boolean.TRUE);
                session.removeAttribute(DriveThruSessionSyncState.SYNC_STATE_KEY);
                session.removeAttribute(ApiKeySessionAuthentication.KEY_FINGERPRINT_ATTR);
            }
        } catch (IllegalStateException ignored) {
            // Session already invalidated.
        }
    }

    private void captureLoadedRevision(final HttpServletRequest request) {
        if (request.getAttribute(LOADED_REVISION_ATTR) != null) {
            return;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        try {
            synchronized (WebUtils.getSessionMutex(session)) {
                DriveThruSessionSyncState state = (DriveThruSessionSyncState) session.getAttribute(
                        DriveThruSessionSyncState.SYNC_STATE_KEY);
                ApiKeyFingerprint fingerprint = (ApiKeyFingerprint) session.getAttribute(
                        ApiKeySessionAuthentication.KEY_FINGERPRINT_ATTR);
                request.setAttribute(LOADED_REVISION_ATTR, new LoadedRevision(
                        session.getId(),
                        state != null ? state.syncGenerationId() : null,
                        state != null ? state.appliedBatches() : 0,
                        fingerprint));
            }
        } catch (IllegalStateException ignored) {
            // Session invalidated concurrently.
        }
    }

    private static boolean isStaleComparedToSession(
            final HttpServletRequest request,
            final HttpSession session) {
        if (!(request.getAttribute(LOADED_REVISION_ATTR) instanceof LoadedRevision loaded)) {
            return session.getAttribute(DriveThruSessionSyncState.SYNC_STATE_KEY) != null
                    || session.getAttribute(ApiKeySessionAuthentication.KEY_FINGERPRINT_ATTR) != null;
        }
        ApiKeyFingerprint currentFp = (ApiKeyFingerprint) session.getAttribute(
                ApiKeySessionAuthentication.KEY_FINGERPRINT_ATTR);
        if (!Objects.equals(loaded.fingerprint(), currentFp)) {
            return true;
        }
        DriveThruSessionSyncState currentState = (DriveThruSessionSyncState) session.getAttribute(
                DriveThruSessionSyncState.SYNC_STATE_KEY);
        if (currentState == null) {
            return loaded.generationId() != null;
        }
        if (!Objects.equals(loaded.generationId(), currentState.syncGenerationId())) {
            return true;
        }
        return currentState.appliedBatches() > loaded.appliedBatches();
    }
}
