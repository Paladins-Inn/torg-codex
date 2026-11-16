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

package de.paladinsinn.torg.codex.web.codex;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.paladinsinn.drivethru.client.DriveThruProperties;
import de.paladinsinn.security.DriveThruSessionSyncState;
import de.paladinsinn.security.SyncStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying progressive multi-batch unlocking and real rendered output
 * differences across home, category browse, htmx filter, embedded list, gated prose, and
 * detail/teaser views using deterministic latches (US1, FR-002, FR-003, FR-004, SC-002,
 * Constitution Principle V).
 */
@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest
@WireMockTest
@Sql(scripts = "/sql/catalog-drm-seed.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "/sql/catalog-drm-cleanup.sql", executionPhase = AFTER_TEST_CLASS)
class AsyncLibraryAccessTest {

    private static final String INCREMENTAL_KEY = "7766554433221100776655443322110077665544";
    private static final String INCREMENTAL_TOKEN = "incremental-aysle-then-living-land-jwt";
    private static final int HTTP_OK = 200;
    private static final int LATCH_TIMEOUT_SECONDS = 15;
    private static final long BATCH_UNLOCK_DEADLINE_NANOS = Duration.ofSeconds(1).toNanos();
    private static final long WAIT_TIMEOUT_NANOS = Duration.ofSeconds(10).toNanos();
    private static final long PARK_NANOS = Duration.ofMillis(5).toNanos();

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DriveThruProperties driveThruProperties;

    private String originalBaseUrl;
    private HttpServer latchServer;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws IOException {
        originalBaseUrl = driveThruProperties.getBaseUrl();
        latchServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        latchServer.setExecutor(Executors.newCachedThreadPool());
        latchServer.start();
        driveThruProperties.setBaseUrl(
                "http://127.0.0.1:" + latchServer.getAddress().getPort() + "/api/vBeta");
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        driveThruProperties.setBaseUrl(originalBaseUrl);
        if (latchServer != null) {
            latchServer.stop(0);
        }
    }

    @Test
    void unlocksEachBatchWithinOneSecondAcrossHomeCategoryFilterEmbeddedAndDetailViews() throws Exception {
        CountDownLatch pageOneRequested = new CountDownLatch(1);
        CountDownLatch releasePageOne = new CountDownLatch(1);
        CountDownLatch pageTwoRequested = new CountDownLatch(1);
        CountDownLatch releasePageTwo = new CountDownLatch(1);

        latchServer.createContext("/api/vBeta/auth_key", exchange -> writeJson(
                exchange,
                "application/json",
                "{\"token\":\"" + INCREMENTAL_TOKEN + "\","
                        + "\"refreshToken\":\"inc-refresh\",\"refreshTokenTTL\":4102444800}"));
        latchServer.createContext("/api/vBeta/order_products", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            try {
                if (query != null && query.contains("page=1")) {
                    pageOneRequested.countDown();
                    releasePageOne.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    writeJson(exchange, "application/vnd.api+json",
                            "{\"links\":{\"next\":\"/api/vBeta/order_products?page=2&pageSize=200\"},"
                                    + "\"meta\":{\"itemsPerPage\":200,\"currentPage\":1},"
                                    + "\"data\":[{\"id\":\"1\",\"type\":\"OrderProduct\","
                                    + "\"attributes\":{\"productId\":284210,\"customerId\":424242,\"files\":[]}}]}");
                } else {
                    pageTwoRequested.countDown();
                    releasePageTwo.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    writeJson(exchange, "application/vnd.api+json",
                            "{\"links\":{\"next\":null},"
                                    + "\"meta\":{\"itemsPerPage\":200,\"currentPage\":2},"
                                    + "\"data\":[{\"id\":\"2\",\"type\":\"OrderProduct\","
                                    + "\"attributes\":{\"productId\":247246,\"customerId\":424242,\"files\":[]}}]}");
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                exchange.close();
            }
        });

        try {
            MvcResult loginResult = mockMvc.perform(post("/login").param("apiKey", INCREMENTAL_KEY).with(csrf()))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrl("/"))
                    .andReturn();
            MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
            assertThat(session).isNotNull();
            assertThat(pageOneRequested.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

            // Stage 0: Deterministically before Batch 1 is released, session has free-tier only
            String initialHome = mockMvc.perform(get("/").session(session))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String initialCategory = mockMvc.perform(get("/spells").session(session))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String initialEmbedded = mockMvc.perform(get("/spell-lists/drm-spell-list-core").session(session))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String initialTeaser = mockMvc.perform(get("/spells/drm-spell-aysle").session(session))
                    .andExpect(status().isForbidden())
                    .andReturn().getResponse().getContentAsString();

            assertThat(initialHome)
                    .contains("<strong>1</strong> unlocked publication(s) active.")
                    .contains("Aysle Sourcebook", "Living Land Sourcebook");
            assertThat(currentSyncState(session).appliedBatches()).isZero();
            assertThat(initialCategory).contains("DRM-spell-Core").doesNotContain("DRM-spell-Aysle");
            assertThat(initialEmbedded)
                    .contains("DRM-CORE-PASSAGE", "DRM-AYSLE-TEASER", "DRM-spell-Core")
                    .doesNotContain("DRM-AYSLE-OWNER-ONLY", "DRM-spell-Aysle");
            assertThat(initialTeaser).contains("DRM-spell-Aysle").doesNotContain("DRM-AYSLE-OWNER-ONLY");

            // Stage 1: Release Batch 1 (Aysle) while Batch 2 (Living Land) remains held on its latch
            long batchOneReleasedAt = System.nanoTime();
            releasePageOne.countDown();
            awaitAppliedBatches(session, 1);
            assertThat(pageTwoRequested.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(currentSyncState(session).status()).isEqualTo(SyncStatus.RUNNING);

            String afterBatchOneDetail = mockMvc.perform(get("/spells/drm-spell-aysle").session(session))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            long unlockElapsed = System.nanoTime() - batchOneReleasedAt;
            assertThat(unlockElapsed)
                    .as("SC-002: Completed batch publications must be accessible within 1s")
                    .isLessThan(BATCH_UNLOCK_DEADLINE_NANOS);

            String afterBatchOneCategory = mockMvc.perform(get("/spells").session(session))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String afterBatchOneFilter = mockMvc.perform(get("/spells")
                            .session(session)
                            .param("q", "DRM-spell")
                            .header("HX-Request", "true")
                            .header("HX-Target", "catalog-results"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String afterBatchOneEmbedded = mockMvc.perform(get("/spell-lists/drm-spell-list-core").session(session))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(afterBatchOneCategory)
                    .isNotEqualTo(initialCategory)
                    .contains("DRM-spell-Core", "DRM-spell-Aysle")
                    .doesNotContain("DRM-spell-Third");
            assertThat(afterBatchOneFilter)
                    .contains("DRM-spell-Core", "DRM-spell-Aysle")
                    .doesNotContain("DRM-spell-Third");
            assertThat(afterBatchOneEmbedded)
                    .isNotEqualTo(initialEmbedded)
                    .contains("DRM-CORE-PASSAGE", "DRM-AYSLE-OWNER-ONLY", "DRM-spell-Core", "DRM-spell-Aysle")
                    .doesNotContain("DRM-AYSLE-TEASER", "DRM-spell-Third");
            assertThat(afterBatchOneDetail)
                    .isNotEqualTo(initialTeaser)
                    .contains("DRM-spell-Aysle", "DRM-AYSLE-OWNER-ONLY");
            mockMvc.perform(get("/spells/drm-spell-third").session(session))
                    .andExpect(status().isForbidden());

            // Stage 2: Release Batch 2 (Living Land) -> union of Core + Aysle + Living Land (3 unlocked books)
            releasePageTwo.countDown();
            awaitCompleted(session);
            String finalHome = mockMvc.perform(get("/").session(session))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(finalHome)
                    .isNotEqualTo(initialHome)
                    .contains("<strong>3</strong> unlocked publication(s) active.")
                    .doesNotContain("Aysle Sourcebook", "Living Land Sourcebook");
            mockMvc.perform(get("/spells/drm-spell-aysle").session(session))
                    .andExpect(status().isOk());
        } finally {
            releasePageOne.countDown();
            releasePageTwo.countDown();
        }
    }

    private static void writeJson(
            final HttpExchange exchange,
            final String contentType,
            final String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(HTTP_OK, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static long awaitAppliedBatches(final MockHttpSession session, final int minBatches) {
        long deadline = System.nanoTime() + WAIT_TIMEOUT_NANOS;
        while (System.nanoTime() < deadline) {
            DriveThruSessionSyncState state = currentSyncState(session);
            if (state != null && state.appliedBatches() >= minBatches) {
                return System.nanoTime();
            }
            LockSupport.parkNanos(PARK_NANOS);
        }
        throw new AssertionError("Timed out waiting for appliedBatches >= " + minBatches);
    }

    private static void awaitCompleted(final MockHttpSession session) {
        long deadline = System.nanoTime() + WAIT_TIMEOUT_NANOS;
        while (System.nanoTime() < deadline) {
            DriveThruSessionSyncState state = currentSyncState(session);
            if (state != null && state.status() == SyncStatus.COMPLETED) {
                return;
            }
            LockSupport.parkNanos(PARK_NANOS);
        }
        throw new AssertionError("Timed out waiting for SyncStatus.COMPLETED");
    }

    private static DriveThruSessionSyncState currentSyncState(final MockHttpSession session) {
        return (DriveThruSessionSyncState) session.getAttribute(DriveThruSessionSyncState.SYNC_STATE_KEY);
    }
}
