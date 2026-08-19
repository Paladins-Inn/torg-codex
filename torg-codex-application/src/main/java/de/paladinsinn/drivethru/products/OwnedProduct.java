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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.paladinsinn.drivethru.resource.DriveThruResource;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * A product owned by a DriveThruRPG customer.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@Getter
@Setter
@ToString(doNotUseGetters = true, onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class OwnedProduct extends DriveThruResource {

    @JsonProperty("products_id")
    @ToString.Include
    @EqualsAndHashCode.Include
    private int id;

    @JsonProperty("products_name")
    @ToString.Include
    private String name;

    @JsonProperty("is_archived")
    private String archived;

    @JsonProperty("cover_url")
    private String coverURL;

    @JsonProperty("date_purchased")
    @ToString.Include
    private OffsetDateTime datePurchased;

    /**
     * Returns the archived status when supplied by DriveThruRPG.
     *
     * @return the optional archived status
     */
    @SuppressWarnings("unused")
    public Optional<String> getArchived() {
        return Optional.ofNullable(archived);
    }

    /**
     * Returns the cover URL when supplied by DriveThruRPG.
     *
     * @return the optional cover URL
     */
    @SuppressWarnings("unused")
    public Optional<String> getCoverURL() {
        return Optional.ofNullable(coverURL);
    }

    /**
     * Returns the purchase date when supplied by DriveThruRPG.
     *
     * @return the optional purchase date
     */
    @SuppressWarnings("unused")
    public Optional<OffsetDateTime> getDatePurchased() {
        return Optional.ofNullable(datePurchased);
    }
}
