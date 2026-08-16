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

package de.paladinsinn.torg.codex.domain.markup;

import java.util.Set;

/**
 * Filters and renders product-gated markup in entity text fields.
 *
 * <p>A {@code Censor} is created with a set of product-ids the current user owns
 * and a {@link TorgMarkupService}.  When {@link #apply(String)} is called on a raw
 * text field that may contain {@code <IF:product-id>…</IF>} blocks, it:
 * <ul>
 *   <li>Keeps content inside {@code <IF:id>…</IF>} only when the user owns {@code id}.</li>
 *   <li>Keeps content inside {@code <IF:!id>…</IF>} only when the user does <em>not</em>
 *       own {@code id} (typically a "buy this book" notice).</li>
 *   <li>Processes entity references, raw HTML, game tokens, and markdown.</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * Censor censor = Censor.of(markupService, Set.of("core-rulebook", "sourcebook-aysle"));
 * String html = cosm.getWorldLaws(censor);
 * }</pre>
 */
public final class Censor {

    private final TorgMarkupService markupService;
    private final Set<String> ownedProducts;

    private Censor(TorgMarkupService markupService, Set<String> ownedProducts) {
        this.markupService = markupService;
        this.ownedProducts = Set.copyOf(ownedProducts);
    }

    /**
     * Creates a {@code Censor} that shows content for the given owned product ids.
     *
     * @param markupService  the rendering pipeline to apply
     * @param ownedProducts  product-ids the user currently owns (without {@code ROLE_} prefix)
     */
    public static Censor of(TorgMarkupService markupService, Set<String> ownedProducts) {
        return new Censor(markupService, ownedProducts);
    }

    /**
     * Creates a {@code Censor} for a user who owns no products.
     * Only {@code <IF:!…>} (upsell) blocks remain visible.
     */
    public static Censor unauthenticated(TorgMarkupService markupService) {
        return new Censor(markupService, Set.of());
    }

    /**
     * Applies the full markup pipeline to {@code rawText} with product-gate filtering.
     *
     * @param rawText the raw text field value; returns {@code null} when {@code null} is passed
     * @return rendered HTML with product-gated blocks resolved
     */
    public String apply(String rawText) {
        if (rawText == null) {
            return null;
        }
        return markupService.render(rawText, ownedProducts);
    }
}
