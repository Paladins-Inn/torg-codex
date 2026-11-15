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

import de.paladinsinn.drivethru.token.DrivethruToken;
import de.paladinsinn.security.ApiKeyAuthenticationToken;
import de.paladinsinn.security.DriveThruUserDetails;
import de.paladinsinn.torg.codex.api.observability.CatalogAccessObservabilityAdapter;
import de.paladinsinn.torg.codex.characterization.DrmSeedArea.Variant;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Live differential test of the domain-level product-ownership DRM (feature 005, ADR-019,
 * constitution Principle V test-adequacy rule) for all 14 product-bound catalog areas.
 *
 * <p>Seeds its own deterministic entries ({@code sql/catalog-drm-seed.sql}) independent of the
 * proprietary catalog data and compares anonymous callers with owners of {@code sourcebook-aysle}.
 */
@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest
@Sql(scripts = "/sql/catalog-drm-seed.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "/sql/catalog-drm-cleanup.sql", executionPhase = AFTER_TEST_CLASS)
class CatalogDrmDifferentialTest {

    static final RequestPostProcessor AYSLE_OWNER = user("drm-aysle-owner")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_sourcebook-aysle")));

    static final RequestPostProcessor DRIVETHRU_USER_ONLY = user("drm-drivethru-user")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_DRIVETHRU_USER")));

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MeterRegistry meterRegistry;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @ParameterizedTest
    @EnumSource(DrmSeedArea.class)
    void anonymousListContainsOnlyFreeTierEntries(DrmSeedArea area) throws Exception {
        List<String> ids = listIds(get(area.path()));

        assertThat(ids).contains(area.id(Variant.CORE).toString());
        assertThat(ids).doesNotContain(
                area.id(Variant.AYSLE).toString(), area.id(Variant.THIRD).toString(), area.id(Variant.UNASSIGNED).toString());
    }

    @ParameterizedTest
    @EnumSource(DrmSeedArea.class)
    void ownerListContainsFreeTierAndOwnedEntriesOnly(DrmSeedArea area) throws Exception {
        List<String> ids = listIds(get(area.path()).with(AYSLE_OWNER));

        assertThat(ids).contains(area.id(Variant.CORE).toString(), area.id(Variant.AYSLE).toString());
        assertThat(ids).doesNotContain(area.id(Variant.THIRD).toString(), area.id(Variant.UNASSIGNED).toString());
    }

    @ParameterizedTest
    @EnumSource(value = DrmSeedArea.class, names = {"ITEMS", "MIRACLE_LISTS", "PERKS", "POWER_LISTS", "SHARDS",
        "SPELL_LISTS", "THREATS", "VEHICLES"})
    void cosmFilterAppliesTheSameRules(DrmSeedArea area) throws Exception {
        assertThat(area.cosmFilter()).isTrue();

        assertThat(listIds(get(area.path()).queryParam("cosm", DrmSeedArea.COSM)))
                .containsExactly(area.id(Variant.CORE).toString());
        assertThat(listIds(get(area.path()).queryParam("cosm", DrmSeedArea.COSM).with(AYSLE_OWNER)))
                .containsExactlyInAnyOrder(area.id(Variant.CORE).toString(), area.id(Variant.AYSLE).toString());
    }

    @ParameterizedTest
    @EnumSource(DrmSeedArea.class)
    void freeTierDetailIsGrantedToAnonymousCallers(DrmSeedArea area) throws Exception {
        MockHttpServletResponse response = perform(get(area.detailPath(Variant.CORE)));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(json(response)).containsEntry("id", area.id(Variant.CORE).toString());
    }

    @ParameterizedTest
    @EnumSource(DrmSeedArea.class)
    void nonOwnedDetailIsAClosedPurchaseTeaser(DrmSeedArea area) throws Exception {
        MockHttpServletResponse response = perform(get(area.detailPath(Variant.AYSLE)));

        assertThat(response.getStatus()).isEqualTo(403);
        Map<String, Object> aysleProduct = new LinkedHashMap<>();
        aysleProduct.put("codexId", "sourcebook-aysle");
        aysleProduct.put("publication", Map.of("id", DrmSeedArea.AYSLE_PUBLICATION.toString(), "title", "Aysle Sourcebook"));
        Map<String, Object> thirdProduct = new LinkedHashMap<>();
        thirdProduct.put("codexId", "drm-third-product");
        thirdProduct.put("publication", null);
        assertThat(json(response)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "id", area.id(Variant.AYSLE).toString(),
                "name", area.name(Variant.AYSLE),
                "type", area.tag(),
                "requiredProducts", List.of(thirdProduct, aysleProduct)));
    }

    @ParameterizedTest
    @EnumSource(DrmSeedArea.class)
    void ownerGetsATeaserForUnownedThirdProducts(DrmSeedArea area) throws Exception {
        MockHttpServletResponse response = perform(get(area.detailPath(Variant.THIRD)).with(AYSLE_OWNER));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(json(response)).containsEntry("requiredProducts", List.of(nullPublication("drm-third-product")));
    }

    @ParameterizedTest
    @EnumSource(DrmSeedArea.class)
    void unassignedDetailIsIndistinguishableFromAMissingEntry(DrmSeedArea area) throws Exception {
        MockHttpServletResponse unassigned = perform(get(area.detailPath(Variant.UNASSIGNED)).with(AYSLE_OWNER));
        MockHttpServletResponse missing = perform(get(area.path() + "/" + UUID.randomUUID()).with(AYSLE_OWNER));

        assertThat(unassigned.getStatus()).isEqualTo(404).isEqualTo(missing.getStatus());
        assertThat(unassigned.getContentAsString()).isEqualTo(missing.getContentAsString()).isEmpty();
        assertThat(unassigned.getContentType()).isEqualTo(missing.getContentType());
        assertThat(unassigned.getHeaderNames()).containsExactlyInAnyOrderElementsOf(missing.getHeaderNames());
    }

    @ParameterizedTest
    @EnumSource(DrmSeedArea.class)
    void everyUnassignedEncounterIncrementsTheAreaCounter(DrmSeedArea area) throws Exception {
        double before = unassignedCount(area);
        perform(get(area.detailPath(Variant.UNASSIGNED)));
        double afterDetail = unassignedCount(area);
        perform(get(area.path()));
        double afterList = unassignedCount(area);

        assertThat(afterDetail - before).isEqualTo(1.0);
        assertThat(afterList - afterDetail).isGreaterThanOrEqualTo(1.0);
    }

    @ParameterizedTest
    @EnumSource(DrmSeedArea.class)
    void ownerWithoutExplicitCoreAuthorityReadsOwnedDetailsWithProtectedFields(DrmSeedArea area) throws Exception {
        MockHttpServletResponse response = perform(get(area.detailPath(Variant.AYSLE)).with(AYSLE_OWNER));

        assertThat(response.getStatus()).isEqualTo(200);
        Map<String, Object> body = json(response);
        assertThat(body).containsEntry("id", area.id(Variant.AYSLE).toString()).containsKey("publications");
        assertThat(String.valueOf(body.get("text"))).contains("DRM entry text");
    }

    @ParameterizedTest
    @EnumSource(DrmSeedArea.class)
    void driveThruUserRoleAloneSeesTheAnonymousEntries(DrmSeedArea area) throws Exception {
        assertThat(listIds(get(area.path()).with(DRIVETHRU_USER_ONLY)))
                .containsExactlyInAnyOrderElementsOf(listIds(get(area.path())));
        assertThat(perform(get(area.detailPath(Variant.AYSLE)).with(DRIVETHRU_USER_ONLY)).getStatus()).isEqualTo(403);
    }

    @ParameterizedTest
    @EnumSource(DrmSeedArea.class)
    void accessIsIndependentOfThePrincipalType(DrmSeedArea area) throws Exception {
        TestingAuthenticationToken testing = new TestingAuthenticationToken("drm-testing", null, "ROLE_sourcebook-aysle");
        DrivethruToken token = new DrivethruToken();
        token.setCustomerId("drm-api-key-customer");
        ApiKeyAuthenticationToken apiKey = ApiKeyAuthenticationToken.authenticated(
                new DriveThruUserDetails(token, List.of("sourcebook-aysle")));

        for (RequestPostProcessor caller : List.of(authentication(testing), authentication(apiKey))) {
            assertThat(listIds(get(area.path()).with(caller)))
                    .containsExactlyInAnyOrderElementsOf(listIds(get(area.path()).with(AYSLE_OWNER)));
            for (Variant variant : Variant.values()) {
                MockHttpServletResponse expected = perform(get(area.detailPath(variant)).with(AYSLE_OWNER));
                MockHttpServletResponse actual = perform(get(area.detailPath(variant)).with(caller));
                assertThat(actual.getStatus()).as("%s %s", area, variant).isEqualTo(expected.getStatus());
                assertThat(actual.getContentAsString()).as("%s %s", area, variant).isEqualTo(expected.getContentAsString());
            }
        }
    }

    @ParameterizedTest
    @EnumSource(DrmSeedArea.class)
    void accessibleDetailsRenderCoreGatedPassagesForOwnersWithoutExplicitCoreAuthority(DrmSeedArea area) throws Exception {
        String text = String.valueOf(json(perform(get(area.detailPath(Variant.AYSLE)).with(AYSLE_OWNER))).get("text"));

        assertThat(text).contains(DrmSeedArea.CORE_PASSAGE).doesNotContain(DrmSeedArea.UPSELL_PASSAGE);
    }

    @org.junit.jupiter.api.Test
    void publicArticleRendersCoreGatedPassagesWithTheEffectiveFreeTier() throws Exception {
        for (RequestPostProcessor caller : List.of(AYSLE_OWNER, DRIVETHRU_USER_ONLY)) {
            MockHttpServletResponse response = perform(get("/api/articles/" + DrmSeedArea.ARTICLE).with(caller));

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(String.valueOf(json(response).get("text")))
                    .contains(DrmSeedArea.CORE_PASSAGE)
                    .doesNotContain(DrmSeedArea.UPSELL_PASSAGE);
        }
        MockHttpServletResponse anonymous = perform(get("/api/articles/" + DrmSeedArea.ARTICLE));
        assertThat(anonymous.getStatus()).isEqualTo(200);
        assertThat(String.valueOf(json(anonymous).get("text"))).contains(DrmSeedArea.CORE_PASSAGE);
        assertThat(listIds(get("/api/articles"))).contains(DrmSeedArea.ARTICLE.toString());
    }

    @ParameterizedTest
    @EnumSource(value = DrmSeedArea.class, names = {"SPELL_LISTS", "MIRACLE_LISTS", "POWER_LISTS"})
    void embeddedReferencesContainOnlyAccessibleEntriesInOriginalOrder(DrmSeedArea area) throws Exception {
        DrmSeedArea child = childArea(area);
        String field = child.collection();

        assertThat(json(perform(get(area.detailPath(Variant.CORE)))).get(field))
                .isEqualTo(List.of(child.id(Variant.CORE).toString()));
        assertThat(json(perform(get(area.detailPath(Variant.CORE)).with(AYSLE_OWNER))).get(field))
                .isEqualTo(List.of(child.id(Variant.AYSLE).toString(), child.id(Variant.CORE).toString()));
        assertThat(json(perform(get(area.detailPath(Variant.AYSLE)).with(AYSLE_OWNER))).get(field))
                .isEqualTo(List.of(child.id(Variant.AYSLE).toString(), child.id(Variant.CORE).toString()));
    }

    @ParameterizedTest
    @EnumSource(value = DrmSeedArea.class, names = {"SPELL_LISTS", "MIRACLE_LISTS", "POWER_LISTS"})
    void unassignedReferencedEntriesAreCountedInTheReferencedArea(DrmSeedArea area) throws Exception {
        DrmSeedArea child = childArea(area);
        double before = unassignedCount(child);

        perform(get(area.detailPath(Variant.CORE)));

        assertThat(unassignedCount(child) - before).isEqualTo(1.0);
    }

    @org.junit.jupiter.api.Test
    void productLessCosmReferencesUseTheUnknownReferenceFallbackAndAreCounted() throws Exception {
        double before = unassignedCount(DrmSeedArea.COSMS);

        MockHttpServletResponse response = perform(get("/api/items/" + DrmSeedArea.ITEM_WITH_UNASSIGNED_COSM));

        assertThat(response.getStatus()).isEqualTo(200);
        Map<String, Object> cosm = new LinkedHashMap<>();
        cosm.put("id", null);
        cosm.put("name", "drm-cosm-unassigned");
        assertThat(json(response)).containsEntry("cosm", cosm);
        assertThat(unassignedCount(DrmSeedArea.COSMS) - before).isEqualTo(1.0);
    }

    @org.junit.jupiter.api.Test
    void assignedCosmReferencesResolveRegardlessOfOwnership() throws Exception {
        Map<String, Object> body = json(perform(get(DrmSeedArea.ITEMS.detailPath(Variant.CORE))));

        assertThat(body).containsEntry("cosm", Map.of(
                "id", DrmSeedArea.COSMS.id(Variant.CORE).toString(), "name", DrmSeedArea.COSMS.name(Variant.CORE)));
    }

    private static DrmSeedArea childArea(DrmSeedArea listArea) {
        return switch (listArea) {
            case SPELL_LISTS -> DrmSeedArea.SPELLS;
            case MIRACLE_LISTS -> DrmSeedArea.MIRACLES;
            case POWER_LISTS -> DrmSeedArea.POWERS;
            default -> throw new IllegalArgumentException(listArea.name());
        };
    }

    private double unassignedCount(DrmSeedArea area) {
        return meterRegistry.get(CatalogAccessObservabilityAdapter.COUNTER_NAME).tag("area", area.tag()).counter().count();
    }

    private static Map<String, Object> nullPublication(String codexId) {
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("codexId", codexId);
        product.put("publication", null);
        return product;
    }

    MockHttpServletResponse perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request).andReturn().getResponse();
    }

    List<String> listIds(MockHttpServletRequestBuilder request) throws Exception {
        MockHttpServletResponse response = perform(request);
        assertThat(response.getStatus()).isEqualTo(200);
        return JsonParserFactory.getJsonParser().parseList(response.getContentAsString()).stream()
                .map(entry -> String.valueOf(((Map<?, ?>) entry).get("id")))
                .toList();
    }

    static Map<String, Object> json(MockHttpServletResponse response) throws Exception {
        return JsonParserFactory.getJsonParser().parseMap(response.getContentAsString());
    }
}
