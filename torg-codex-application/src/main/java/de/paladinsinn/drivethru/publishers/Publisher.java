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

/**
 * A publisher resource from DriveThruRPG {@code /publishers/{publisherId}} ({@code PublisherAttributes}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@Getter
@Setter
@ToString(doNotUseGetters = true)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Publisher extends DriveThruResource {

    /** DriveThruRPG publisher id. */
    @JsonProperty("publisherId")
    @JsonAlias("publishers_id")
    @EqualsAndHashCode.Include
    private Integer publisherIdValue;

    /** DriveThruRPG publisher name. */
    @JsonProperty("name")
    @JsonAlias("publishers_name")
    private String publisherName;

    /** URL-friendly publisher slug. */
    @JsonProperty("slug")
    private String slug;

    /** Relative publisher thumbnail image path. */
    @JsonProperty("thumbnail")
    private String thumbnail;

    @JsonIgnore
    public int getPublisherId() {
        return publisherIdValue != null ? publisherIdValue : 0;
    }

    @JsonIgnore
    public void setPublisherId(final int publisherId) {
        this.publisherIdValue = publisherId;
    }
}
