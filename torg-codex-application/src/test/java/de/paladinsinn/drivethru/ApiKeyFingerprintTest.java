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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
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

package de.paladinsinn.drivethru;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ApiKeyFingerprint} normalization, SHA-256 hex derivation,
 * redacted diagnostic output, and constant-time equality.
 */
class ApiKeyFingerprintTest {

    private static final int SHA256_HEX_LENGTH = 64;
    private static final String SAMPLE_KEY = "aabbccddeeff00112233445566778899aabbccdd";
    private static final String OTHER_KEY = "1122334455667788990011223344556677889900";

    @Test
    void normalizesSurroundingWhitespaceAndProducesLowercaseSha256Hex() {
        ApiKeyFingerprint trimmed = ApiKeyFingerprint.of(SAMPLE_KEY);
        ApiKeyFingerprint padded = ApiKeyFingerprint.of("  \t" + SAMPLE_KEY + "\n ");

        assertThat(trimmed.value())
                .hasSize(SHA256_HEX_LENGTH)
                .matches("[0-9a-f]{64}")
                .doesNotContain(SAMPLE_KEY);
        assertThat(padded).isEqualTo(trimmed).hasSameHashCodeAs(trimmed);
        assertThat(padded.value()).isEqualTo(trimmed.value());
    }

    @Test
    void distinguishesDifferentKeysAndRedactsToString() {
        ApiKeyFingerprint first = ApiKeyFingerprint.of(SAMPLE_KEY);
        ApiKeyFingerprint second = ApiKeyFingerprint.of(OTHER_KEY);

        assertThat(first).isNotEqualTo(second).isNotEqualTo(SAMPLE_KEY).isNotEqualTo(null);
        assertThat(first.toString())
                .contains("REDACTED")
                .doesNotContain(SAMPLE_KEY)
                .doesNotContain(first.value());
    }

    @Test
    void rejectsMissingBlankOrMalformedValues() {
        assertThatThrownBy(() -> ApiKeyFingerprint.of(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ApiKeyFingerprint.of("   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApiKeyFingerprint("not-a-sha256-digest"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApiKeyFingerprint("A".repeat(SHA256_HEX_LENGTH)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
