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

package de.paladinsinn.torg.codex.api.security;

import de.paladinsinn.torg.codex.domain.markup.Censor;
import de.paladinsinn.torg.codex.domain.markup.TorgMarkupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Primary-adapter component that creates a {@link Censor} for the current request.
 *
 * <p>The set of owned products is resolved solely through {@link ProductOwnershipResolver} — the
 * single, principal-type-agnostic {@code ROLE_<codex-id>}-authority mechanism required by
 * constitution Principle V (feature {@code 001-unify-censoring-authorization}). This class no
 * longer derives ownership from any authentication-provider-specific principal.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserCensorFactory {

    private final ProductOwnershipResolver ownershipResolver;
    private final TorgMarkupService markupService;

    public Censor create() {
        return Censor.of(markupService, ownershipResolver.resolve());
    }
}
