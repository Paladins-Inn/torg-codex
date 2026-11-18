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

package de.paladinsinn.torg.codex.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PublicationThumbnailTest {

    private static final int CORE_RULEBOOK_ID = 216248;
    private static final int ARBITRARY_UNREGISTERED_ID = 99999991;
    private static final int ALTERNATE_ID = 333333;

    @Test
    void derivesPrimaryJpgAndFallbackPngThumbnailsForCoreRulebook() {
        Publication pub = publication(CORE_RULEBOOK_ID, Set.of(CORE_RULEBOOK_ID, ALTERNATE_ID));

        String jpg = pub.thumbnailUrl();
        String png = pub.thumbnailFallbackUrl();

        assertThat(jpg).isEqualTo("https://www.drivethrurpg.com/images/3444/216248-thumb140.jpg");
        assertThat(png).isEqualTo("https://www.drivethrurpg.com/images/3444/216248-thumb140.png");
        assertThat(png).isEqualTo(jpg.substring(0, jpg.length() - 3) + "png");
    }

    @Test
    void derivesThumbnailsForArbitraryPositiveIdUsingPrimaryIdOnly() {
        Publication pub = publication(ARBITRARY_UNREGISTERED_ID, Set.of(ALTERNATE_ID, ARBITRARY_UNREGISTERED_ID));

        assertThat(pub.thumbnailUrl())
                .isEqualTo("https://www.drivethrurpg.com/images/3444/99999991-thumb140.jpg");
        assertThat(pub.thumbnailFallbackUrl())
                .isEqualTo("https://www.drivethrurpg.com/images/3444/99999991-thumb140.png");
        assertThat(pub.coverURL())
                .isEqualTo("https://www.drivethrurpg.com/images/3444/99999991.jpg");
        assertThat(pub.productUrl())
                .isEqualTo("https://www.drivethrurpg.com/product/99999991");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -42})
    void returnsNullThumbnailsForNonpositivePrimaryId(final int primaryId) {
        Publication pub = publication(primaryId, Set.of(ALTERNATE_ID));

        assertThat(pub.thumbnailUrl()).isNull();
        assertThat(pub.thumbnailFallbackUrl()).isNull();
        assertThat(pub.productUrl()).isNull();
        assertThat(pub.coverURL()).isEqualTo("https://www.drivethrurpg.com/images/3444/" + primaryId + ".jpg");
    }

    private static Publication publication(final int primaryProductId, final Set<Integer> productIds) {
        return new Publication(
                UUID.fromString("7cbda2b8-1812-51b6-840a-3b039bcb99c8"),
                "test-publication",
                "Test Publication",
                primaryProductId,
                null,
                productIds
        );
    }
}
