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

package de.paladinsinn.drivethru.resource;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * Wrapper for a single-object DriveThruRPG API response.
 *
 * <p>The API returns {@code {"status":"…","message":{…}}}.</p>
 *
 * @param <T> payload type
 */
@Jacksonized
@SuperBuilder(toBuilder = true, setterPrefix = "")
@RequiredArgsConstructor
@ToString(doNotUseGetters = true, includeFieldNames = true, onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DriveThruMessage<T> {

    @JsonProperty("status")
    @EqualsAndHashCode.Include
    @ToString.Include
    private final String status;

    @JsonProperty("message")
    @EqualsAndHashCode.Include
    @ToString.Include
    private final T message;

    @JsonIgnore
    public Optional<T> getData() {
        return Optional.ofNullable(message);
    }
}

