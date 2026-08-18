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

package de.paladinsinn.torg.codex.architecture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class FreezeListLoader {
    private static final Path FREEZE_LIST_PATH = Path.of("..", "specs", "architecture-migration", "freeze-list.md");

    private FreezeListLoader() {
    }

    static List<FreezeListEntry> load() {
        try {
            return Files.readAllLines(FREEZE_LIST_PATH).stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("|"))
                    .map(FreezeListLoader::splitColumns)
                    .filter(columns -> columns.size() == 8)
                    .filter(columns -> !"id".equalsIgnoreCase(columns.getFirst()))
                    .filter(columns -> !columns.getFirst().matches("-+"))
                    .map(columns -> new FreezeListEntry(
                            columns.get(0),
                            columns.get(1),
                            columns.get(2),
                            columns.get(3),
                            columns.get(4),
                            columns.get(5),
                            columns.get(6),
                            columns.get(7)))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load freeze list from " + FREEZE_LIST_PATH.toAbsolutePath(), exception);
        }
    }

    private static List<String> splitColumns(String line) {
        String trimmed = line.substring(1, line.length() - 1);
        return List.of(trimmed.split("\\|", -1)).stream()
                .map(String::trim)
                .map(value -> value.replace("`", ""))
                .toList();
    }
}
