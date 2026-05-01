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

package de.paladinsinn.drivethru.publishers;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.paladinsinn.drivethru.resource.DriveThruResource;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * A publisher from DriveThruRPG.
 */
@Jacksonized
@SuperBuilder(toBuilder = true, setterPrefix = "")
@RequiredArgsConstructor
@Getter
@ToString(doNotUseGetters = true, includeFieldNames = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Publisher extends DriveThruResource {

    /** DriveThruRPG publisher id. */
    @JsonProperty("publishers_id")
    @EqualsAndHashCode.Include
    private final int publisherId;

    /** DriveThruRPG publisher name. */
    @JsonProperty("publishers_name")
    private final String publisherName;
}

