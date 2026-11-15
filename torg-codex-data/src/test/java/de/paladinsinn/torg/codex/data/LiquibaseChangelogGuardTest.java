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

package de.paladinsinn.torg.codex.data;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

class LiquibaseChangelogGuardTest {
    private static final Path CHANGELOG_ROOT = Path.of("src", "main", "resources", "db");
    private static final Path BASELINE_MANIFEST = Path.of("src", "test", "resources", "liquibase-changelog-manifest.sha256");
    private static final Path APPLIED_IDENTITIES = Path.of("src", "test", "resources", "liquibase-changeset-identities.txt");

    /**
     * {@code torg-data-load.yaml} contains copyrighted game data and is only present in local,
     * fully-checked-out working copies (see .gitignore). The public repository checkout does not
     * contain this file, so the baseline entry for it must be ignored whenever the file is absent.
     */
    private static final String OPTIONAL_PROPRIETARY_FILE = "load/torg-data-load.yaml";

    @Test
    void liquibaseYamlFilesMatchTheCapturedBaseline() throws Exception {
        Map<String, String> current = currentManifest();
        Map<String, String> expected = expectedManifest();
        if (!current.containsKey(OPTIONAL_PROPRIETARY_FILE)) {
            expected.remove(OPTIONAL_PROPRIETARY_FILE);
        }

        assertThat(current).isEqualTo(expected);
    }

    /**
     * Liquibase identifies a changeset by file, id and author. Changing any of them for an applied
     * changeset makes existing databases run it again (constitution Principle IV), so every
     * recorded identity must still exist unchanged. The list is append-only.
     */
    @Test
    void appliedChangesetIdentitiesNeverChange() throws IOException {
        Set<String> current = currentIdentities();
        Set<String> missing = new TreeSet<>();
        for (String line : Files.readAllLines(APPLIED_IDENTITIES)) {
            String identity = line.trim();
            if (identity.isEmpty() || identity.startsWith("#")) {
                continue;
            }
            boolean optionalAndAbsent = identity.startsWith(OPTIONAL_PROPRIETARY_FILE + "|")
                    && !Files.exists(CHANGELOG_ROOT.resolve(OPTIONAL_PROPRIETARY_FILE));
            if (!optionalAndAbsent && !current.contains(identity)) {
                missing.add(identity);
            }
        }

        assertThat(missing).as("applied changeset identities (file|id|author) that were altered or removed").isEmpty();
    }

    @SuppressWarnings("unchecked")
    private Set<String> currentIdentities() throws IOException {
        Set<String> identities = new TreeSet<>();
        try (var stream = Files.walk(CHANGELOG_ROOT)) {
            for (Path path : stream.filter(Files::isRegularFile).filter(file -> file.toString().endsWith(".yaml")).toList()) {
                String file = CHANGELOG_ROOT.relativize(path).toString().replace('\\', '/');
                Map<String, Object> changelog = new Yaml().load(Files.readString(path));
                List<Map<String, Object>> entries = (List<Map<String, Object>>) changelog.getOrDefault("databaseChangeLog", List.of());
                for (Map<String, Object> entry : entries) {
                    Map<String, Object> changeSet = (Map<String, Object>) entry.get("changeSet");
                    if (changeSet != null) {
                        identities.add(file + "|" + changeSet.get("id") + "|" + changeSet.get("author"));
                    }
                }
            }
        }
        return identities;
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
