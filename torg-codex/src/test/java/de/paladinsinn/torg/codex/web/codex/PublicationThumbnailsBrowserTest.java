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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.core.io.ClassPathResource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real headless-browser behavioral test executing {@code /themes/codex/codex.css} and
 * {@code /js/publication-thumbnails.js} inside headless Chrome over DevTools WebSocket protocol
 * to verify JPEG success without PNG fallback, JPEG failure followed by single PNG fallback,
 * both failing without retries, cached completion/failure before script initialization, stable
 * frame dimensions, and usable navigation while images are pending (T011, US3).
 */
class PublicationThumbnailsBrowserTest {

    private static final int HTTP_OK = 200;
    private static final int HTTP_FOUND = 302;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int TIMEOUT_SECONDS = 10;
    private static final long POLL_PARK_NANOS = Duration.ofMillis(25).toNanos();
    private static final int SAMPLE_WIDTH = 140;
    private static final int SAMPLE_HEIGHT = 190;

    private HttpServer server;
    private Process chromeProcess;
    private Path profileDir;
    private byte[] validJpeg;
    private byte[] validPng;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        profileDir = Files.createTempDirectory("torg-codex-thumb-chrome-test-");
        validJpeg = createTestImage("jpg");
        validPng = createTestImage("png");
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
                paths.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // Best-effort temp cleanup.
                    }
                });
            }
        }
    }

    @Test
    void handlesJpegSuccessPngFallbackDoubleFailureCachedStateAndPendingNavigation() throws Exception {
        String chromeBinary = findChromeBinary();
        Assumptions.assumeTrue(chromeBinary != null, "Headless Chrome/Chromium binary is required for browser test");

        registerClasspathAsset("/themes/codex/codex.css", "static/themes/codex/codex.css", "text/css; charset=UTF-8");
        registerClasspathAsset("/js/publication-thumbnails.js",
                "static/js/publication-thumbnails.js",
                "application/javascript; charset=UTF-8");

        AtomicInteger jpgOkCount = new AtomicInteger();
        AtomicInteger jpgOkFallbackCount = new AtomicInteger();
        AtomicInteger jpgFailForPngCount = new AtomicInteger();
        AtomicInteger pngOkRedirectHopCount = new AtomicInteger();
        AtomicInteger pngOkTargetCount = new AtomicInteger();
        AtomicInteger bothFailJpgCount = new AtomicInteger();
        AtomicInteger bothFailPngCount = new AtomicInteger();
        AtomicInteger cachedFailPngCount = new AtomicInteger();
        CountDownLatch pendingImageStarted = new CountDownLatch(1);
        CountDownLatch releasePendingImage = new CountDownLatch(1);

        server.createContext("/img/ok.jpg", ex -> {
            jpgOkCount.incrementAndGet();
            writeBytes(ex, HTTP_OK, "image/jpeg", validJpeg, false);
        });
        server.createContext("/img/ok.png", ex -> {
            jpgOkFallbackCount.incrementAndGet();
            writeBytes(ex, HTTP_OK, "image/png", validPng, false);
        });
        server.createContext("/img/png-only.jpg", ex -> {
            jpgFailForPngCount.incrementAndGet();
            writeBytes(ex, HTTP_NOT_FOUND, "text/plain", new byte[0], false);
        });
        server.createContext("/img/png-only.png", ex -> {
            pngOkRedirectHopCount.incrementAndGet();
            ex.getResponseHeaders().set("Location", "/img/png-only-target.png");
            ex.sendResponseHeaders(HTTP_FOUND, -1);
            ex.close();
        });
        server.createContext("/img/png-only-target.png", ex -> {
            pngOkTargetCount.incrementAndGet();
            writeBytes(ex, HTTP_OK, "image/png", validPng, false);
        });
        server.createContext("/img/broken.jpg", ex -> {
            bothFailJpgCount.incrementAndGet();
            writeBytes(ex, HTTP_NOT_FOUND, "text/plain", new byte[0], false);
        });
        server.createContext("/img/broken.png", ex -> {
            bothFailPngCount.incrementAndGet();
            writeBytes(ex, HTTP_NOT_FOUND, "text/plain", new byte[0], false);
        });
        server.createContext("/img/cached-ok.jpg", ex -> writeBytes(ex, HTTP_OK, "image/jpeg", validJpeg, true));
        server.createContext("/img/cached-fail.jpg",
                ex -> writeBytes(ex, HTTP_NOT_FOUND, "text/plain", new byte[0], true));
        server.createContext("/img/cached-fail.png", ex -> {
            cachedFailPngCount.incrementAndGet();
            writeBytes(ex, HTTP_OK, "image/png", validPng, false);
        });
        server.createContext("/img/pending.jpg", ex -> {
            pendingImageStarted.countDown();
            try {
                releasePendingImage.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            writeBytes(ex, HTTP_OK, "image/jpeg", validJpeg, false);
        });
        server.createContext("/img/pending.png", ex -> writeBytes(ex, HTTP_OK, "image/png", validPng, false));

        // Pre-warm cache page for cached-ok.jpg and cached-fail.jpg, then navigate to /home.
        server.createContext("/warm", ex -> {
            String html = "<!DOCTYPE html><html><body>"
                    + "<img src=\"/img/cached-ok.jpg\">"
                    + "<img src=\"/img/cached-fail.jpg\">"
                    + "<a id=\"go-home\" href=\"/home\">go</a>"
                    + "</body></html>";
            writeBytes(ex, HTTP_OK, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8), false);
        });

        server.createContext("/home", ex -> {
            String html = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <link rel="stylesheet" href="/themes/codex/codex.css">
                    </head>
                    <body class="codex-reader-theme">
                        <nav id="categories-nav"><a href="/spells">Spells</a></nav>
                        <main>
                            <div class="card" id="card-jpg-ok">
                                <a href="/publications/jpg-ok" aria-labelledby="t-jpg-ok">
                                    <div class="publication-thumbnail">
                                        <img class="publication-thumbnail-img" src="/img/ok.jpg"
                                             data-fallback-src="/img/ok.png" alt="JPG OK">
                                    </div>
                                    <h3 id="t-jpg-ok">JPG OK</h3>
                                </a>
                                <a class="store-link" href="/store/1">Store</a>
                            </div>
                            <div class="card" id="card-png-fallback">
                                <a href="/publications/png-fallback" aria-labelledby="t-png-fallback">
                                    <div class="publication-thumbnail">
                                        <img class="publication-thumbnail-img" src="/img/png-only.jpg"
                                             data-fallback-src="/img/png-only.png" alt="PNG Fallback">
                                    </div>
                                    <h3 id="t-png-fallback">PNG Fallback</h3>
                                </a>
                            </div>
                            <div class="card" id="card-both-fail">
                                <a href="/publications/both-fail" aria-labelledby="t-both-fail">
                                    <div class="publication-thumbnail">
                                        <img class="publication-thumbnail-img" src="/img/broken.jpg"
                                             data-fallback-src="/img/broken.png" alt="Both Fail">
                                    </div>
                                    <h3 id="t-both-fail">Both Fail</h3>
                                </a>
                            </div>
                            <div class="card" id="card-cached-ok">
                                <a href="/publications/cached-ok" aria-labelledby="t-cached-ok">
                                    <div class="publication-thumbnail">
                                        <img class="publication-thumbnail-img" src="/img/cached-ok.jpg"
                                             data-fallback-src="/img/cached-ok.png" alt="Cached OK">
                                    </div>
                                    <h3 id="t-cached-ok">Cached OK</h3>
                                </a>
                            </div>
                            <div class="card" id="card-cached-fail">
                                <a href="/publications/cached-fail" aria-labelledby="t-cached-fail">
                                    <div class="publication-thumbnail">
                                        <img class="publication-thumbnail-img" src="/img/cached-fail.jpg"
                                             data-fallback-src="/img/cached-fail.png" alt="Cached Fail">
                                    </div>
                                    <h3 id="t-cached-fail">Cached Fail</h3>
                                </a>
                            </div>
                            <div class="card" id="card-pending">
                                <a href="/publications/pending" aria-labelledby="t-pending">
                                    <div class="publication-thumbnail">
                                        <img class="publication-thumbnail-img" src="/img/pending.jpg"
                                             data-fallback-src="/img/pending.png" alt="Pending">
                                    </div>
                                    <h3 id="t-pending">Pending</h3>
                                </a>
                            </div>
                        </main>
                        <script src="/js/publication-thumbnails.js" defer></script>
                    </body>
                    </html>
                    """;
            writeBytes(ex, HTTP_OK, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8), false);
        });

        server.start();
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

        try (CdpSession cdp = launchHeadlessChrome(chromeBinary, baseUrl + "/warm")) {
            awaitJsTrue(cdp, "document.readyState === 'complete' && document.getElementById('go-home') !== null");
            cdp.evalString("document.getElementById('go-home').click(); 'ok'");

            assertThat(pendingImageStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

            // While pending.jpg is still held: text, category nav, and links are visible and usable; frame is 140x190.
            awaitJsTrue(cdp, "document.querySelector('#card-jpg-ok .publication-thumbnail.is-loaded') !== null");
            awaitJsTrue(cdp, "document.querySelector('#card-png-fallback .publication-thumbnail.is-loaded') !== null");
            awaitJsTrue(cdp, "document.querySelector('#card-cached-ok .publication-thumbnail.is-loaded') !== null");
            awaitJsTrue(cdp, "document.querySelector('#card-cached-fail .publication-thumbnail.is-loaded') !== null");

            assertThat(cdp.evalString("document.querySelector('#card-pending .publication-thumbnail')"
                    + ".classList.contains('is-loaded').toString()")).isEqualTo("false");
            assertThat(cdp.evalString("getComputedStyle(document.querySelector('#card-pending img')).opacity"))
                    .isEqualTo("0");
            assertThat(cdp.evalString("Math.round(document.querySelector('#card-pending .publication-thumbnail')"
                    + ".getBoundingClientRect().width) + 'x' + "
                    + "Math.round(document.querySelector('#card-pending .publication-thumbnail')"
                    + ".getBoundingClientRect().height)")).isEqualTo("140x190");
            assertThat(cdp.evalString("document.querySelector('#categories-nav a').getBoundingClientRect().height > 0"))
                    .isEqualTo("true");
            assertThat(cdp.evalString("document.querySelector('#card-pending h3').textContent")).isEqualTo("Pending");

            // Release pending image and confirm it transitions to loaded without changing frame dimensions.
            releasePendingImage.countDown();
            awaitJsTrue(cdp, "document.querySelector('#card-pending .publication-thumbnail.is-loaded') !== null");
            assertThat(cdp.evalString("Math.round(document.querySelector('#card-pending .publication-thumbnail')"
                    + ".getBoundingClientRect().width) + 'x' + "
                    + "Math.round(document.querySelector('#card-pending .publication-thumbnail')"
                    + ".getBoundingClientRect().height)")).isEqualTo("140x190");

            // Verify both-failed card stays in neutral frame (not loaded, opacity 0) and no retries occur.
            awaitJsTrue(cdp, "document.querySelector('#card-both-fail img').getAttribute('data-fallback-src') === null");
            assertThat(cdp.evalString("document.querySelector('#card-both-fail .publication-thumbnail')"
                    + ".classList.contains('is-loaded').toString()")).isEqualTo("false");
            assertThat(cdp.evalString("getComputedStyle(document.querySelector('#card-both-fail img')).opacity"))
                    .isEqualTo("0");

            assertThat(jpgOkCount.get()).isEqualTo(1);
            assertThat(jpgOkFallbackCount.get()).isZero();
            assertThat(jpgFailForPngCount.get()).isEqualTo(1);
            assertThat(pngOkRedirectHopCount.get()).isEqualTo(1);
            assertThat(pngOkTargetCount.get()).isEqualTo(1);
            assertThat(bothFailJpgCount.get()).isEqualTo(1);
            assertThat(bothFailPngCount.get()).isEqualTo(1);
            assertThat(cachedFailPngCount.get()).isEqualTo(1);
        }
    }

    @Test
    void publicationDetailHeaderHandlesJpegPngFallbackAndDoubleFailureWithoutBrokenImage() throws Exception {
        String chromeBinary = findChromeBinary();
        Assumptions.assumeTrue(chromeBinary != null, "Headless Chrome/Chromium binary is required for browser test");

        registerClasspathAsset("/themes/codex/codex.css", "static/themes/codex/codex.css", "text/css; charset=UTF-8");
        registerClasspathAsset("/js/publication-thumbnails.js",
                "static/js/publication-thumbnails.js",
                "application/javascript; charset=UTF-8");

        server.createContext("/img/detail-ok.jpg", ex -> writeBytes(ex, HTTP_OK, "image/jpeg", validJpeg, false));
        server.createContext("/img/detail-png.jpg",
                ex -> writeBytes(ex, HTTP_NOT_FOUND, "text/plain", new byte[0], false));
        server.createContext("/img/detail-png.png", ex -> writeBytes(ex, HTTP_OK, "image/png", validPng, false));
        server.createContext("/img/detail-broken.jpg",
                ex -> writeBytes(ex, HTTP_NOT_FOUND, "text/plain", new byte[0], false));
        server.createContext("/img/detail-broken.png",
                ex -> writeBytes(ex, HTTP_NOT_FOUND, "text/plain", new byte[0], false));

        server.createContext("/detail", ex -> {
            String html = """
                    <!DOCTYPE html>
                    <html>
                    <head><link rel="stylesheet" href="/themes/codex/codex.css"></head>
                    <body>
                        <header id="detail-ok">
                            <div class="publication-thumbnail">
                                <img class="publication-thumbnail-img" src="/img/detail-ok.jpg"
                                     data-fallback-src="/img/detail-ok.png" alt="">
                            </div>
                            <h1>Detail OK</h1>
                        </header>
                        <header id="detail-png">
                            <div class="publication-thumbnail">
                                <img class="publication-thumbnail-img" src="/img/detail-png.jpg"
                                     data-fallback-src="/img/detail-png.png" alt="">
                            </div>
                            <h1>Detail PNG</h1>
                        </header>
                        <header id="detail-broken">
                            <div class="publication-thumbnail">
                                <img class="publication-thumbnail-img" src="/img/detail-broken.jpg"
                                     data-fallback-src="/img/detail-broken.png" alt="">
                            </div>
                            <h1>Detail Broken</h1>
                        </header>
                        <script src="/js/publication-thumbnails.js" defer></script>
                    </body>
                    </html>
                    """;
            writeBytes(ex, HTTP_OK, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8), false);
        });

        server.start();
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        try (CdpSession cdp = launchHeadlessChrome(chromeBinary, baseUrl + "/detail")) {
            awaitJsTrue(cdp, "document.querySelector('#detail-ok .publication-thumbnail.is-loaded') !== null");
            awaitJsTrue(cdp, "document.querySelector('#detail-png .publication-thumbnail.is-loaded') !== null");
            awaitJsTrue(cdp, "document.querySelector('#detail-broken img').getAttribute('data-fallback-src') === null");
            assertThat(cdp.evalString("document.querySelector('#detail-broken .publication-thumbnail')"
                    + ".classList.contains('is-loaded').toString()")).isEqualTo("false");
            assertThat(cdp.evalString("getComputedStyle(document.querySelector('#detail-broken img')).opacity"))
                    .isEqualTo("0");
        }
    }

    private static byte[] createTestImage(final String format) throws IOException {
        BufferedImage image = new BufferedImage(SAMPLE_WIDTH, SAMPLE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    private void registerClasspathAsset(
            final String route,
            final String classpathLocation,
            final String contentType) {
        server.createContext(route, exchange -> {
            ClassPathResource resource = new ClassPathResource(classpathLocation);
            try (InputStream in = resource.getInputStream()) {
                byte[] data = in.readAllBytes();
                writeBytes(exchange, HTTP_OK, contentType, data, false);
            }
        });
    }

    private static void writeBytes(
            final HttpExchange exchange,
            final int status,
            final String contentType,
            final byte[] body,
            final boolean cacheable) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", cacheable ? "public, max-age=3600" : "no-store");
        if (body.length == 0) {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
            return;
        }
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
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
            return null;
        }

        @Override
        public void close() {
            webSocket.abort();
        }
    }
}
