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
 * Domain-agnostic severity tier for user notifications across reader and administrative surfaces (FR-005).
 */
public enum NotificationSeverity {
    INFO(true),
    SUCCESS(true),
    WARNING(false),
    ERROR(false);

    private final boolean autoDismiss;

    NotificationSeverity(final boolean autoDismiss) {
        this.autoDismiss = autoDismiss;
    }

    /**
     * Indicates whether UI surfaces should automatically dismiss this notification after a brief delay.
     *
     * @return {@code true} for {@link #INFO} and {@link #SUCCESS}; {@code false} for sticky severities
     */
    public boolean autoDismiss() {
        return autoDismiss;
    }
}
