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

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Deterministic, non-reversible lowercase hex SHA-256 digest of a normalized DriveThruRPG API key.
 *
 * <p>Used as the single session-identity and library-cache key so cleartext API keys never enter
 * session storage, cache keys, logs, or diagnostic output (FR-010, FR-018, ADR-023).</p>
 *
 * @param value 64-character lowercase hexadecimal SHA-256 digest
 */
public record ApiKeyFingerprint(String value) implements Serializable {

    private static final Pattern SHA256_HEX_PATTERN = Pattern.compile("^[0-9a-f]{64}$");
    private static final String REDACTED_REPR = "ApiKeyFingerprint[REDACTED]";

    /**
     * Validates that {@code value} is a non-null 64-character lowercase SHA-256 hex digest.
     *
     * @param value lowercase SHA-256 hex string
     */
    public ApiKeyFingerprint {
        if (value == null || !SHA256_HEX_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("ApiKeyFingerprint requires a 64-character lowercase hex SHA-256 digest.");
        }
    }

    /**
     * Derives the fingerprint of the supplied API key after stripping surrounding whitespace.
     *
     * @param apiKey submitted DriveThruRPG Application Key
     * @return non-reversible SHA-256 fingerprint
     */
    public static ApiKeyFingerprint of(final String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key must not be null or blank.");
        }
        String normalized = apiKey.strip();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8));
            return new ApiKeyFingerprint(HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK does not support SHA-256", exception);
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ApiKeyFingerprint that)) {
            return false;
        }
        return MessageDigest.isEqual(
                this.value.getBytes(StandardCharsets.UTF_8),
                that.value.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return REDACTED_REPR;
    }
}
