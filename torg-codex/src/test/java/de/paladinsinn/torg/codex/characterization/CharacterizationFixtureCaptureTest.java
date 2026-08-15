package de.paladinsinn.torg.codex.characterization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest
@SuppressWarnings({"rawtypes", "unchecked"})
class CharacterizationFixtureCaptureTest {
    @Autowired
    private ApplicationContext applicationContext;

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
    void captureAllCatalogFixturesWhenEnabled() throws Exception {
        assumeTrue(Boolean.getBoolean("characterization.capture"));

        for (CatalogArea area : CatalogArea.values()) {
            captureFixturesFor(area);
        }
    }

    private void captureFixturesFor(CatalogArea area) throws Exception {
        JpaRepository repository = applicationContext.getBean(area.repositoryType());
        List<?> entities = repository.findAll();
        assertThat(entities).as("sample entities for %s", area).isNotEmpty();

        UUID sampleId = CharacterizationFixtureSupport.sampleId(entities.getFirst());
        UUID drmSensitiveId = findDrmSensitiveId(area, entities).orElse(sampleId);

        write(area, perform("anonymous-list", area.collectionPath(), Map.of(), CharacterizationAuthVariant.ANONYMOUS));
        write(area, perform("owner-list", area.collectionPath(), Map.of(), CharacterizationAuthVariant.SOURCEBOOK_AYSLE_OWNER));
        write(area, perform("anonymous-detail", area.collectionPath() + "/" + drmSensitiveId, Map.of(), CharacterizationAuthVariant.ANONYMOUS));
        write(area, perform("owner-detail", area.collectionPath() + "/" + drmSensitiveId, Map.of(), CharacterizationAuthVariant.SOURCEBOOK_AYSLE_OWNER));
        write(area, perform("anonymous-not-found", area.collectionPath() + "/" + CharacterizationFixtureSupport.MISSING_ID, Map.of(), CharacterizationAuthVariant.ANONYMOUS));
        write(area, perform("anonymous-invalid-uuid", area.collectionPath() + "/not-a-uuid", Map.of(), CharacterizationAuthVariant.ANONYMOUS));

        if (area.supportsCosm()) {
            String cosm = CharacterizationFixtureSupport.sampleCosm(entities.stream()
                    .filter(entity -> CharacterizationFixtureSupport.sampleCosm(entity) != null)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No cosm-backed entity found for " + area)));
            write(area, perform("anonymous-cosm-filter", area.collectionPath(), CharacterizationFixtureSupport.queryParameters("cosm", cosm), CharacterizationAuthVariant.ANONYMOUS));
            write(area, perform("anonymous-cosm-filter-miss", area.collectionPath(), CharacterizationFixtureSupport.queryParameters("cosm", "definitely-not-a-real-cosm"), CharacterizationAuthVariant.ANONYMOUS));
        }
    }

    private Optional<UUID> findDrmSensitiveId(CatalogArea area, List<?> entities) throws Exception {
        for (Object entity : entities.stream().limit(25).toList()) {
            UUID id = CharacterizationFixtureSupport.sampleId(entity);
            CharacterizationFixture.Response anonymous = perform("probe-anonymous", area.collectionPath() + "/" + id, Map.of(), CharacterizationAuthVariant.ANONYMOUS).response();
            CharacterizationFixture.Response owner = perform("probe-owner", area.collectionPath() + "/" + id, Map.of(), CharacterizationAuthVariant.SOURCEBOOK_AYSLE_OWNER).response();
            if (CharacterizationFixtureSupport.responsesDiffer(anonymous, owner)) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }

    private CharacterizationFixture perform(
            String name,
            String path,
            Map<String, List<String>> queryParameters,
            CharacterizationAuthVariant authVariant) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(path);
        queryParameters.forEach((parameter, values) -> requestBuilder.queryParam(parameter, values.toArray(String[]::new)));
        if (authVariant.requestPostProcessor() != null) {
            requestBuilder.with(authVariant.requestPostProcessor());
        }

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        return new CharacterizationFixture(
                name,
                new CharacterizationFixture.Request("GET", path, queryParameters, authVariant),
                new CharacterizationFixture.Response(
                        mvcResult.getResponse().getStatus(),
                        CharacterizationFixtureSupport.normalizeHeaders(mvcResult.getResponse()),
                        CharacterizationFixtureSupport.normalizeBody(mvcResult.getResponse())));
    }

    private void write(CatalogArea area, CharacterizationFixture fixture) throws Exception {
        CharacterizationFixtureSupport.writeFixture(area, fixture);
    }
}
