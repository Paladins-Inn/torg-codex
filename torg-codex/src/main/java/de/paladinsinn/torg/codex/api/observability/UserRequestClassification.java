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

package de.paladinsinn.torg.codex.api.observability;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Set;

/**
 * Classifies incoming HTTP requests into excluded infrastructure/asset traffic, TRACE-only
 * background notification controls, or DEBUG/INFO user requests using strictly bounded route
 * labels free of caller slugs, UUIDs, or query strings (US4, T054, FR-015, FR-016, ADR-022).
 */
public final class UserRequestClassification {

    public enum RequestTier {
        EXCLUDED,
        BACKGROUND_CONTROL,
        USER_REQUEST
    }

    public record Classification(RequestTier tier, String startRoute, String fallbackCompletionRoute) {
    }

    private static final Set<String> BACKGROUND_CONTROL_PATHS = Set.of(
            "/notifications/stream",
            "/notifications/ack",
            "/admin/notifications/stream",
            "/admin/notifications/ack");

    private UserRequestClassification() {
    }

    /**
     * Classifies the normalized context-relative request path without reading query strings,
     * headers, or parameters.
     *
     * @param request current HTTP request
     * @return bounded classification
     */
    public static Classification classify(final HttpServletRequest request) {
        String path = normalizePath(request);
        if (isExcluded(path)) {
            return new Classification(RequestTier.EXCLUDED, "excluded", "excluded");
        }
        if (BACKGROUND_CONTROL_PATHS.contains(path)) {
            return new Classification(RequestTier.BACKGROUND_CONTROL, "notification", path);
        }
        if ("/login".equals(path) || "/logout".equals(path)) {
            return new Classification(RequestTier.USER_REQUEST, "auth", path);
        }
        if ("/library/reload".equals(path) || path.startsWith("/library/")) {
            return new Classification(
                    RequestTier.USER_REQUEST,
                    "library",
                    "/library/reload".equals(path) ? "/library/reload" : "unmatched");
        }
        if ("/api".equals(path) || path.startsWith("/api/")) {
            return new Classification(RequestTier.USER_REQUEST, "api", "unmatched");
        }
        if ("/admin".equals(path) || path.startsWith("/admin/")) {
            return new Classification(RequestTier.USER_REQUEST, "admin-page", "unmatched");
        }
        return new Classification(RequestTier.USER_REQUEST, "reader-page", "unmatched");
    }

    /**
     * Resolves the bounded route pattern recorded on request completion.
     *
     * @param request        completed HTTP request
     * @param classification initial request classification
     * @return matched Spring MVC route pattern or bounded fallback
     */
    public static String resolveCompletionRoute(
            final HttpServletRequest request,
            final Classification classification) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern != null) {
            String patternText = String.valueOf(pattern);
            if (!patternText.isBlank()) {
                return patternText;
            }
        }
        return classification.fallbackCompletionRoute();
    }

    private static boolean isExcluded(final String path) {
        return "/favicon.ico".equals(path)
                || "/actuator".equals(path)
                || path.startsWith("/actuator/")
                || path.startsWith("/webjars/")
                || path.startsWith("/themes/")
                || path.startsWith("/js/")
                || path.startsWith("/css/")
                || path.startsWith("/images/");
    }

    private static String normalizePath(final HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || uri.isEmpty()) {
            return "/";
        }
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            String stripped = uri.substring(contextPath.length());
            return stripped.isEmpty() ? "/" : stripped;
        }
        return uri;
    }
}
