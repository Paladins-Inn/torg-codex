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

/**
 * Framework-free integration event payload published via the in-process Spring Events bus and
 * bridged into {@link UserNotificationCommand} (Constitution II, ADR-023, ADR-024).
 *
 * <p>Supports targeting either a single session channel ({@link NotificationTarget.SessionChannel})
 * or all currently logged-in user sessions ({@link NotificationTarget.AllAuthenticatedUsers},
 * prepared for the administration frontend).</p>
 *
 * @param target   recipient target scope (single session channel or all authenticated users)
 * @param severity notification severity
 * @param title    concise plain-text heading
 * @param message  plain-text message body
 * @param action   optional interactive action, or {@code null}
 */
public record UserNotificationRequested(
        NotificationTarget target,
        NotificationSeverity severity,
        String title,
        String message,
        NotificationAction action) {

    public static UserNotificationRequested info(
            final String channelId,
            final String title,
            final String message) {
        return new UserNotificationRequested(
                NotificationTarget.session(channelId), NotificationSeverity.INFO, title, message, null);
    }

    public static UserNotificationRequested success(
            final String channelId,
            final String title,
            final String message,
            final NotificationAction action) {
        return new UserNotificationRequested(
                NotificationTarget.session(channelId), NotificationSeverity.SUCCESS, title, message, action);
    }

    public static UserNotificationRequested warning(
            final String channelId,
            final String title,
            final String message,
            final NotificationAction action) {
        return new UserNotificationRequested(
                NotificationTarget.session(channelId), NotificationSeverity.WARNING, title, message, action);
    }

    public static UserNotificationRequested error(
            final String channelId,
            final String title,
            final String message,
            final NotificationAction action) {
        return new UserNotificationRequested(
                NotificationTarget.session(channelId), NotificationSeverity.ERROR, title, message, action);
    }

    public static UserNotificationRequested broadcast(
            final NotificationSeverity severity,
            final String title,
            final String message,
            final NotificationAction action) {
        return new UserNotificationRequested(
                NotificationTarget.allAuthenticatedUsers(), severity, title, message, action);
    }

    public static UserNotificationRequested broadcastInfo(final String title, final String message) {
        return broadcast(NotificationSeverity.INFO, title, message, null);
    }

    public static UserNotificationRequested broadcastWarning(final String title, final String message) {
        return broadcast(NotificationSeverity.WARNING, title, message, null);
    }
}
