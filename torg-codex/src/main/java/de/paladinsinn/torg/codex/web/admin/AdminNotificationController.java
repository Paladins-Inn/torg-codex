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

package de.paladinsinn.torg.codex.web.admin;

import de.paladinsinn.torg.codex.web.notification.NotificationSurface;
import de.paladinsinn.torg.codex.web.notification.WebNotificationSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Administrative-surface inbound controller exposing the session-derived SSE toast stream and
 * CSRF-protected notification acknowledgment endpoint using isolated admin templates (US2, FR-005,
 * FR-019, ADR-020, ADR-024).
 */
@Controller
@RequiredArgsConstructor
public class AdminNotificationController {

    private final WebNotificationSupport notificationSupport;

    @GetMapping(path = WebNotificationSupport.ADMIN_STREAM_ENDPOINT, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Object streamToasts(
            final HttpServletRequest request,
            final HttpServletResponse response) {
        return notificationSupport.openStream(request, response, NotificationSurface.ADMIN);
    }

    @PostMapping(WebNotificationSupport.ADMIN_ACK_ENDPOINT)
    public ResponseEntity<Void> acknowledgeToasts(
            @RequestParam(name = "id", required = false) final List<String> ids,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        return notificationSupport.acknowledge(ids, request, response);
    }
}
