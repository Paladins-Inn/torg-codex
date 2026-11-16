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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
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

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DrivethruToken#sessionCopy()} secret stripping and state independence.
 */
class DrivethruTokenTest {

    private static final long REFRESH_TTL = 4102444800L;
    private static final long EXPIRES_SECONDS = 3600L;
    private static final int YEAR = 2026;
    private static final int MONTH = 11;
    private static final int DAY = 16;
    private static final int HOUR = 12;

    @Test
    void sessionCopyStripsApplicationKeyPreservesIdentityAndIsolatesMutableFields() {
        DrivethruToken original = new DrivethruToken();
        original.setAccessToken("access-jwt-1");
        original.setRefreshToken("refresh-cookie-1");
        original.setRefreshTokenTTL(REFRESH_TTL);
        original.setApplicationKey("raw-cleartext-application-key");
        original.setCustomerId("424242");
        LocalDateTime timestamp = LocalDateTime.of(YEAR, MONTH, DAY, HOUR, 0);
        original.setExpireTime(timestamp);
        original.setServerTime(timestamp);
        original.setLocalTime(timestamp);
        original.setExpires(EXPIRES_SECONDS);

        DrivethruToken copy = original.sessionCopy();

        assertThat(copy).isNotSameAs(original);
        assertThat(copy.getApplicationKey()).isNull();
        assertThat(original.getApplicationKey()).isEqualTo("raw-cleartext-application-key");
        assertThat(copy.getSubject()).isEqualTo(original.getSubject());
        assertThat(copy.getAccessToken()).isEqualTo("access-jwt-1");
        assertThat(copy.getBearerToken()).isEqualTo("access-jwt-1");
        assertThat(copy.getRefreshToken()).isEqualTo("refresh-cookie-1");
        assertThat(copy.getRefreshTokenTTL()).isEqualTo(REFRESH_TTL);
        assertThat(copy.getCustomerId()).isEqualTo("424242");
        assertThat(copy.getExpireTime()).isEqualTo(timestamp);
        assertThat(copy.getServerTime()).isEqualTo(timestamp);
        assertThat(copy.getLocalTime()).isEqualTo(timestamp);
        assertThat(copy.getExpires()).isEqualTo(EXPIRES_SECONDS);
        assertThat(copy.toString()).doesNotContain("raw-cleartext-application-key", "access-jwt-1", "refresh-cookie-1");

        copy.setAccessToken("renewed-jwt-2");
        copy.setRefreshToken("renewed-refresh-2");
        copy.setCustomerId("999999");

        assertThat(original.getAccessToken()).isEqualTo("access-jwt-1");
        assertThat(original.getRefreshToken()).isEqualTo("refresh-cookie-1");
        assertThat(original.getCustomerId()).isEqualTo("424242");
    }
}
