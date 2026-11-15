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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * An ordered library product returned by DriveThruRPG {@code /order_products} ({@code OrderProductAttributes}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@Getter
@Setter
@ToString(doNotUseGetters = true, onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class OwnedProduct extends DriveThruResource {

    @JsonProperty("productId")
    @JsonAlias("products_id")
    @ToString.Include
    @EqualsAndHashCode.Include
    private Integer productId;

    @JsonProperty("orderProductId")
    private Long orderProductId;

    @JsonProperty("orderId")
    private Long orderId;

    @JsonProperty("customerId")
    @JsonAlias("customers_id")
    private Long customerId;

    @JsonProperty("royaltyPublisherId")
    private Integer royaltyPublisherId;

    @JsonProperty("bundleId")
    private Integer bundleId;

    @JsonProperty("name")
    @JsonAlias("products_name")
    @ToString.Include
    private String name;

    @JsonProperty("isbn")
    private String isbn;

    @JsonProperty("archived")
    @JsonAlias("is_archived")
    private Integer archivedFlag;

    @JsonProperty("cover_url")
    private String coverURL;

    @JsonProperty("datePurchased")
    @JsonAlias("date_purchased")
    @ToString.Include
    private OffsetDateTime datePurchased;

    @JsonProperty("fileLastModified")
    private OffsetDateTime fileLastModified;

    @JsonProperty("fileLastDownloaded")
    private OffsetDateTime fileLastDownloaded;

    @JsonIgnore
    public int getId() {
        return productId != null ? productId : 0;
    }

    @JsonIgnore
    public void setId(final int id) {
        this.productId = id;
    }

    /**
     * Returns the archived status when supplied by DriveThruRPG.
     *
     * @return the optional archived status
     */
    @SuppressWarnings("unused")
    @JsonIgnore
    public Optional<String> getArchived() {
        return Optional.ofNullable(archivedFlag).map(String::valueOf);
    }

    @SuppressWarnings("unused")
    @JsonIgnore
    public void setArchived(final String archived) {
        this.archivedFlag = archived == null || archived.isBlank() ? null : Integer.valueOf(archived);
    }

    /**
     * Returns the cover URL when supplied by DriveThruRPG.
     *
     * @return the optional cover URL
     */
    @SuppressWarnings("unused")
    @JsonIgnore
    public Optional<String> getCoverURL() {
        return Optional.ofNullable(coverURL);
    }

    /**
     * Returns the purchase date when supplied by DriveThruRPG.
     *
     * @return the optional purchase date
     */
    @SuppressWarnings("unused")
    @JsonIgnore
    public Optional<OffsetDateTime> getDatePurchased() {
        return Optional.ofNullable(datePurchased);
    }
}
