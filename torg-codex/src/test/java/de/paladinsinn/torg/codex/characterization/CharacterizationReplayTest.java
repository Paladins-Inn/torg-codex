package de.paladinsinn.torg.codex.characterization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest
class CharacterizationReplayTest {
    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @TestFactory
    List<DynamicTest> replaysEveryCapturedFixture() throws Exception {
        return CharacterizationFixtureSupport.fixtureFiles().stream()
                .map(path -> DynamicTest.dynamicTest(path.toString(), () -> assertFixture(path)))
                .toList();
    }

    private void assertFixture(java.nio.file.Path path) throws Exception {
        CharacterizationFixture fixture = CharacterizationFixtureSupport.readFixture(path);
        MockHttpServletRequestBuilder requestBuilder = get(fixture.request().path());
        for (Map.Entry<String, List<String>> entry : fixture.request().queryParameters().entrySet()) {
            requestBuilder.queryParam(entry.getKey(), entry.getValue().toArray(String[]::new));
        }
        if (fixture.request().authVariant().requestPostProcessor() != null) {
            requestBuilder.with(fixture.request().authVariant().requestPostProcessor());
        }

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();

        assertThat(mvcResult.getResponse().getStatus())
                .as("status for %s", path)
                .isEqualTo(fixture.response().status());
        assertThat(CharacterizationFixtureSupport.normalizeHeaders(mvcResult.getResponse()))
                .as("headers for %s", path)
                .isEqualTo(fixture.response().headers());
        assertThat(CharacterizationFixtureSupport.normalizeBody(mvcResult.getResponse()))
                .as("body for %s", path)
                .isEqualTo(fixture.response().body());
    }
}
