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

package de.paladinsinn.torg.codex.characterization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Live differential test (feature {@code 001-unify-censoring-authorization}, User Story 3,
 * FR-008 / SC-001, constitution Principle V test-adequacy rule).
 *
 * <p>Issues two live MockMvc requests to the Aysle cosm detail endpoint — one anonymous, one
 * carrying {@code ROLE_sourcebook-aysle} via a generic Spring Security principal (deliberately
 * <em>not</em> a {@code DriveThruUserDetails}, to prove principal-type independence) — and asserts
 * the rendered {@code worldLaws} field genuinely differs: the owner sees the
 * {@code <IF:sourcebook-aysle>} content, the anonymous caller sees the
 * {@code <IF:!sourcebook-aysle>} upsell content.
 *
 * <p>This assertion lives directly in test code (not merely a stored snapshot), so it fails loudly
 * if the single-mechanism ownership-resolution consolidation is ever reverted.
 */
@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest
class CensoringDifferentialTest {

    private static final String AYSLE_COSM_DETAIL_PATH =
            "/api/cosms/6cf031c3-ab0a-4d12-9173-91d74f7c809f";

    /** Text that only appears inside the {@code <IF:sourcebook-aysle>} (owner-only) block. */
    private static final String OWNER_ONLY_MARKER = "The laws of reality in Aysle form the foundation";

    /** Text that only appears inside the {@code <IF:!sourcebook-aysle>} (upsell) block. */
    private static final String UPSELL_ONLY_MARKER = "Outsiders";

    private static final RequestPostProcessor SOURCEBOOK_AYSLE_OWNER = user("fixture-owner").authorities(List.of(
            new SimpleGrantedAuthority("ROLE_core-rulebook"),
            new SimpleGrantedAuthority("ROLE_sourcebook-aysle")));

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void aysleWorldLawsDifferBetweenAnonymousAndOwner() throws Exception {
        String anonymousWorldLaws = worldLaws(get(AYSLE_COSM_DETAIL_PATH));
        String ownerWorldLaws = worldLaws(get(AYSLE_COSM_DETAIL_PATH).with(SOURCEBOOK_AYSLE_OWNER));

        assertThat(ownerWorldLaws)
                .as("owner must see the <IF:sourcebook-aysle> owner-only world-law content")
                .contains(OWNER_ONLY_MARKER)
                .doesNotContain(UPSELL_ONLY_MARKER);

        assertThat(anonymousWorldLaws)
                .as("anonymous must see the <IF:!sourcebook-aysle> upsell content")
                .contains(UPSELL_ONLY_MARKER)
                .doesNotContain(OWNER_ONLY_MARKER);

        assertThat(ownerWorldLaws)
                .as("owner and anonymous Aysle worldLaws must genuinely differ (FR-008/SC-001)")
                .isNotEqualTo(anonymousWorldLaws);
    }

    private String worldLaws(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        assertThat(result.getResponse().getStatus())
                .as("Aysle cosm detail must be retrievable")
                .isEqualTo(200);
        Map<String, Object> body = JsonParserFactory.getJsonParser()
                .parseMap(result.getResponse().getContentAsString());
        return String.valueOf(body.get("worldLaws"));
    }
}
