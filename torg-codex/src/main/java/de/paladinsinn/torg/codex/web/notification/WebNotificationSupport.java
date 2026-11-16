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

import de.paladinsinn.security.ApiKeySessionAuthentication;
import de.paladinsinn.torg.codex.application.port.in.NotificationSeverity;
import de.paladinsinn.torg.codex.application.port.in.UserNotification;
import de.paladinsinn.torg.codex.application.port.in.UserNotificationQuery;
import de.paladinsinn.torg.codex.application.port.out.UserNotificationStorePort;
import de.paladinsinn.torg.codex.application.service.UserNotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.util.WebUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Shared non-controller web support binding opaque session notification channels, assembling SSR
 * {@link ToastRegionViewModel}s, handling request-scoped authentication-failure toasts, and
 * executing SSE subscription and UUID acknowledgment protocols (T031, ADR-024).
 */
@RequiredArgsConstructor
public class WebNotificationSupport {

    public static final String CODEX_STREAM_ENDPOINT = "/notifications/stream";
    public static final String CODEX_ACK_ENDPOINT = "/notifications/ack";
    public static final String ADMIN_STREAM_ENDPOINT = "/admin/notifications/stream";
    public static final String ADMIN_ACK_ENDPOINT = "/admin/notifications/ack";

    private static final String REQUEST_TOASTS_ATTR = WebNotificationSupport.class.getName() + ".requestToasts";

    private final UserNotificationQuery notificationQuery;
    private final UserNotificationStorePort storePort;
    private final ToastViewModelMapper toastMapper;
    private final SseNotificationBroadcaster sseBroadcaster;

    /**
     * Resolves an existing session notification channel without creating an {@link HttpSession}.
     *
     * @param request current HTTP request
     * @return existing channel ID when present
     */
    public Optional<String> existingChannelId(final HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }
        try {
            synchronized (WebUtils.getSessionMutex(session)) {
                if (session.getAttribute(ApiKeySessionAuthentication.NOTIFICATION_CHANNEL_ATTR)
                        instanceof String channelId && !channelId.isBlank()) {
                    return Optional.of(channelId);
                }
                return Optional.empty();
            }
        } catch (IllegalStateException invalidated) {
            return Optional.empty();
        }
    }

    /**
     * Returns or creates the session's opaque notification channel ID, optionally registering it
     * as an authenticated user channel for broadcast delivery.
     *
     * @param request       current HTTP request
     * @param authenticated whether the session represents a signed-in user
     * @return opaque channel ID
     */
    public String getOrCreateChannelId(final HttpServletRequest request, final boolean authenticated) {
        HttpSession session = request.getSession(true);
        String channelId;
        synchronized (WebUtils.getSessionMutex(session)) {
            if (session.getAttribute(ApiKeySessionAuthentication.NOTIFICATION_CHANNEL_ATTR)
                    instanceof String existing && !existing.isBlank()) {
                channelId = existing;
            } else {
                channelId = UUID.randomUUID().toString();
                session.setAttribute(ApiKeySessionAuthentication.NOTIFICATION_CHANNEL_ATTR, channelId);
            }
        }
        if (authenticated) {
            storePort.registerAuthenticatedChannel(channelId);
        }
        return channelId;
    }

    /**
     * Attaches a transient request-scoped sticky error toast (e.g. login rejection) without
     * allocating an authenticated notification channel.
     *
     * @param request current HTTP request
     * @param title   toast heading
     * @param message toast body
     */
    @SuppressWarnings("unchecked")
    public void addRequestErrorToast(
            final HttpServletRequest request,
            final String title,
            final String message) {
        if (request == null) {
            return;
        }
        List<UserNotification> list = (List<UserNotification>) request.getAttribute(REQUEST_TOASTS_ATTR);
        if (list == null) {
            list = new ArrayList<>();
            request.setAttribute(REQUEST_TOASTS_ATTR, list);
        }
        list.add(new UserNotification(
                UUID.randomUUID(),
                NotificationSeverity.ERROR,
                title,
                message,
                Instant.now(),
                null));
    }

    /**
     * Builds the SSR toast region for the reader layout without allocating sessions for anonymous
     * catalog views.
     *
     * @param request  current HTTP request
     * @param signedIn whether the caller is signed in
     * @return reader toast region view model
     */
    public ToastRegionViewModel buildReaderToastRegion(
            final HttpServletRequest request,
            final boolean signedIn) {
        Optional<String> channelOpt = signedIn && request != null
                ? Optional.of(getOrCreateChannelId(request, true))
                : existingChannelId(request);
        return buildRegion(request, channelOpt, CODEX_STREAM_ENDPOINT, CODEX_ACK_ENDPOINT);
    }

    /**
     * Builds the SSR toast region for the {@code /admin} layout.
     *
     * @param request current HTTP request
     * @return admin toast region view model
     */
    public ToastRegionViewModel buildAdminToastRegion(final HttpServletRequest request) {
        Optional<String> channelOpt = existingChannelId(request);
        return buildRegion(request, channelOpt, ADMIN_STREAM_ENDPOINT, ADMIN_ACK_ENDPOINT);
    }

    /**
     * Handles {@code GET .../notifications/stream}: returns {@code 204 No Content} without session
     * allocation when no channel exists, or opens an {@link SseEmitter} stream otherwise.
     *
     * @param request  current HTTP request
     * @param response current HTTP response
     * @param surface  UI layout surface
     * @return {@code 204} response or {@link SseEmitter}
     */
    public Object openStream(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final NotificationSurface surface) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store");
        Optional<String> channelOpt = existingChannelId(request);
        if (channelOpt.isEmpty()) {
            return ResponseEntity.noContent()
                    .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                    .build();
        }
        response.setHeader("X-Accel-Buffering", "no");
        return sseBroadcaster.subscribe(channelOpt.get(), surface);
    }

    /**
     * Handles {@code POST .../notifications/ack}: validates up to 20 UUID parameters and
     * acknowledges matching IDs scoped strictly to the session's own channel.
     *
     * @param rawIds   submitted UUID strings
     * @param request  current HTTP request
     * @param response current HTTP response
     * @return {@code 204 No Content} or {@code 400 Bad Request}
     */
    public ResponseEntity<Void> acknowledge(
            final List<String> rawIds,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store");
        if (rawIds == null || rawIds.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        if (rawIds.size() > UserNotificationService.MAX_PENDING_BATCH) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Set<UUID> parsedIds = new LinkedHashSet<>(rawIds.size());
        for (String raw : rawIds) {
            if (raw == null || raw.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            try {
                parsedIds.add(UUID.fromString(raw.strip()));
            } catch (IllegalArgumentException malformed) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        }
        existingChannelId(request)
                .ifPresent(channelId -> notificationQuery.acknowledge(channelId, parsedIds));
        return ResponseEntity.noContent().build();
    }

    @SuppressWarnings("unchecked")
    private ToastRegionViewModel buildRegion(
            final HttpServletRequest request,
            final Optional<String> channelOpt,
            final String streamUrl,
            final String ackUrl) {
        List<ToastViewModel> combined = new ArrayList<>();
        if (request != null && request.getAttribute(REQUEST_TOASTS_ATTR) instanceof List<?> reqToasts) {
            for (Object item : reqToasts) {
                if (item instanceof UserNotification notification) {
                    combined.add(toastMapper.toViewModel(notification));
                }
            }
        }
        if (channelOpt.isPresent()) {
            List<UserNotification> pending = notificationQuery.pending(channelOpt.get());
            combined.addAll(toastMapper.toViewModels(pending));
            return new ToastRegionViewModel(combined, streamUrl, ackUrl);
        }
        return new ToastRegionViewModel(combined, null, null);
    }
}
