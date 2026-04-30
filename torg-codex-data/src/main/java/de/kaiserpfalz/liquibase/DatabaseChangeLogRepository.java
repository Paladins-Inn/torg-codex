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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Spring Data repository for querying Liquibase's {@code DATABASECHANGELOG} table.
 *
 * <p>This repository is <em>read-only</em> by convention – the table is managed
 * exclusively by Liquibase and must never be modified via JPA.
 */
public interface DatabaseChangeLogRepository
        extends JpaRepository<DatabaseChangeLog, DatabaseChangeLogId> {

    /**
     * Returns all changesets applied by a specific author, ordered by execution sequence.
     */
    List<DatabaseChangeLog> findByChangeLogIdAuthorOrderByOrderExecuted(String author);

    /**
     * Returns all changesets with the given changeset id, regardless of author or file.
     */
    @Query("SELECT c FROM DatabaseChangeLog c WHERE c.changeLogId.id = :changesetId ORDER BY c.orderExecuted")
    List<DatabaseChangeLog> findByChangesetId(String changesetId);

    /**
     * Returns all changesets that were executed successfully.
     */
    List<DatabaseChangeLog> findByExecType(String execType);
}

