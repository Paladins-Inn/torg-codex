/*
 * Copyright (c) 2026. Roland T. Lichti <rlichti@kaiserpfalz-edv.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package de.paladinsinn.torg.codex.data.migration;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Repairs encoded prose lists after all catalog data has been loaded. */
public class ProseListToMarkdownChange implements CustomTaskChange {

    private static final Field[] FIELDS = {
        new Field("torg_item", "additional_features"),
        new Field("torg_threat", "quote"),
        new Field("torg_spell_list", "notes")
    };
    private int changedRows;

    @Override
    public void execute(Database database) throws CustomChangeException {
        final var connection = database.getConnection().getUnderlyingConnection();
        try {
            for (Field field : FIELDS) {
                changedRows += repair(connection, field);
            }
        } catch (SQLException exception) {
            throw new CustomChangeException("Could not repair encoded prose lists", exception);
        }
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    private int repair(java.sql.Connection connection, Field field) throws SQLException {
        int changed = 0;
        final String selectSql = "SELECT id, " + field.column() + " FROM " + field.table()
                + " WHERE " + field.column() + " IS NOT NULL";
        final String updateSql = "UPDATE " + field.table() + " SET " + field.column() + " = ? WHERE id = ?";
        try (PreparedStatement select = connection.prepareStatement(selectSql);
             ResultSet rows = select.executeQuery();
             PreparedStatement update = connection.prepareStatement(updateSql)) {
            while (rows.next()) {
                final String original = rows.getString(2);
                final var parsed = EncodedListTokenizer.parse(original);
                if (parsed.isEmpty()) {
                    continue;
                }
                final String replacement = EncodedListTokenizer.toMarkdown(parsed.orElseThrow());
                if (replacement.equals(original)) {
                    continue;
                }
                update.setString(1, replacement);
                update.setObject(2, rows.getObject(1));
                update.addBatch();
                changed++;
            }
            update.executeBatch();
        }
        return changed;
    }

    @Override
    public String getConfirmationMessage() {
        return "Repaired " + changedRows + " encoded prose values";
    }

    @Override
    public void setUp() {
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }

    private record Field(String table, String column) {
    }
}
