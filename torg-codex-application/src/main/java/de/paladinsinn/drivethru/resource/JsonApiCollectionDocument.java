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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Paginated JSON:API collection document ({@code links}, {@code meta}, {@code data}, {@code included}).
 *
 * @param <A> item attributes type
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class JsonApiCollectionDocument<A> {

    @ToString.Include
    private JsonApiLinks links;

    @ToString.Include
    private JsonApiMeta meta;

    @EqualsAndHashCode.Include
    private List<JsonApiResource<A>> data;

    private List<JsonApiResource<Map<String, Object>>> included;

    @JsonIgnore
    public List<A> getAttributesList() {
        if (data == null) {
            return List.of();
        }
        return data.stream()
                .filter(Objects::nonNull)
                .map(JsonApiResource::getAttributes)
                .filter(Objects::nonNull)
                .toList();
    }
}
