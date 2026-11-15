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

package de.paladinsinn.torg.codex.api.controller;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import de.paladinsinn.torg.codex.api.dto.ArticleDetailDto;
import de.paladinsinn.torg.codex.api.dto.ArticleSummaryDto;
import de.paladinsinn.torg.codex.api.dto.CosmDetailDto;
import de.paladinsinn.torg.codex.api.dto.CosmRefDto;
import de.paladinsinn.torg.codex.api.dto.CosmSummaryDto;
import de.paladinsinn.torg.codex.api.dto.DifficultyNumberDto;
import de.paladinsinn.torg.codex.api.dto.EntryTeaserDto;
import de.paladinsinn.torg.codex.api.dto.ItemDetailDto;
import de.paladinsinn.torg.codex.api.dto.ItemSummaryDto;
import de.paladinsinn.torg.codex.api.dto.MiracleDetailDto;
import de.paladinsinn.torg.codex.api.dto.MiracleListDetailDto;
import de.paladinsinn.torg.codex.api.dto.MiracleListSummaryDto;
import de.paladinsinn.torg.codex.api.dto.MiracleSummaryDto;
import de.paladinsinn.torg.codex.api.dto.PerkDetailDto;
import de.paladinsinn.torg.codex.api.dto.PerkGroupDetailDto;
import de.paladinsinn.torg.codex.api.dto.PerkGroupSummaryDto;
import de.paladinsinn.torg.codex.api.dto.PerkSummaryDto;
import de.paladinsinn.torg.codex.api.dto.PowerDetailDto;
import de.paladinsinn.torg.codex.api.dto.PowerListDetailDto;
import de.paladinsinn.torg.codex.api.dto.PowerListSummaryDto;
import de.paladinsinn.torg.codex.api.dto.PowerSummaryDto;
import de.paladinsinn.torg.codex.api.dto.PublicationDetailDto;
import de.paladinsinn.torg.codex.api.dto.PublicationRefDto;
import de.paladinsinn.torg.codex.api.dto.PublicationSummaryDto;
import de.paladinsinn.torg.codex.api.dto.RaceDetailDto;
import de.paladinsinn.torg.codex.api.dto.RaceSummaryDto;
import de.paladinsinn.torg.codex.api.dto.RequiredProductDto;
import de.paladinsinn.torg.codex.api.dto.ShardDetailDto;
import de.paladinsinn.torg.codex.api.dto.ShardSummaryDto;
import de.paladinsinn.torg.codex.api.dto.SpellDetailDto;
import de.paladinsinn.torg.codex.api.dto.SpellListDetailDto;
import de.paladinsinn.torg.codex.api.dto.SpellListSummaryDto;
import de.paladinsinn.torg.codex.api.dto.SpellSummaryDto;
import de.paladinsinn.torg.codex.api.dto.TagDetailDto;
import de.paladinsinn.torg.codex.api.dto.TagSummaryDto;
import de.paladinsinn.torg.codex.api.dto.ThreatDetailDto;
import de.paladinsinn.torg.codex.api.dto.ThreatSummaryDto;
import de.paladinsinn.torg.codex.api.dto.VehicleDetailDto;
import de.paladinsinn.torg.codex.api.dto.VehicleSummaryDto;
import de.paladinsinn.torg.codex.api.dto.VehicleWeaponDto;
import de.paladinsinn.torg.codex.characterization.DrmSeedArea;
import de.paladinsinn.torg.codex.characterization.DrmSeedArea.Variant;
import de.paladinsinn.torg.codex.domain.markup.EntityType;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Verifies the authoritative catalog OpenAPI contract (constitution Principle III, ADR-006,
 * feature 005): the specification is valid, covers all 17 catalog collections, describes the
 * actual DTOs with closed schemas, declares the {@code 403} purchase teaser for the 14
 * product-bound areas only, and matches the controllers' paths and cosm filters. Live anonymous
 * and owner responses of all 17 collections, including teasers and not-found responses, are
 * validated against the specification.
 */
@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest
@Sql(scripts = "/sql/catalog-drm-seed.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "/sql/catalog-drm-cleanup.sql", executionPhase = AFTER_TEST_CLASS)
class CatalogOpenApiContractTest {

    private static final RequestPostProcessor AYSLE_OWNER = user("contract-aysle-owner")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_sourcebook-aysle")));

    /** The authoritative specification, relative to the {@code torg-codex} module directory. */
    static final Path SPECIFICATION = Path.of("..", "specs", "005-domain-drm-enforcement", "contracts", "catalog-api.openapi.yaml")
            .toAbsolutePath().normalize();

    private static final Map<String, Class<? extends Record>> SCHEMA_DTOS = schemaDtos();

    private static final Map<String, Class<?>> COLLECTION_CONTROLLERS = collectionControllers();

    private static final Set<String> PUBLIC_COLLECTIONS = Set.of("articles", "publications", "tags");

    private static OpenAPI openApi;

    private static OpenApiInteractionValidator validator;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeAll
    static void parseSpecification() {
        assertThat(SPECIFICATION).exists();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(SPECIFICATION.toUri().toString(), null, options);

        assertThat(result.getMessages()).as("OpenAPI parser messages").isEmpty();
        openApi = result.getOpenAPI();
        assertThat(openApi).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(DrmSeedArea.class)
    void liveProductBoundResponsesMatchTheSpecification(DrmSeedArea area) throws Exception {
        assertConforms(get(area.path()), 200);
        assertConforms(get(area.path()).with(AYSLE_OWNER), 200);
        assertConforms(get(area.detailPath(Variant.CORE)), 200);
        assertConforms(get(area.detailPath(Variant.AYSLE)).with(AYSLE_OWNER), 200);
        assertConforms(get(area.detailPath(Variant.AYSLE)), 403);
        assertConforms(get(area.detailPath(Variant.THIRD)).with(AYSLE_OWNER), 403);
        assertConforms(get(area.detailPath(Variant.UNASSIGNED)), 404);
        assertConforms(get(area.path() + "/" + UUID.randomUUID()), 404);
        assertConforms(get(area.path() + "/not-a-uuid"), 400);
        if (area.cosmFilter()) {
            assertConforms(get(area.path()).queryParam("cosm", DrmSeedArea.COSM), 200);
        }
        if (area == DrmSeedArea.ITEMS) {
            assertConforms(get("/api/items/" + DrmSeedArea.ITEM_WITH_UNASSIGNED_COSM), 200);
        }
    }

    @Test
    void livePublicReferenceDataResponsesMatchTheSpecification() throws Exception {
        assertConforms(get("/api/articles"), 200);
        assertConforms(get("/api/articles/" + DrmSeedArea.ARTICLE), 200);
        assertConforms(get("/api/articles/" + DrmSeedArea.ARTICLE).with(AYSLE_OWNER), 200);
        assertConforms(get("/api/articles/" + UUID.randomUUID()), 404);
        for (String collection : List.of("publications", "tags")) {
            MockHttpServletResponse list = assertConforms(get("/api/" + collection), 200);
            List<Object> entries = JsonParserFactory.getJsonParser().parseList(list.getContentAsString());
            if (!entries.isEmpty()) {
                assertConforms(get("/api/" + collection + "/" + ((Map<?, ?>) entries.getFirst()).get("id")), 200);
            }
            assertConforms(get("/api/" + collection + "/" + UUID.randomUUID()), 404);
            assertConforms(get("/api/" + collection + "/not-a-uuid"), 400);
        }
    }

    private MockHttpServletResponse assertConforms(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        var mvcResult = mockMvc.perform(request).andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();
        String path = mvcResult.getRequest().getRequestURI();
        assertThat(response.getStatus()).as("status of %s", path).isEqualTo(expectedStatus);

        SimpleResponse.Builder builder = SimpleResponse.Builder.status(response.getStatus());
        if (response.getContentType() != null) {
            builder.withContentType(response.getContentType());
        }
        String body = response.getContentAsString();
        if (!body.isEmpty()) {
            builder.withBody(body);
        }
        ValidationReport report = validator().validateResponse(path, Request.Method.GET, builder.build());

        assertThat(report.getMessages()).as("OpenAPI violations of %s %s", path, body).isEmpty();
        return response;
    }

    private static OpenApiInteractionValidator validator() throws Exception {
        if (validator == null) {
            validator = OpenApiInteractionValidator.createForInlineApiSpecification(Files.readString(SPECIFICATION)).build();
        }
        return validator;
    }

    @Test
    void specificationIsAcceptedByTheResponseValidator() throws Exception {
        OpenApiInteractionValidator validator = OpenApiInteractionValidator
                .createForInlineApiSpecification(Files.readString(SPECIFICATION))
                .build();

        assertThat(validator).isNotNull();
    }

    @Test
    void everyCatalogCollectionHasListAndDetailOperations() {
        assertThat(COLLECTION_CONTROLLERS).hasSize(17);
        for (String collection : COLLECTION_CONTROLLERS.keySet()) {
            assertThat(operation("/api/" + collection)).as("list of %s", collection).isPresent();
            assertThat(operation("/api/" + collection + "/{id}")).as("detail of %s", collection).isPresent();
        }
        assertThat(openApi.getPaths()).hasSize(34);
    }

    @Test
    void specificationPathsMatchTheControllers() {
        for (Map.Entry<String, Class<?>> entry : COLLECTION_CONTROLLERS.entrySet()) {
            RequestMapping mapping = entry.getValue().getAnnotation(RequestMapping.class);

            assertThat(mapping.value()).as("controller path of %s", entry.getKey()).containsExactly("/api/" + entry.getKey());
        }
    }

    @Test
    void cosmFiltersMatchTheControllers() {
        for (Map.Entry<String, Class<?>> entry : COLLECTION_CONTROLLERS.entrySet()) {
            boolean controllerSupportsCosm = listMethod(entry.getValue()).getParameters().length == 1
                    && Arrays.stream(listMethod(entry.getValue()).getParameters()[0].getAnnotationsByType(RequestParam.class))
                    .findAny().isPresent();
            List<Parameter> parameters = Optional.ofNullable(operation("/api/" + entry.getKey()).orElseThrow().getParameters())
                    .orElse(List.of());
            boolean specSupportsCosm = parameters.stream()
                    .anyMatch(parameter -> "cosm".equals(parameter.getName()) && "query".equals(parameter.getIn())
                            && !Boolean.TRUE.equals(parameter.getRequired()));

            assertThat(specSupportsCosm).as("cosm filter of %s", entry.getKey()).isEqualTo(controllerSupportsCosm);
        }
    }

    @Test
    void productBoundDetailsDeclareTheTeaserAndAnEmptyNotFound() {
        for (String collection : COLLECTION_CONTROLLERS.keySet()) {
            Map<String, ApiResponse> responses = operation("/api/" + collection + "/{id}").orElseThrow().getResponses();
            boolean productBound = !PUBLIC_COLLECTIONS.contains(collection);

            assertThat(responses).as("responses of %s", collection).containsKeys("200", "404", "400");
            assertThat(responses.get("404").getContent()).as("404 of %s has no body", collection).isNullOrEmpty();
            assertThat(responses.get("400").getContent()).as("400 of %s has no body", collection).isNullOrEmpty();
            if (productBound) {
                assertThat(responses).containsKey("403");
                assertThat(responses.get("403").getContent().get("application/json").getSchema().get$ref())
                        .isEqualTo("#/components/schemas/EntryTeaser");
            } else {
                assertThat(responses).as("public %s has no teaser", collection).doesNotContainKey("403");
            }
        }
    }

    @Test
    void teaserSchemaIsClosedAndCarriesOnlyTeaserFields() {
        Schema<?> teaser = schema("EntryTeaser");

        assertThat(teaser.getProperties()).containsOnlyKeys("id", "name", "type", "requiredProducts");
        assertThat(teaser.getAdditionalProperties()).isEqualTo(false);
        assertThat(teaser.getRequired()).containsExactlyInAnyOrder("id", "name", "type", "requiredProducts");
        assertThat(teaser.getProperties().get("type").getEnum())
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(EntityType.values()).map(EntityType::tag).toList());
        assertThat(teaser.getProperties().get("requiredProducts").getMinItems()).isEqualTo(1);
    }

    @Test
    void schemasDescribeTheActualDtosWithClosedObjects() {
        for (Map.Entry<String, Class<? extends Record>> entry : SCHEMA_DTOS.entrySet()) {
            Schema<?> schema = schema(entry.getKey());
            Set<String> components = Arrays.stream(entry.getValue().getRecordComponents())
                    .map(RecordComponent::getName)
                    .collect(Collectors.toSet());

            assertThat(schema.getProperties()).as("properties of %s", entry.getKey()).containsOnlyKeys(components);
            assertThat(schema.getRequired()).as("required of %s", entry.getKey()).containsExactlyInAnyOrderElementsOf(components);
            assertThat(schema.getAdditionalProperties()).as("%s is closed", entry.getKey()).isEqualTo(false);
        }
        assertThat(openApi.getComponents().getSchemas().keySet())
                .containsExactlyInAnyOrderElementsOf(allSchemaNames());
    }

    private static Set<String> allSchemaNames() {
        Set<String> names = new java.util.HashSet<>(SCHEMA_DTOS.keySet());
        names.add("ClearanceLevel");
        names.add("NullablePublicationRef");
        return names;
    }

    private static Optional<Operation> operation(String path) {
        return Optional.ofNullable(openApi.getPaths().get(path)).map(item -> item.getGet());
    }

    private static Schema<?> schema(String name) {
        Schema<?> schema = openApi.getComponents().getSchemas().get(name);
        assertThat(schema).as("schema %s", name).isNotNull();
        return schema;
    }

    private static Method listMethod(Class<?> controller) {
        return Arrays.stream(controller.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(GetMapping.class))
                .filter(method -> method.getAnnotation(GetMapping.class).value().length == 0)
                .findFirst()
                .orElseThrow();
    }

    private static Map<String, Class<? extends Record>> schemaDtos() {
        Map<String, Class<? extends Record>> dtos = new LinkedHashMap<>();
        dtos.put("PublicationRef", PublicationRefDto.class);
        dtos.put("CosmRef", CosmRefDto.class);
        dtos.put("DifficultyNumber", DifficultyNumberDto.class);
        dtos.put("VehicleWeapon", VehicleWeaponDto.class);
        dtos.put("RequiredProduct", RequiredProductDto.class);
        dtos.put("EntryTeaser", EntryTeaserDto.class);
        dtos.put("ArticleSummary", ArticleSummaryDto.class);
        dtos.put("ArticleDetail", ArticleDetailDto.class);
        dtos.put("CosmSummary", CosmSummaryDto.class);
        dtos.put("CosmDetail", CosmDetailDto.class);
        dtos.put("ItemSummary", ItemSummaryDto.class);
        dtos.put("ItemDetail", ItemDetailDto.class);
        dtos.put("MiracleSummary", MiracleSummaryDto.class);
        dtos.put("MiracleDetail", MiracleDetailDto.class);
        dtos.put("MiracleListSummary", MiracleListSummaryDto.class);
        dtos.put("MiracleListDetail", MiracleListDetailDto.class);
        dtos.put("PerkSummary", PerkSummaryDto.class);
        dtos.put("PerkDetail", PerkDetailDto.class);
        dtos.put("PerkGroupSummary", PerkGroupSummaryDto.class);
        dtos.put("PerkGroupDetail", PerkGroupDetailDto.class);
        dtos.put("PowerSummary", PowerSummaryDto.class);
        dtos.put("PowerDetail", PowerDetailDto.class);
        dtos.put("PowerListSummary", PowerListSummaryDto.class);
        dtos.put("PowerListDetail", PowerListDetailDto.class);
        dtos.put("PublicationSummary", PublicationSummaryDto.class);
        dtos.put("PublicationDetail", PublicationDetailDto.class);
        dtos.put("RaceSummary", RaceSummaryDto.class);
        dtos.put("RaceDetail", RaceDetailDto.class);
        dtos.put("ShardSummary", ShardSummaryDto.class);
        dtos.put("ShardDetail", ShardDetailDto.class);
        dtos.put("SpellSummary", SpellSummaryDto.class);
        dtos.put("SpellDetail", SpellDetailDto.class);
        dtos.put("SpellListSummary", SpellListSummaryDto.class);
        dtos.put("SpellListDetail", SpellListDetailDto.class);
        dtos.put("TagSummary", TagSummaryDto.class);
        dtos.put("TagDetail", TagDetailDto.class);
        dtos.put("ThreatSummary", ThreatSummaryDto.class);
        dtos.put("ThreatDetail", ThreatDetailDto.class);
        dtos.put("VehicleSummary", VehicleSummaryDto.class);
        dtos.put("VehicleDetail", VehicleDetailDto.class);
        return dtos;
    }

    private static Map<String, Class<?>> collectionControllers() {
        Map<String, Class<?>> controllers = new LinkedHashMap<>();
        controllers.put("articles", ArticleController.class);
        controllers.put("cosms", CosmController.class);
        controllers.put("items", ItemController.class);
        controllers.put("miracles", MiracleController.class);
        controllers.put("miracle-lists", MiracleListController.class);
        controllers.put("perks", PerkController.class);
        controllers.put("perk-groups", PerkGroupController.class);
        controllers.put("powers", PowerController.class);
        controllers.put("power-lists", PowerListController.class);
        controllers.put("publications", PublicationController.class);
        controllers.put("races", RaceController.class);
        controllers.put("shards", ShardController.class);
        controllers.put("spells", SpellController.class);
        controllers.put("spell-lists", SpellListController.class);
        controllers.put("tags", TagController.class);
        controllers.put("threats", ThreatController.class);
        controllers.put("vehicles", VehicleController.class);
        return controllers;
    }
}
