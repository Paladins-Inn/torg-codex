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

package de.paladinsinn.torg.codex.data.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A standalone article such as credits, changelog, or rules clarifications.
 *
 * <p>Articles are pure textual content. Like all Torg entities they support
 * clearance-level gating via {@link TorgEntity#getClearanceLevel()}.
 * They do not carry product associations.
 */
@Entity
@Table(name = "torg_article")
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class Article extends TorgEntity {

    /** Full article body, potentially containing markup and markdown. */
    @Column(columnDefinition = "TEXT")
    private String text;

    /** Returns {@link #text} rendered and product-gate-filtered by the injected censor. */
    public String getText() {
        return render(text);
    }

    /**
     * Returns the raw, un-rendered {@link #text} without applying the censor.
     * Used by the entity&#8596;domain persistence mapper, which must preserve the
     * exact persisted value; product-gate rendering is a presentation concern.
     */
    public String getRawText() {
        return text;
    }
}
