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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.paladinsinn.drivethru.resource.DriveThruResource;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * An access token for the DriveThruRPG API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString(doNotUseGetters = true, onlyExplicitlyIncluded = true)
public class DrivethruToken extends DriveThruResource {

    @JsonProperty("token")
    @JsonAlias("access_token")
    private String accessToken;

    @JsonProperty("refreshToken")
    private String refreshToken;

    @JsonProperty("refreshTokenTTL")
    private Long refreshTokenTTL;

    @JsonIgnore
    private String applicationKey;

    @JsonIgnore
    private final String subject;

    @JsonProperty("customers_id")
    @ToString.Include
    private String customerId;

    @JsonProperty("expires")
    @ToString.Include
    private LocalDateTime expireTime;

    @JsonProperty("server_time")
    private LocalDateTime serverTime;

    @JsonIgnore
    private LocalDateTime localTime;

    @JsonIgnore
    private Long expires;

    /**
     * Creates a new token instance with a fresh local fallback subject identifier.
     */
    public DrivethruToken() {
        this(UUID.randomUUID().toString());
    }

    private DrivethruToken(final String subject) {
        this.subject = Objects.requireNonNull(subject, "subject");
    }

    /**
     * Creates an independent session-safe copy that preserves identity, bearer/refresh tokens,
     * customer ID, and expiry metadata while stripping {@link #applicationKey} (FR-010, FR-018).
     *
     * @return key-free copy isolated from subsequent mutations on the source token
     */
    public DrivethruToken sessionCopy() {
        DrivethruToken copy = new DrivethruToken(this.subject);
        copy.setAccessToken(this.accessToken);
        copy.setRefreshToken(this.refreshToken);
        copy.setRefreshTokenTTL(this.refreshTokenTTL);
        copy.setCustomerId(this.customerId);
        copy.setExpireTime(this.expireTime);
        copy.setServerTime(this.serverTime);
        copy.setLocalTime(this.localTime);
        copy.setExpires(this.expires);
        return copy;
    }

    /**
     * Returns the Bearer token string ready for use in an {@code Authorization} header.
     *
     * @return {@code "Bearer <accessToken>"}
     */
    @JsonIgnore
    public String getBearerToken() {
        return accessToken;
    }

    /**
     * Checks whether the refresh token has expired at the given wall-clock instant.
     *
     * @param instant current instant to compare against {@link #refreshTokenTTL} (Unix epoch seconds)
     * @return {@code true} when {@code refreshTokenTTL} is set and {@code instant} is at or past expiry
     */
    @JsonIgnore
    public boolean isRefreshTokenExpiredAt(final Instant instant) {
        return refreshTokenTTL != null && instant.getEpochSecond() >= refreshTokenTTL;
    }
}
