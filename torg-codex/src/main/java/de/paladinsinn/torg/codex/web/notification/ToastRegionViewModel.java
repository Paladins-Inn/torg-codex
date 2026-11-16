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

import java.util.List;

/**
 * View model configuring the server-rendered toast container and optional SSE stream subscription
 * on reader and administrative layouts (ADR-024).
 *
 * @param initialToasts  pending session toasts plus any request-scoped error toasts
 * @param streamEndpoint SSE stream URL when a session channel exists, or {@code null}
 * @param ackEndpoint    CSRF-protected acknowledgment URL when a session channel exists, or {@code null}
 */
public record ToastRegionViewModel(
        List<ToastViewModel> initialToasts,
        String streamEndpoint,
        String ackEndpoint) {

    public ToastRegionViewModel {
        initialToasts = initialToasts == null ? List.of() : List.copyOf(initialToasts);
    }

    public static ToastRegionViewModel empty() {
        return new ToastRegionViewModel(List.of(), null, null);
    }
}
