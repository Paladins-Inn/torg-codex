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
import de.paladinsinn.drivethru.resource.JsonApiDocument;
import de.paladinsinn.drivethru.resource.JsonApiResource;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A catalog product from DriveThruRPG {@code /products/{productId}}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@Getter
@Setter
@ToString(doNotUseGetters = true, onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Product extends DriveThruResource {

    private static final String IMAGES_BASE_URL = "https://api.drivethrurpg.com/images/";

    @JsonProperty("productId")
    @JsonAlias("products_id")
    @ToString.Include
    @EqualsAndHashCode.Include
    private Integer productIdValue;

    @JsonProperty("name")
    @JsonAlias("products_name")
    @ToString.Include
    private String productsName;

    @JsonProperty("description")
    private ProductDescription description;

    @JsonProperty("publisherId")
    @JsonAlias("publishers_id")
    private Integer publisherIdValue;

    @JsonProperty("publishers_name")
    @ToString.Include
    private String publisherName;

    @JsonProperty("image")
    @JsonAlias("cover_url")
    @ToString.Include
    private String coverURL;

    @JsonProperty("webImage")
    private String webImage;

    @JsonProperty("thumbnail")
    @JsonAlias("products_thumbnail")
    private String thumbnail;

    @JsonProperty("thumbnail100")
    @JsonAlias("products_thumbnail100")
    private String thumbnail100;

    @JsonProperty("thumbnail80")
    @JsonAlias("products_thumbnail80")
    private String thumbnail80;

    @JsonProperty("thumbnail40")
    @JsonAlias("products_thumbnail40")
    private String thumbnail40;

    @JsonIgnore
    public int getProductsId() {
        return productIdValue != null ? productIdValue : 0;
    }

    @JsonIgnore
    public void setProductsId(final int productsId) {
        this.productIdValue = productsId;
    }

    @JsonIgnore
    public String getProductsName() {
        if (productsName != null && !productsName.isBlank()) {
            return productsName;
        }
        return description != null ? description.getName() : productsName;
    }

    @JsonIgnore
    public int getPublisherId() {
        return publisherIdValue != null ? publisherIdValue : 0;
    }

    @JsonIgnore
    public void setPublisherId(final int publisherId) {
        this.publisherIdValue = publisherId;
    }

    @JsonIgnore
    public String getCoverURL() {
        return resolveImageUrl(coverURL);
    }

    /**
     * Extracts a {@link Product} from a JSON:API document, enriching {@code publisherName}
     * from sideloaded {@code included} publisher resources when present.
     *
     * @param document JSON:API product document
     * @return optional enriched product
     */
    public static Optional<Product> fromDocument(final JsonApiDocument<Product> document) {
        if (document == null) {
            return Optional.empty();
        }
        Optional<Product> productOpt = document.getAttributes();
        productOpt.ifPresent(product -> enrichPublisherName(product, document.getIncluded()));
        return productOpt;
    }

    private static void enrichPublisherName(
            final Product product,
            final List<JsonApiResource<Map<String, Object>>> included) {
        if (product.getPublisherName() != null || included == null) {
            return;
        }
        included.stream()
                .filter(Objects::nonNull)
                .filter(item -> "Publisher".equalsIgnoreCase(item.getType()))
                .map(JsonApiResource::getAttributes)
                .filter(Objects::nonNull)
                .filter(attrs -> matchesPublisher(product.getPublisherId(), attrs.get("publisherId")))
                .map(attrs -> attrs.get("name"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst()
                .ifPresent(product::setPublisherName);
    }

    private static boolean matchesPublisher(final int expectedId, final Object candidate) {
        if (expectedId <= 0 || candidate == null) {
            return true;
        }
        if (candidate instanceof Number number) {
            return number.intValue() == expectedId;
        }
        return String.valueOf(expectedId).equals(String.valueOf(candidate));
    }

    private static String resolveImageUrl(final String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return rawPath;
        }
        if (rawPath.startsWith("http://") || rawPath.startsWith("https://")) {
            return rawPath;
        }
        String normalized = rawPath.startsWith("/") ? rawPath.substring(1) : rawPath;
        return IMAGES_BASE_URL + normalized;
    }

    /**
     * Embedded localized description block on DriveThruRPG vBeta product resources.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @Getter
    @Setter
    public static class ProductDescription {
        private String name;
        private String slug;
        private String shortDescription;
    }
}
