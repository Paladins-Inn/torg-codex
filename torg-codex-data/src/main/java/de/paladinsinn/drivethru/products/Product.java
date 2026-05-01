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

import com.fasterxml.jackson.annotation.JsonProperty;

import de.paladinsinn.drivethru.resource.DriveThruResource;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * A product from DriveThruRPG.
 */
@Jacksonized
@SuperBuilder(toBuilder = true, setterPrefix = "")
@RequiredArgsConstructor
@Getter
@ToString(doNotUseGetters = true, includeFieldNames = true, onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Product extends DriveThruResource {

    @JsonProperty("products_id")
    @ToString.Include
    @EqualsAndHashCode.Include
    private final int productsId;

    @JsonProperty("products_name")
    @ToString.Include
    private final String productsName;

    @JsonProperty("publishers_id")
    private final int publisherId;

    @JsonProperty("publishers_name")
    @ToString.Include
    private final String publisherName;

    @JsonProperty("cover_url")
    @ToString.Include
    private final String coverURL;

    @JsonProperty("products_thumbnail")
    private final String thumbnail;

    @JsonProperty("products_thumbnail100")
    private final String thumbnail100;

    @JsonProperty("products_thumbnail80")
    private final String thumbnail80;

    @JsonProperty("products_thumbnail40")
    private final String thumbnail40;
}

