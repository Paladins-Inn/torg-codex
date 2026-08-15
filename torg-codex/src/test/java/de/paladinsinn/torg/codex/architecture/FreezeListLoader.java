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
