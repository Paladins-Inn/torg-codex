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

/**
 * Visual, ARIA, and Bootstrap dismissal presentation attributes for a toast notification.
 *
 * @param severityKey lowercase severity key ({@code info}, {@code success}, {@code warning}, {@code error})
 * @param iconCss     Bootstrap Icons class name
 * @param ariaRole    ARIA role ({@code status} or {@code alert})
 * @param ariaLive    ARIA live politeness ({@code polite} or {@code assertive})
 * @param autoHide    whether Bootstrap auto-dismisses the toast
 * @param delayMs     auto-hide delay in milliseconds ({@code 6000} or {@code 0})
 */
public record ToastPresentation(
        String severityKey,
        String iconCss,
        String ariaRole,
        String ariaLive,
        boolean autoHide,
        int delayMs) {
}
