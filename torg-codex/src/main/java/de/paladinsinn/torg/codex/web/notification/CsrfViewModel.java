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

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;

/**
 * Presentation model carrying resolved Spring Security anti-forgery token metadata for forms and
 * same-origin dynamic UI requests (FR-019, ADR-023).
 *
 * <p>Resolved only on pages that render state-changing controls so anonymous catalog views never
 * force creation of an {@code HttpSession}.</p>
 *
 * @param parameterName form parameter name (default {@code _csrf})
 * @param headerName    HTTP header name (default {@code X-CSRF-TOKEN})
 * @param token         masked per-request anti-forgery token value
 */
public record CsrfViewModel(
        String parameterName,
        String headerName,
        String token) {

    /**
     * Resolves the current request's deferred {@link CsrfToken} into an immutable view model.
     *
     * @param request current HTTP request
     * @return resolved CSRF metadata, or {@code null} when CSRF protection is not active on the request
     */
    public static CsrfViewModel fromRequest(final HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object attr = request.getAttribute(CsrfToken.class.getName());
        if (!(attr instanceof CsrfToken csrfToken)) {
            attr = request.getAttribute("_csrf");
        }
        if (!(attr instanceof CsrfToken csrfToken)) {
            return null;
        }
        String tokenValue = csrfToken.getToken();
        if (tokenValue == null || tokenValue.isBlank()) {
            return null;
        }
        return new CsrfViewModel(csrfToken.getParameterName(), csrfToken.getHeaderName(), tokenValue);
    }
}
