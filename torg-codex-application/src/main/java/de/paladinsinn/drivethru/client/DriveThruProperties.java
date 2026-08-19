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

package de.paladinsinn.drivethru.client;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for the DriveThruRPG REST client.
 *
 * <pre>
 * drivethru:
 *   base-url: <a href="https://www.drivethrurpg.com/api/v1">...</a>
 *   connect-timeout: 5s
 *   read-timeout: 10s
 * </pre>
 */
@ConfigurationProperties(prefix = "drivethru")
@Getter
@Setter
public class DriveThruProperties {

    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 5;
    private static final int DEFAULT_READ_TIMEOUT_SECONDS = 10;

    /** Base URL of the DriveThruRPG REST API. */
    private String baseUrl = "https://www.drivethrurpg.com/api/v1";

    /** HTTP connection timeout. */
    private Duration connectTimeout = Duration.ofSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS);

    /** HTTP read timeout. */
    private Duration readTimeout = Duration.ofSeconds(DEFAULT_READ_TIMEOUT_SECONDS);
}
