package de.paladinsinn.torg.codex.data;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LiquibaseChangelogGuardTest {
    private static final Path CHANGELOG_ROOT = Path.of("src", "main", "resources", "db");
    private static final Path BASELINE_MANIFEST = Path.of("src", "test", "resources", "liquibase-changelog-manifest.sha256");

    @Test
    void liquibaseYamlFilesMatchTheCapturedBaseline() throws Exception {
        assertThat(currentManifest()).isEqualTo(expectedManifest());
    }

    private Map<String, String> expectedManifest() throws IOException {
        Map<String, String> manifest = new LinkedHashMap<>();
        for (String line : Files.readAllLines(BASELINE_MANIFEST)) {
            if (line.isBlank()) {
                continue;
            }
            String normalized = line.trim();
            manifest.put(normalized.substring(64), normalized.substring(0, 64));
        }
        return manifest;
    }

    private Map<String, String> currentManifest() throws IOException, NoSuchAlgorithmException {
        Map<String, String> manifest = new LinkedHashMap<>();
        try (var stream = Files.walk(CHANGELOG_ROOT)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".yaml"))
                    .sorted()
                    .forEach(path -> manifest.put(
                            CHANGELOG_ROOT.relativize(path).toString().replace('\\', '/'),
                            sha256(path)));
        }
        return manifest;
    }

    private String sha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Files.readAllBytes(path));
            StringBuilder builder = new StringBuilder();
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to hash " + path, exception);
        }
    }
}
