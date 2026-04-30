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
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Read-only JPA view of Liquibase's internal {@code DATABASECHANGELOGLOCK} table.
 *
 * <p>Liquibase uses this single-row table as a distributed lock during schema
 * migrations.  It must never be written to via JPA.
 */
@Entity
@Table(name = "databasechangeloglock")
@Immutable
@Getter
@Setter
@NoArgsConstructor
@ToString
public class DatabaseChangeLogLock {

    /** Surrogate identifier – Liquibase always inserts a single row with {@code id = 1}. */
    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    /** {@code true} while a Liquibase migration is running. */
    @Column(name = "locked", nullable = false)
    private boolean locked;

    /** Timestamp when the lock was acquired; {@code null} if not currently locked. */
    @Column(name = "lockgranted")
    private LocalDateTime lockGranted;

    /** Host description of the process that acquired the lock. */
    @Column(name = "lockedby", length = 255)
    private String lockedBy;
}

