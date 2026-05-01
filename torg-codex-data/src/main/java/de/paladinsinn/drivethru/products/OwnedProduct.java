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

package de.paladinsinn.drivethru.products;

import java.time.OffsetDateTime;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.paladinsinn.drivethru.resource.DriveThruResource;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * A product owned by a DriveThruRPG customer.
 */
@Jacksonized
@SuperBuilder(toBuilder = true, setterPrefix = "")
@RequiredArgsConstructor
@Getter
@ToString(doNotUseGetters = true, includeFieldNames = true, onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class OwnedProduct extends DriveThruResource {

    @JsonProperty("products_id")
    @ToString.Include
    @EqualsAndHashCode.Include
    private final int id;

    @JsonProperty("products_name")
    @ToString.Include
    private final String name;

    @JsonProperty("is_archived")
    private final Optional<String> archived;

    @JsonProperty("cover_url")
    private final Optional<String> coverURL;

    @JsonProperty("date_purchased")
    @ToString.Include
    private final Optional<OffsetDateTime> datePurchased;
}

