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

import java.util.List;

/**
 * View model for a titled card of key-value attributes/statistics on a single-element detail page.
 *
 * @param title card heading
 * @param items non-empty attribute rows
 */
public record AttributeGroupViewModel(String title, List<AttributeItem> items) {

    /**
     * Single labeled attribute value (either plain escaped text or pre-sanitized markup).
     *
     * @param label       field label
     * @param plainValue  plain text value when {@code markupValue == null}
     * @param markupValue censored and sanitized HTML when rendered as markup
     */
    public record AttributeItem(String label, String plainValue, SanitizedHtml markupValue) {
    }
}
