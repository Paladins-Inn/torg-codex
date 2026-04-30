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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package de.kaiserpfalz.liquibase;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Composite primary key for {@link DatabaseChangeLog}.
 *
 * <p>Liquibase's {@code DATABASECHANGELOG} table does not define a surrogate PK;
 * the logical identity of each applied changeset is the triple
 * (id, author, filename).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class DatabaseChangeLogId implements Serializable {

    /** Changeset id as declared in the Liquibase changelog file. */
    @Column(name = "id", length = 255, nullable = false)
    private String id;

    /** Author as declared in the Liquibase changelog file. */
    @Column(name = "author", length = 255, nullable = false)
    private String author;

    /** Classpath or filesystem path to the changelog file. */
    @Column(name = "filename", length = 255, nullable = false)
    private String filename;
}

