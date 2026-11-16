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

package de.paladinsinn.torg.codex.application.port.in;

import java.util.List;

/**
 * Driving port for queuing domain-agnostic user notifications into a single session channel or
 * broadcasting to all currently authenticated user sessions (ADR-024).
 */
public interface UserNotificationCommand {

    UserNotification notifySession(
            String channelId,
            NotificationSeverity severity,
            String title,
            String message,
            NotificationAction action);

    List<UserNotification> notifyAllAuthenticated(
            NotificationSeverity severity,
            String title,
            String message,
            NotificationAction action);

    default List<UserNotification> dispatch(
            final NotificationTarget target,
            final NotificationSeverity severity,
            final String title,
            final String message,
            final NotificationAction action) {
        if (target == null) {
            throw new IllegalArgumentException("target must not be null.");
        }
        return switch (target) {
            case NotificationTarget.SessionChannel session ->
                    List.of(notifySession(session.channelId(), severity, title, message, action));
            case NotificationTarget.AllAuthenticatedUsers ignored ->
                    notifyAllAuthenticated(severity, title, message, action);
        };
    }

    default UserNotification notifyInfo(final String channelId, final String title, final String message) {
        return notifySession(channelId, NotificationSeverity.INFO, title, message, null);
    }

    default UserNotification notifyInfo(
            final String channelId,
            final String title,
            final String message,
            final NotificationAction action) {
        return notifySession(channelId, NotificationSeverity.INFO, title, message, action);
    }

    default UserNotification notifySuccess(final String channelId, final String title, final String message) {
        return notifySession(channelId, NotificationSeverity.SUCCESS, title, message, null);
    }

    default UserNotification notifySuccess(
            final String channelId,
            final String title,
            final String message,
            final NotificationAction action) {
        return notifySession(channelId, NotificationSeverity.SUCCESS, title, message, action);
    }

    default UserNotification notifyWarning(final String channelId, final String title, final String message) {
        return notifySession(channelId, NotificationSeverity.WARNING, title, message, null);
    }

    default UserNotification notifyWarning(
            final String channelId,
            final String title,
            final String message,
            final NotificationAction action) {
        return notifySession(channelId, NotificationSeverity.WARNING, title, message, action);
    }

    default UserNotification notifyError(final String channelId, final String title, final String message) {
        return notifySession(channelId, NotificationSeverity.ERROR, title, message, null);
    }

    default UserNotification notifyError(
            final String channelId,
            final String title,
            final String message,
            final NotificationAction action) {
        return notifySession(channelId, NotificationSeverity.ERROR, title, message, action);
    }
}
