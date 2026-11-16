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

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable generic user notification queued for delivery to a user session channel (FR-005).
 *
 * @param id        unique notification identifier used for deduplication and acknowledgment
 * @param severity  display and dismissal severity
 * @param title     concise plain-text heading
 * @param message   plain-text body free of secrets or raw upstream payloads
 * @param createdAt creation timestamp
 * @param action    optional interactive action, or {@code null}
 */
public record UserNotification(
        UUID id,
        NotificationSeverity severity,
        String title,
        String message,
        Instant createdAt,
        NotificationAction action) {
}
