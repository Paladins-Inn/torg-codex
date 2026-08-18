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
