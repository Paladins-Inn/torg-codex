package de.paladinsinn.torg.codex.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies (T123) that every data row in {@code specs/architecture-migration/freeze-list.md}
 * has a non-empty value for all eight required columns (id, module, violating
 * class/dependency, violated rule, rationale, baseline task, planned removal phase, status),
 * and that {@code status} is one of the three contractually allowed values.
 *
 * <p>Unlike {@link FreezeListLoader#load()}, which silently drops malformed rows (rows that do
 * not split into exactly 8 columns) so that a broken row cannot accidentally suppress
 * unrelated violations, this test reads the raw table rows directly and fails loudly if any
 * row is missing a required field, guaranteeing the format contract documented at the bottom
 * of freeze-list.md is actually enforced rather than merely stated.
 */
class FreezeListFormatTest {

    private static final Path FREEZE_LIST_PATH = Path.of("..", "specs", "architecture-migration", "freeze-list.md");
    private static final Set<String> ALLOWED_STATUSES = Set.of("open", "removed", "accepted deviation");

    @Test
    void everyFreezeListRowHasAllEightRequiredFields() {
        List<String> rows = readDataRows();
        assertTrue(!rows.isEmpty(), "freeze-list.md should contain at least one data row (or the "
                + "explicit example accepted-deviation row); if it is now truly empty, update this "
                + "test alongside closing the last freeze-list entry per T126/T127");

        List<String> violations = rows.stream()
                .map(FreezeListFormatTest::splitColumns)
                .<String>mapMulti((columns, consumer) -> {
                    if (columns.size() != 8) {
                        consumer.accept("row does not have exactly 8 columns: " + columns);
                        return;
                    }
                    String[] names = {
                            "id", "module", "violating class/dependency", "violated rule",
                            "rationale", "baseline task", "planned removal phase", "status"
                    };
                    for (int i = 0; i < 8; i++) {
                        if (columns.get(i).isBlank()) {
                            consumer.accept("column '" + names[i] + "' is blank in row: " + columns);
                        }
                    }
                    String status = columns.get(7);
                    if (!status.isBlank() && !ALLOWED_STATUSES.contains(status)) {
                        consumer.accept("column 'status' has an invalid value '" + status
                                + "' (must be one of " + ALLOWED_STATUSES + ") in row: " + columns);
                    }
                })
                .toList();

        if (!violations.isEmpty()) {
            fail("freeze-list.md format contract violated:" + System.lineSeparator()
                    + String.join(System.lineSeparator(), violations));
        }
    }

    private static List<String> readDataRows() {
        try {
            return Files.readAllLines(FREEZE_LIST_PATH).stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("|"))
                    .filter(line -> !line.contains("| id "))
                    .filter(line -> !line.replace("|", "").replace("-", "").isBlank())
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to read freeze list from " + FREEZE_LIST_PATH.toAbsolutePath(), exception);
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
