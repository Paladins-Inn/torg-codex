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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import de.paladinsinn.torg.codex.application.port.out.CatalogAccessObserver;
import de.paladinsinn.torg.codex.domain.markup.EntityType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogAccessObservabilityAdapterTest {

    private static final String COUNTER = "torg.codex.catalog.entries.without.products";
    private static final String SENTINEL_PRINCIPAL = "sentinel-principal-4711";
    private static final String SENTINEL_API_KEY = "sentinel-api-key-0815";
    private static final String SENTINEL_PRODUCT = "sentinel-product-42";

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final CatalogAccessObservabilityAdapter adapter = new CatalogAccessObservabilityAdapter(registry);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(CatalogAccessObservabilityAdapter.class);

    @BeforeEach
    void captureLogs() {
        appender.start();
        logger.addAppender(appender);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                SENTINEL_PRINCIPAL, SENTINEL_API_KEY, List.of(new SimpleGrantedAuthority("ROLE_" + SENTINEL_PRODUCT))));
    }

    @AfterEach
    void releaseLogs() {
        logger.detachAppender(appender);
        SecurityContextHolder.clearContext();
    }

    @Test
    void isTheCatalogAccessObserver() {
        assertThat(adapter).isInstanceOf(CatalogAccessObserver.class);
    }

    @Test
    void oneEncounterLogsOneWarningWithStructuredFields() {
        UUID id = UUID.randomUUID();

        adapter.entryWithoutProducts(EntityType.SPELL_LIST, id, "Orphan List");

        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage())
                .isEqualTo("Catalog entry without product assignment hidden by DRM: area=spell-list, id=" + id + ", name=Orphan List");
        assertThat(keyValues(event)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "catalogArea", "spell-list",
                "entryId", id.toString(),
                "entryName", "Orphan List"));
    }

    @Test
    void oneEncounterIncrementsTheAreaCounterByOne() {
        adapter.entryWithoutProducts(EntityType.COSM, UUID.randomUUID(), "Aysle");

        assertThat(counter("cosm").count()).isEqualTo(1.0);
        assertThat(counter("spell").count()).isZero();
    }

    @Test
    void everyEncounterIsCountedAndLoggedWithoutDeduplication() {
        UUID id = UUID.randomUUID();

        adapter.entryWithoutProducts(EntityType.COSM, id, "Aysle");
        adapter.entryWithoutProducts(EntityType.COSM, id, "Aysle");

        assertThat(counter("cosm").count()).isEqualTo(2.0);
        assertThat(appender.list).hasSize(2);
    }

    @Test
    void countersAreTaggedOnlyByTheBoundedArea() {
        adapter.entryWithoutProducts(EntityType.PERK, UUID.randomUUID(), "First");
        adapter.entryWithoutProducts(EntityType.PERK, UUID.randomUUID(), "Second");

        List<Meter> meters = registry.find(COUNTER).meters().stream().toList();
        assertThat(meters).hasSize(EntityType.values().length);
        assertThat(meters).allSatisfy(meter -> assertThat(meter.getId().getTags())
                .extracting(tag -> tag.getKey())
                .containsExactly("area"));
    }

    @Test
    void neitherLogNorMetricContainsCallerData() {
        adapter.entryWithoutProducts(EntityType.THREAT, UUID.randomUUID(), "Orphan Threat");

        ILoggingEvent event = appender.list.getFirst();
        String logged = event.getFormattedMessage() + keyValues(event) + event.getMDCPropertyMap();
        String metrics = registry.getMeters().stream().map(meter -> meter.getId().toString()).collect(Collectors.joining());
        for (String sentinel : List.of(SENTINEL_PRINCIPAL, SENTINEL_API_KEY, SENTINEL_PRODUCT)) {
            assertThat(logged).doesNotContain(sentinel);
            assertThat(metrics).doesNotContain(sentinel);
        }
    }

    private Counter counter(String area) {
        return registry.get(COUNTER).tag("area", area).counter();
    }

    private static Map<String, String> keyValues(ILoggingEvent event) {
        List<KeyValuePair> pairs = event.getKeyValuePairs() == null ? List.of() : event.getKeyValuePairs();
        return pairs.stream().collect(Collectors.toMap(pair -> pair.key, pair -> String.valueOf(pair.value)));
    }
}
