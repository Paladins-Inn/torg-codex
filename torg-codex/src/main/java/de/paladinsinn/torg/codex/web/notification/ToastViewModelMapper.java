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

import de.paladinsinn.torg.codex.api.mapper.TorgMappingSupport;
import de.paladinsinn.torg.codex.application.port.in.NotificationAction;
import de.paladinsinn.torg.codex.application.port.in.NotificationActionKind;
import de.paladinsinn.torg.codex.application.port.in.NotificationSeverity;
import de.paladinsinn.torg.codex.application.port.in.UserNotification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Locale;

/**
 * MapStruct mapper translating framework-free {@link UserNotification} values into composed
 * {@link ToastViewModel} presentation records (ADR-014, ADR-024).
 */
@Mapper(componentModel = "spring", uses = TorgMappingSupport.class)
public interface ToastViewModelMapper {

    int AUTO_DISMISS_DELAY_MS = 6_000;

    @Mapping(target = "content", expression = "java(toContent(notification))")
    @Mapping(target = "presentation", expression = "java(toPresentation(notification.severity()))")
    @Mapping(target = "action", source = "action")
    ToastViewModel toViewModel(UserNotification notification);

    List<ToastViewModel> toViewModels(List<UserNotification> notifications);

    default ToastContent toContent(final UserNotification notification) {
        String rawId = notification.id().toString();
        return new ToastContent("toast-" + rawId, rawId, notification.title(), notification.message());
    }

    default ToastPresentation toPresentation(final NotificationSeverity severity) {
        String key = severity.name().toLowerCase(Locale.ROOT);
        String icon = switch (severity) {
            case INFO -> "bi-info-circle-fill text-info";
            case SUCCESS -> "bi-check-circle-fill text-success";
            case WARNING -> "bi-exclamation-triangle-fill text-warning";
            case ERROR -> "bi-x-octagon-fill text-danger";
        };
        boolean autoHide = severity.autoDismiss();
        String role = autoHide ? "status" : "alert";
        String live = autoHide ? "polite" : "assertive";
        int delay = autoHide ? AUTO_DISMISS_DELAY_MS : 0;
        return new ToastPresentation(key, icon, role, live, autoHide, delay);
    }

    default ToastActionView toActionView(final NotificationAction action) {
        if (action == null) {
            return null;
        }
        String kind = action.kind() == NotificationActionKind.RELOAD_CURRENT_PAGE ? "reload" : "navigate";
        return new ToastActionView(action.label(), kind, action.targetPath());
    }
}
