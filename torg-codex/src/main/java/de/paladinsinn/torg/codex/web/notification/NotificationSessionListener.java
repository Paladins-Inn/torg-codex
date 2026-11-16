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
import de.paladinsinn.torg.codex.application.port.out.UserNotificationStorePort;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.RequiredArgsConstructor;

/**
 * {@link HttpSessionListener} closing active SSE streams and purging pending session notifications
 * when an HTTP session is invalidated or expires (FR-011, ADR-024).
 */
@RequiredArgsConstructor
public class NotificationSessionListener implements HttpSessionListener {

    private final UserNotificationStorePort storePort;
    private final SseNotificationBroadcaster sseBroadcaster;

    @Override
    public void sessionDestroyed(final HttpSessionEvent event) {
        if (event == null || event.getSession() == null) {
            return;
        }
        cleanupSession(event.getSession());
    }

    /**
     * Closes SSE emitters and clears notification storage for the given HTTP session.
     *
     * @param session HTTP session being signed out or destroyed
     */
    public void cleanupSession(final HttpSession session) {
        if (session == null) {
            return;
        }
        try {
            Object rawChannel = session.getAttribute(ApiKeySessionAuthentication.NOTIFICATION_CHANNEL_ATTR);
            if (rawChannel instanceof String channelId && !channelId.isBlank()) {
                sseBroadcaster.closeChannel(channelId);
                storePort.clear(channelId);
            }
        } catch (IllegalStateException ignored) {
            // Session already invalidated.
        }
    }
}
