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
 * ERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
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

package de.paladinsinn.torg.codex.web.codex.model;

import de.paladinsinn.torg.codex.web.notification.CsrfViewModel;

/**
 * View model for dedicated reader authentication pages ({@code /login} and {@code /logout}, FR-014, FR-019).
 *
 * @param nav             shared global navigation model
 * @param errorMessage    optional validation/authentication error message
 * @param signedOutBanner whether to display a signed-out confirmation banner
 * @param csrf            resolved CSRF token metadata for form rendering
 */
public record AuthPageViewModel(
        NavContextViewModel nav,
        String errorMessage,
        boolean signedOutBanner,
        CsrfViewModel csrf) {

    /**
     * Backward-compatible constructor deriving CSRF metadata from {@code nav} when available.
     *
     * @param nav             navigation model
     * @param errorMessage    optional error message
     * @param signedOutBanner signed-out banner flag
     */
    public AuthPageViewModel(
            final NavContextViewModel nav,
            final String errorMessage,
            final boolean signedOutBanner) {
        this(nav, errorMessage, signedOutBanner, nav != null ? nav.csrf() : null);
    }
}
