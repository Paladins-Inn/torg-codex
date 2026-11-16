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
 * Target recipient scope for an event-driven user notification (ADR-024).
 *
 * <p>Supports addressing either a single session notification channel or all currently
 * authenticated user sessions (prepared for administrative broadcast use).</p>
 */
public sealed interface NotificationTarget
        permits NotificationTarget.SessionChannel, NotificationTarget.AllAuthenticatedUsers {

    NotificationTarget ALL_AUTHENTICATED = new AllAuthenticatedUsers();

    /**
     * Targets a single browser session identified by its opaque notification channel ID.
     *
     * @param channelId opaque session notification channel identifier
     */
    record SessionChannel(String channelId) implements NotificationTarget {
    }

    /**
     * Targets every currently authenticated user session across reader and administrative surfaces,
     * excluding anonymous sessions.
     */
    record AllAuthenticatedUsers() implements NotificationTarget {
    }

    static NotificationTarget session(final String channelId) {
        return new SessionChannel(channelId);
    }

    static NotificationTarget allAuthenticatedUsers() {
        return ALL_AUTHENTICATED;
    }
}
