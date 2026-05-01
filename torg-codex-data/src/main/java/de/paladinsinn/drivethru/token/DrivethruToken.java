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

package de.paladinsinn.drivethru.token;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.paladinsinn.drivethru.resource.DriveThruResource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * An access token for the DriveThruRPG API.
 */
@Jacksonized
@SuperBuilder(toBuilder = true, setterPrefix = "")
@RequiredArgsConstructor
@Getter
@ToString(includeFieldNames = true, doNotUseGetters = true, onlyExplicitlyIncluded = true)
public class DrivethruToken extends DriveThruResource {

    @JsonProperty("access_token")
    private final String accessToken;

    @JsonProperty("customers_id")
    @ToString.Include
    private final String customerId;

    @JsonProperty("expires")
    @ToString.Include
    private final LocalDateTime expireTime;

    @JsonProperty("server_time")
    private final LocalDateTime serverTime;

    @JsonIgnore
    private final LocalDateTime localTime;

    @JsonIgnore
    private final Long expires;

    /**
     * Returns the Bearer token string ready for use in an {@code Authorization} header.
     *
     * @return {@code "Bearer <accessToken>"}
     */
    @JsonIgnore
    public String getBearerToken() {
        return "Bearer " + accessToken;
    }
}

