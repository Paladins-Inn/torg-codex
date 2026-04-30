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
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Read-only JPA view of Liquibase's internal {@code DATABASECHANGELOG} tracking table.
 *
 * <p>The table has no surrogate primary key; uniqueness is determined by the
 * composite ({@code id}, {@code author}, {@code filename}) triple, modelled via
 * {@link DatabaseChangeLogId}.
 *
 * <p>This class is annotated {@link Immutable} because the table is managed
 * exclusively by Liquibase and must never be written to via JPA.
 */
@Entity
@Table(name = "databasechangelog")
@Immutable
@Getter
@Setter
@NoArgsConstructor
@ToString
public class DatabaseChangeLog {

    @EmbeddedId
    private DatabaseChangeLogId changeLogId;

    /** Timestamp when this changeset was applied. */
    @Column(name = "dateexecuted", nullable = false)
    private LocalDateTime dateExecuted;

    /** Sequential application order across all changesets. */
    @Column(name = "orderexecuted", nullable = false)
    private Integer orderExecuted;

    /**
     * Result of the changeset execution.
     * Typical values: {@code "EXECUTED"}, {@code "RERAN"}, {@code "MARK_RAN"}, {@code "FAILED"}.
     */
    @Column(name = "exectype", length = 10, nullable = false)
    private String execType;

    /** MD5 checksum of the changeset content at the time of execution. */
    @Column(name = "md5sum", length = 35)
    private String md5sum;

    /** Human-readable description of what the changeset does. */
    @Column(name = "description", length = 255)
    private String description;

    /** Optional comment attached to the changeset. */
    @Column(name = "comments", length = 255)
    private String comments;

    /** Optional tag applied to this changeset (for rollback). */
    @Column(name = "tag", length = 255)
    private String tag;

    /** Liquibase version that executed the changeset. */
    @Column(name = "liquibase", length = 20)
    private String liquibase;

    /** Comma-separated list of contexts in which the changeset ran. */
    @Column(name = "contexts", length = 255)
    private String contexts;

    /** Comma-separated list of labels attached to the changeset. */
    @Column(name = "labels", length = 255)
    private String labels;

    /** Deployment correlation id (first 10 chars of a random UUID). */
    @Column(name = "deployment_id", length = 10)
    private String deploymentId;

    // -----------------------------------------------------------------------
    // Convenience accessors that delegate to the composite key
    // -----------------------------------------------------------------------

    /** Changeset id as declared in the changelog XML/YAML. */
    public String getId() {
        return changeLogId != null ? changeLogId.getId() : null;
    }

    /** Author as declared in the changelog. */
    public String getAuthor() {
        return changeLogId != null ? changeLogId.getAuthor() : null;
    }

    /** Classpath or filesystem location of the changelog file. */
    public String getFilename() {
        return changeLogId != null ? changeLogId.getFilename() : null;
    }
}

