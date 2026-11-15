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

package de.paladinsinn.torg.codex.api.observability;

import de.paladinsinn.torg.codex.application.port.in.CatalogBrowseQuery;
import de.paladinsinn.torg.codex.application.port.in.CatalogBrowseResult;
import de.paladinsinn.torg.codex.application.port.in.CatalogDetailResult;
import de.paladinsinn.torg.codex.application.port.in.CatalogFilterCriteria;
import de.paladinsinn.torg.codex.domain.access.EntryAccess;
import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import de.paladinsinn.torg.codex.domain.markup.EntityType;
import de.paladinsinn.torg.codex.web.codex.CodexAuthController;
import de.paladinsinn.torg.codex.web.codex.CodexCatalogController;
import de.paladinsinn.torg.codex.web.codex.mapper.CodexViewMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParseException;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ConcurrentModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies the production logging configuration (ADR-017, constitution Principle VII): the
 * application's own {@code application.yaml} selects Spring Boot's native ECS structured format,
 * and the DRM warning is emitted as ECS JSON that keeps the SLF4J key-value fields and contains
 * no caller data. The output is captured from the real console appender and parsed as JSON.
 */
@SpringBootTest(classes = StructuredLoggingTest.LoggingProbe.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ExtendWith(OutputCaptureExtension.class)
class StructuredLoggingTest {

    private static final String SENTINEL_PRINCIPAL = "sentinel-principal-4711";
    private static final String SENTINEL_API_KEY = "sentinel-api-key-0815";
    private static final String SENTINEL_PRODUCT = "sentinel-product-42";

    @Autowired
    private CatalogAccessObservabilityAdapter adapter;

    @Autowired
    private Environment environment;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void productionConfigurationSelectsNativeEcsFormat() {
        assertThat(environment.getProperty("logging.structured.format.console")).isEqualTo("ecs");

        boolean fileLoggingEnabled = environment.containsProperty("logging.file.name")
                || environment.containsProperty("logging.file.path");
        if (fileLoggingEnabled) {
            assertThat(environment.getProperty("logging.structured.format.file")).isEqualTo("ecs");
        }
    }

    @Test
    void drmWarningIsEmittedAsEcsJsonWithStructuredFieldsAndWithoutCallerData(CapturedOutput output) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                SENTINEL_PRINCIPAL, SENTINEL_API_KEY, List.of(new SimpleGrantedAuthority("ROLE_" + SENTINEL_PRODUCT))));
        UUID id = UUID.randomUUID();

        adapter.entryWithoutProducts(EntityType.GROUP, id, "Orphan Group");

        String line = output.getOut().lines()
                .filter(candidate -> candidate.contains(id.toString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no emitted log line for " + id + " in:\n" + output.getOut()));
        Map<String, Object> record = parse(line);

        assertThat(record).containsKeys("@timestamp", "ecs", "message");
        assertThat(value(record, "log.level")).contains("WARN");
        assertThat(value(record, "log.logger")).contains(CatalogAccessObservabilityAdapter.class.getName());
        assertThat(value(record, "message")).contains(
                "Catalog entry without product assignment hidden by DRM: area=group, id=" + id + ", name=Orphan Group");
        assertThat(value(record, "catalogArea")).contains("group");
        assertThat(value(record, "entryId")).contains(id.toString());
        assertThat(value(record, "entryName")).contains("Orphan Group");
        for (String sentinel : List.of(SENTINEL_PRINCIPAL, SENTINEL_API_KEY, SENTINEL_PRODUCT)) {
            assertThat(line).doesNotContain(sentinel);
        }
    }

    @Test
    void domainOutcomesInUiControllersEmitInfoEcsJsonWithoutCredentialsOrOwnership(final CapturedOutput output) {
        AuthenticationManager rejectingAuth = authentication -> {
            throw new BadCredentialsException("invalid");
        };
        CodexViewMapper viewMapper = new CodexViewMapper(mock(de.paladinsinn.torg.codex.api.security.CurrentUserCensorFactory.class));
        CodexAuthController authController = new CodexAuthController(rejectingAuth, viewMapper);

        UsernamePasswordAuthenticationToken callerAuth = UsernamePasswordAuthenticationToken.authenticated(
                SENTINEL_PRINCIPAL, SENTINEL_API_KEY, List.of(new SimpleGrantedAuthority("ROLE_" + SENTINEL_PRODUCT)));
        SecurityContextHolder.getContext().setAuthentication(callerAuth);

        authController.performLogin(
                SENTINEL_API_KEY,
                callerAuth,
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                new ConcurrentModel());

        CatalogBrowseQuery stubBrowse = new CatalogBrowseQuery() {
            @Override
            public CatalogBrowseResult browse(final CatalogArea area, final CatalogFilterCriteria criteria) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CatalogDetailResult findDetail(final CatalogArea area, final String slugOrId) {
                if ("locked-spell".equals(slugOrId)) {
                    var teaser = new EntryAccess.Teaser<>(
                            UUID.randomUUID(), "Locked Spell", EntityType.SPELL, new TreeSet<>(Set.of("sourcebook-aysle")));
                    return new CatalogDetailResult.RestrictedTeaser(area, teaser, List.of());
                }
                return new CatalogDetailResult.Absent(area);
            }
        };
        CodexCatalogController catalogController = new CodexCatalogController(stubBrowse, viewMapper);
        catalogController.viewEntryDetail("spells", "locked-spell", callerAuth, new MockHttpServletResponse(), new ConcurrentModel());
        catalogController.viewEntryDetail("spells", "missing-spell", callerAuth, new MockHttpServletResponse(), new ConcurrentModel());

        List<Map<String, Object>> loginLines = output.getOut().lines()
                .filter(line -> line.contains("user-login-rejected"))
                .map(StructuredLoggingTest::parse)
                .toList();
        assertThat(loginLines).isNotEmpty();
        assertThat(value(loginLines.getLast(), "log.level")).contains("INFO");
        assertThat(value(loginLines.getLast(), "reason")).contains("invalid-credentials");

        List<Map<String, Object>> teaserLines = output.getOut().lines()
                .filter(line -> line.contains("Restricted codex entry teaser served"))
                .map(StructuredLoggingTest::parse)
                .toList();
        assertThat(teaserLines).isNotEmpty();
        assertThat(value(teaserLines.getLast(), "log.level")).contains("INFO");
        assertThat(value(teaserLines.getLast(), "outcome")).contains("teaser");

        List<Map<String, Object>> notFoundLines = output.getOut().lines()
                .filter(line -> line.contains("Catalog entry not found"))
                .map(StructuredLoggingTest::parse)
                .toList();
        assertThat(notFoundLines).isNotEmpty();
        assertThat(value(notFoundLines.getLast(), "log.level")).contains("INFO");
        assertThat(value(notFoundLines.getLast(), "outcome")).contains("not-found");

        for (String sentinel : List.of(SENTINEL_PRINCIPAL, SENTINEL_API_KEY, SENTINEL_PRODUCT)) {
            assertThat(output.getOut()).doesNotContain(sentinel);
        }
    }

    private static Map<String, Object> parse(String line) {
        try {
            return JsonParserFactory.getJsonParser().parseMap(line);
        } catch (JsonParseException exception) {
            throw new AssertionError("emitted log line is not JSON: " + line, exception);
        }
    }

    /**
     * Reads a value either as a flat dotted ECS key or as a nested object path.
     */
    @SuppressWarnings("unchecked")
    private static Optional<String> value(Map<String, Object> record, String key) {
        if (record.containsKey(key)) {
            return Optional.of(String.valueOf(record.get(key)));
        }
        Object current = record;
        for (String part : key.split("\\.")) {
            if (!(current instanceof Map<?, ?> map) || !map.containsKey(part)) {
                return Optional.empty();
            }
            current = ((Map<String, Object>) map).get(part);
        }
        return Optional.of(String.valueOf(current));
    }

    /** Minimal context: the real configuration files, the observability adapter and a meter registry. */
    @Configuration(proxyBeanMethods = false)
    @Import(CatalogAccessObservabilityAdapter.class)
    static class LoggingProbe {

        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
