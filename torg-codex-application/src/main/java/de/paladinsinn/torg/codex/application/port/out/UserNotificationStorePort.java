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

package de.paladinsinn.torg.codex.application.port.out;

import de.paladinsinn.torg.codex.application.port.in.UserNotification;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Driven port storing pending notifications per opaque session channel and tracking currently
 * authenticated user channels for administrative broadcasts (ADR-024).
 */
public interface UserNotificationStorePort {

    void enqueue(String channelId, UserNotification notification);

    List<UserNotification> pending(String channelId, int limit);

    void acknowledge(String channelId, Set<UUID> notificationIds);

    void registerAuthenticatedChannel(String channelId);

    Set<String> authenticatedChannels();

    void clear(String channelId);
}
