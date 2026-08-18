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

import java.util.UUID;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A hierarchical tag used to categorise entities.
 *
 * <p>Tags form a tree via the {@link #parent} field, which stores the slug id
 * of the parent tag ({@code null} for root tags).
 */
@Entity
@Table(name = "torg_tag")
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class Tag extends TorgEntity {

    /**
     * Slug id of the parent tag, or {@code null} for a root-level tag.
     * Self-referencing is kept deliberately lightweight as a plain string
     * to avoid circular JPA relationships.
     */
    @Column(name = "parent_id")
    private UUID parent;
}
