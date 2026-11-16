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

package de.paladinsinn.torg.codex.web.notification;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.paladinsinn.torg.codex.application.port.in.NotificationAction;
import de.paladinsinn.torg.codex.application.port.in.NotificationSeverity;
import de.paladinsinn.torg.codex.application.port.in.UserNotification;
import gg.jte.Content;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real headless-browser behavioral test executing Bootstrap 5, {@code /js/csrf.js}, and
 * {@code /js/notifications.js} inside headless Chrome over DevTools WebSocket protocol to verify
 * {@code EventSource} SSE reception within 5 seconds, zero polling requests, Bootstrap show/sticky/
 * autohide behavior, CSRF header transport on acknowledgments, {@code sessionStorage} replay
 * deduplication across navigation, and explicit-only page reload (T028, SC-003, ADR-024).
 */
@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest
class ToastBrowserTest {

    private static final int HTTP_OK = 200;
    private static final int HTTP_NO_CONTENT = 204;
    private static final int HTTP_INTERNAL_ERROR = 500;
    private static final int TIMEOUT_SECONDS = 10;
    private static final long DELIVERY_DEADLINE_NANOS = Duration.ofSeconds(5).toNanos();
    private static final long POLL_PARK_NANOS = Duration.ofMillis(25).toNanos();
    private static final String EXPECTED_CSRF = "browser-test-csrf-token-value";

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private ToastViewModelMapper toastMapper;

    private HttpServer server;
    private Process chromeProcess;
    private Path profileDir;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        profileDir = Files.createTempDirectory("torg-codex-chrome-test-");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (chromeProcess != null) {
            chromeProcess.destroyForcibly();
        }
        if (server != null) {
            server.stop(0);
        }
        if (profileDir != null && Files.exists(profileDir)) {
            try (var paths = Files.walk(profileDir)) {
                paths.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {
                                // Best-effort temp cleanup.
                            }
                        });
            }
        }
    }

    @Test
    void displaysSseToastsWithinFiveSecondsDeduplicatesReplaysSendsCsrfAckAndReloadsOnlyOnAction()
            throws Exception {
        String chromeBinary = findChromeBinary();
        Assumptions.assumeTrue(chromeBinary != null, "Headless Chrome/Chromium required for browser DOM test");

        UUID ssrInfoId = UUID.randomUUID();
        UUID liveStickyErrorId = UUID.randomUUID();
        UUID liveCompletionId = UUID.randomUUID();

        ToastViewModel ssrInfoToast = toastMapper.toViewModel(new UserNotification(
                ssrInfoId, NotificationSeverity.INFO, "Library Sync Started",
                "Loading in background", Instant.now(), null));
        ToastViewModel stickyErrorToast = toastMapper.toViewModel(new UserNotification(
                liveStickyErrorId, NotificationSeverity.ERROR, "Sticky Warning Check",
                "Remains visible until closed", Instant.now(), null));
        ToastViewModel completionToast = toastMapper.toViewModel(new UserNotification(
                liveCompletionId, NotificationSeverity.SUCCESS, "Library Sync Complete",
                "All books ready", Instant.now(), NotificationAction.reloadCurrentPage("Reload page")));

        AtomicInteger pageRenderCount = new AtomicInteger(0);
        AtomicInteger streamConnectCount = new AtomicInteger(0);
        AtomicInteger failNextAckCount = new AtomicInteger(1);
        List<String> ackBodies = new CopyOnWriteArrayList<>();
        List<String> ackCsrfHeaders = new CopyOnWriteArrayList<>();
        List<OutputStream> openSseStreams = new CopyOnWriteArrayList<>();
        CountDownLatch firstStreamConnected = new CountDownLatch(1);

        registerClasspathAsset("/webjars/bootstrap/dist/css/bootstrap.min.css",
                "META-INF/resources/webjars/bootstrap/5.3.8/dist/css/bootstrap.min.css", "text/css");
        registerClasspathAsset("/webjars/bootstrap-icons/font/bootstrap-icons.min.css",
                "META-INF/resources/webjars/bootstrap-icons/1.13.1/font/bootstrap-icons.min.css", "text/css");
        registerClasspathAsset("/webjars/bootstrap/dist/js/bootstrap.bundle.min.js",
                "META-INF/resources/webjars/bootstrap/5.3.8/dist/js/bootstrap.bundle.min.js", "application/javascript");
        registerClasspathAsset("/webjars/htmx.org/dist/htmx.min.js",
                "META-INF/resources/webjars/htmx.org/2.0.8/dist/htmx.min.js", "application/javascript");
        registerClasspathAsset("/themes/codex/codex.css", "static/themes/codex/codex.css", "text/css");
        registerClasspathAsset("/js/csrf.js", "static/js/csrf.js", "application/javascript");
        registerClasspathAsset("/js/notifications.js", "static/js/notifications.js", "application/javascript");

        server.createContext("/page", exchange -> {
            int renderNum = pageRenderCount.incrementAndGet();
            ToastRegionViewModel region = new ToastRegionViewModel(
                    List.of(ssrInfoToast), "/notifications/stream", "/notifications/ack");
            StringOutput htmlOut = new StringOutput();
            Content mainBody = out -> out.writeContent(
                    "<article id=\"reading-view\" data-render=\"" + renderNum + "\">Stable reading text</article>");
            templateEngine.render("codex/layout/page.jte", Map.of(
                    "title", "Browser Test",
                    "nav", de.paladinsinn.torg.codex.web.codex.model.NavContextViewModel.create(null, true),
                    "csrf", new CsrfViewModel("_csrf", "X-CSRF-TOKEN", EXPECTED_CSRF),
                    "toastRegion", region,
                    "content", mainBody), htmlOut);
            writeResponse(exchange, HTTP_OK, "text/html; charset=UTF-8", htmlOut.toString());
        });

        server.createContext("/notifications/stream", exchange -> {
            streamConnectCount.incrementAndGet();
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "private, no-store");
            exchange.sendResponseHeaders(HTTP_OK, 0);
            OutputStream sseOut = exchange.getResponseBody();
            sseOut.write(": connected\n\n".getBytes(StandardCharsets.UTF_8));
            sseOut.flush();
            openSseStreams.add(sseOut);
            firstStreamConnected.countDown();
        });

        server.createContext("/notifications/ack", exchange -> {
            String csrfHeader = exchange.getRequestHeaders().getFirst("X-CSRF-TOKEN");
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (failNextAckCount.getAndDecrement() > 0) {
                writeResponse(exchange, HTTP_INTERNAL_ERROR, "text/plain", "simulated transient failure");
                return;
            }
            if (csrfHeader != null) {
                ackCsrfHeaders.add(csrfHeader);
            }
            ackBodies.add(body);
            exchange.sendResponseHeaders(HTTP_NO_CONTENT, -1);
            exchange.close();
        });

        server.start();
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

        try (CdpSession cdp = launchHeadlessChrome(chromeBinary, baseUrl + "/page")) {
            assertThat(firstStreamConnected.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

            // 1. SSR info toast is initialized with Bootstrap .show and autohide=true, delay=6000
            awaitJsTrue(cdp, "document.querySelectorAll('.toast.show').length === 1");
            assertThat(cdp.evalString(
                    "document.querySelector('[data-notification-id=\"" + ssrInfoId + "\"]')"
                            + ".getAttribute('data-bs-autohide')")).isEqualTo("true");

            // 2. Push sticky error + completion toast over SSE -> delivered < 5s without page replacement!
            long pushStart = System.nanoTime();
            String pushedHtml = renderToastFragment(List.of(stickyErrorToast, completionToast));
            pushSseToast(openSseStreams, liveCompletionId.toString(), pushedHtml);
            awaitJsTrue(cdp, "document.querySelector('[data-notification-id=\"" + liveCompletionId + "\"]') !== null");
            assertThat(System.nanoTime() - pushStart).isLessThan(DELIVERY_DEADLINE_NANOS);

            assertThat(cdp.evalString("document.getElementById('reading-view').getAttribute('data-render')"))
                    .isEqualTo("1");
            assertThat(streamConnectCount.get()).isEqualTo(1);
            assertThat(ackCsrfHeaders).contains(EXPECTED_CSRF);
            assertThat(String.join("&", ackBodies))
                    .contains(ssrInfoId.toString(), liveStickyErrorId.toString(), liveCompletionId.toString());

            // 3. Clicking Reload page action triggers explicit reload; replayed SSR toast is deduplicated!
            cdp.evalString("document.querySelector('[data-toast-action=\"reload\"]').click(); 'clicked'");
            awaitJsTrue(cdp, "document.readyState === 'complete' "
                    + "&& document.getElementById('reading-view') !== null "
                    + "&& document.getElementById('reading-view').getAttribute('data-render') === '2'");
            awaitJsTrue(cdp, "document.querySelectorAll('[data-notification-id=\"" + ssrInfoId + "\"]').length === 0");
        }
    }

    private String renderToastFragment(final List<ToastViewModel> toasts) {
        StringOutput out = new StringOutput();
        templateEngine.render("codex/fragments/toasts.jte", Map.of("toasts", toasts), out);
        return out.toString();
    }

    private static void pushSseToast(
            final List<OutputStream> streams,
            final String eventId,
            final String html) throws IOException {
        StringBuilder frame = new StringBuilder();
        frame.append("id:").append(eventId).append('\n');
        frame.append("event:toast\n");
        for (String line : html.split("\n", -1)) {
            frame.append("data:").append(line).append('\n');
        }
        frame.append('\n');
        byte[] bytes = frame.toString().getBytes(StandardCharsets.UTF_8);
        for (OutputStream stream : streams) {
            stream.write(bytes);
            stream.flush();
        }
    }

    private void registerClasspathAsset(
            final String route,
            final String classpathLocation,
            final String contentType) {
        server.createContext(route, exchange -> {
            ClassPathResource resource = new ClassPathResource(classpathLocation);
            try (InputStream in = resource.getInputStream()) {
                byte[] data = in.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(HTTP_OK, data.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(data);
                }
            }
        });
    }

    private static void writeResponse(
            final HttpExchange exchange,
            final int status,
            final String contentType,
            final String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void awaitJsTrue(final CdpSession cdp, final String booleanExpression) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(TIMEOUT_SECONDS).toNanos();
        while (System.nanoTime() < deadline) {
            if ("true".equals(cdp.evalString("Boolean(" + booleanExpression + ").toString()"))) {
                return;
            }
            LockSupport.parkNanos(POLL_PARK_NANOS);
        }
        throw new AssertionError("Timed out waiting for JS condition: " + booleanExpression);
    }

    private static String findChromeBinary() {
        for (String candidate : List.of("/usr/bin/google-chrome", "/usr/bin/chromium", "/usr/bin/chromium-browser")) {
            if (Files.isExecutable(Path.of(candidate))) {
                return candidate;
            }
        }
        return null;
    }

    private CdpSession launchHeadlessChrome(final String binary, final String targetUrl) throws Exception {
        int debugPort;
        try (var socket = new java.net.ServerSocket(0)) {
            debugPort = socket.getLocalPort();
        }
        chromeProcess = new ProcessBuilder(
                binary,
                "--headless=new",
                "--no-sandbox",
                "--disable-gpu",
                "--disable-dev-shm-usage",
                "--user-data-dir=" + profileDir.toAbsolutePath(),
                "--remote-debugging-port=" + debugPort,
                targetUrl)
                .redirectErrorStream(true)
                .start();

        HttpClient client = HttpClient.newHttpClient();
        String wsUrl = pollPageDebuggerUrl(client, debugPort);
        return new CdpSession(client, URI.create(wsUrl));
    }

    @SuppressWarnings("unchecked")
    private static String pollPageDebuggerUrl(final HttpClient client, final int debugPort) throws Exception {
        URI listUri = URI.create("http://127.0.0.1:" + debugPort + "/json/list");
        long deadline = System.nanoTime() + Duration.ofSeconds(TIMEOUT_SECONDS).toNanos();
        while (System.nanoTime() < deadline) {
            try {
                HttpResponse<String> res = client.send(
                        HttpRequest.newBuilder(listUri).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == HTTP_OK) {
                    List<Object> targets = JsonParserFactory.getJsonParser().parseList(res.body());
                    for (Object item : targets) {
                        if (item instanceof Map<?, ?> map && "page".equals(map.get("type"))
                                && map.get("webSocketDebuggerUrl") instanceof String ws) {
                            return ws;
                        }
                    }
                }
            } catch (IOException ignored) {
                // Chrome still starting up.
            }
            LockSupport.parkNanos(POLL_PARK_NANOS);
        }
        throw new IllegalStateException("Headless Chrome DevTools endpoint did not become ready.");
    }

    /** Minimal JDK WebSocket client for Chrome DevTools Runtime.evaluate calls. */
    private static final class CdpSession implements AutoCloseable {

        private final AtomicInteger idSeq = new AtomicInteger(1);
        private final Map<Integer, CompletableFuture<String>> pending = new ConcurrentHashMap<>();
        private final WebSocket webSocket;

        CdpSession(final HttpClient client, final URI wsUri) {
            this.webSocket = client.newWebSocketBuilder()
                    .buildAsync(wsUri, new WebSocket.Listener() {
                        private final StringBuilder buffer = new StringBuilder();

                        @Override
                        public CompletableFuture<?> onText(
                                final WebSocket ws,
                                final CharSequence data,
                                final boolean last) {
                            buffer.append(data);
                            if (last) {
                                String message = buffer.toString();
                                buffer.setLength(0);
                                Map<String, Object> parsed = JsonParserFactory.getJsonParser().parseMap(message);
                                if (parsed.get("id") instanceof Number num) {
                                    CompletableFuture<String> future = pending.remove(num.intValue());
                                    if (future != null) {
                                        future.complete(message);
                                    }
                                }
                            }
                            ws.request(1);
                            return null;
                        }
                    }).join();
        }

        @SuppressWarnings("unchecked")
        String evalString(final String expression) throws Exception {
            int callId = idSeq.getAndIncrement();
            CompletableFuture<String> reply = new CompletableFuture<>();
            pending.put(callId, reply);
            String escaped = expression.replace("\\", "\\\\").replace("\"", "\\\"");
            String cmd = "{\"id\":" + callId + ",\"method\":\"Runtime.evaluate\","
                    + "\"params\":{\"expression\":\"" + escaped + "\",\"returnByValue\":true}}";
            webSocket.sendText(cmd, true).join();
            String raw = reply.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Map<String, Object> root = JsonParserFactory.getJsonParser().parseMap(raw);
            if (root.get("result") instanceof Map<?, ?> res
                    && res.get("result") instanceof Map<?, ?> inner
                    && inner.get("value") != null) {
                return String.valueOf(inner.get("value"));
            }
            return "";
        }

        @Override
        public void close() {
            webSocket.abort();
        }
    }
}
