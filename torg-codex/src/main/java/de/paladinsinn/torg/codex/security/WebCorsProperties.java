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

package de.paladinsinn.torg.codex.security;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuration properties governing restrictive cross-origin access for browser UI endpoints
 * ({@code torg.codex.web.cors.*}, FR-019, ADR-023).
 *
 * <p>Defaults to an empty origin allowlist so only same-origin browser requests are accepted unless
 * trusted origins are explicitly configured.</p>
 */
@ConfigurationProperties(prefix = "torg.codex.web.cors")
@Validated
@Getter
public class WebCorsProperties {

    private List<String> allowedOrigins = List.of();

    /**
     * Replaces the configured browser UI origin allowlist, stripping blank entries and rejecting wildcards.
     *
     * @param origins configured origin list
     */
    public void setAllowedOrigins(final List<String> origins) {
        if (origins == null || origins.isEmpty()) {
            this.allowedOrigins = List.of();
            return;
        }
        List<String> cleaned = origins.stream()
                .filter(origin -> origin != null && !origin.isBlank())
                .map(String::strip)
                .peek(origin -> {
                    if ("*".equals(origin)
                            || (!origin.startsWith("http://") && !origin.startsWith("https://"))) {
                        throw new IllegalArgumentException(
                                "Invalid browser UI CORS allowed origin: " + origin);
                    }
                })
                .distinct()
                .toList();
        this.allowedOrigins = List.copyOf(cleaned);
    }
}
