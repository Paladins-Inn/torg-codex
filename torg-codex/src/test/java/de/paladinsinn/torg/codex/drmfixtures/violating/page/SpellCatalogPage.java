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

package de.paladinsinn.torg.codex.drmfixtures.violating.page;

import de.paladinsinn.torg.codex.data.adapter.out.persistence.JpaSpellPersistenceAdapter;
import lombok.RequiredArgsConstructor;

/** Violates A1: a server-rendered page/export outside {@code api..} using a concrete persistence adapter. */
@RequiredArgsConstructor
public class SpellCatalogPage {

    private final JpaSpellPersistenceAdapter adapter;

    public String render() {
        return adapter.findAll().toString();
    }
}
