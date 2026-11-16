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
 * Optional interactive action offered on a {@link UserNotification} toast.
 *
 * @param label      short user-facing button/link label
 * @param kind       interaction type
 * @param targetPath internal root-relative path for {@link NotificationActionKind#NAVIGATE_URL}, or {@code null}
 */
public record NotificationAction(
        String label,
        NotificationActionKind kind,
        String targetPath) {

    /**
     * Creates a one-click reload action for the currently open page.
     *
     * @param label button label
     * @return reload action
     */
    public static NotificationAction reloadCurrentPage(final String label) {
        return new NotificationAction(label, NotificationActionKind.RELOAD_CURRENT_PAGE, null);
    }

    /**
     * Creates an internal navigation action pointing to an application-local path.
     *
     * @param label      link label
     * @param targetPath internal root-relative path starting with {@code /}
     * @return navigation action
     */
    public static NotificationAction navigateUrl(final String label, final String targetPath) {
        return new NotificationAction(label, NotificationActionKind.NAVIGATE_URL, targetPath);
    }
}
