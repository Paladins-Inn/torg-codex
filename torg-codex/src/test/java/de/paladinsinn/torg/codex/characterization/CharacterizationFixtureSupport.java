package de.paladinsinn.torg.codex.characterization;

import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.json.JsonWriter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class CharacterizationFixtureSupport {
    static final Path FIXTURE_ROOT = Path.of("src", "test", "resources", "characterization");
    static final UUID MISSING_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private static final List<String> IGNORED_HEADERS = List.of("Content-Length");

    private CharacterizationFixtureSupport() {
    }

    static void writeFixture(CatalogArea area, CharacterizationFixture fixture) throws IOException {
        Path directory = FIXTURE_ROOT.resolve(area.fixtureDirectory());
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(fixture.name() + ".json"), toJson(fixture) + System.lineSeparator());
    }

    static CharacterizationFixture readFixture(Path path) throws IOException {
        Map<String, Object> document = JsonParserFactory.getJsonParser().parseMap(Files.readString(path));
        Map<String, Object> request = castMap(document.get("request"));
        Map<String, Object> response = castMap(document.get("response"));
        return new CharacterizationFixture(
                (String) document.get("name"),
                new CharacterizationFixture.Request(
                        (String) request.get("method"),
                        (String) request.get("path"),
                        toStringListMap(request.get("queryParameters")),
                        CharacterizationAuthVariant.valueOf((String) request.get("authVariant"))),
                new CharacterizationFixture.Response(
                        ((Number) response.get("status")).intValue(),
                        toStringListMap(response.get("headers")),
                        (String) response.get("body")));
    }

    static Map<String, List<String>> queryParameters(String name, String value) {
        return Map.of(name, List.of(value));
    }

    static Map<String, List<String>> normalizeHeaders(MockHttpServletResponse response) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        response.getHeaderNames().stream()
                .filter(name -> IGNORED_HEADERS.stream().noneMatch(ignored -> ignored.equalsIgnoreCase(name)))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(name -> headers.put(name, response.getHeaders(name)));
        return headers;
    }

    static String normalizeBody(MockHttpServletResponse response) throws IOException {
        String body = response.getContentAsString();
        if (body == null || body.isBlank()) {
            return null;
        }

        if (response.getContentType() != null && response.getContentType().contains(MediaType.APPLICATION_JSON_VALUE)) {
            return canonicalizeJson(body);
        }

        return body;
    }

    static UUID sampleId(Object entity) {
        return invoke(entity, "getId", UUID.class);
    }

    static String sampleCosm(Object entity) {
        return invoke(entity, "getCosm", String.class);
    }

    static <T> T invoke(Object target, String methodName, Class<T> returnType) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            return returnType.cast(value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to invoke %s on %s".formatted(methodName, target.getClass().getName()), exception);
        }
    }

    static List<Path> fixtureFiles() throws IOException {
        try (var stream = Files.walk(FIXTURE_ROOT)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
    }

    static boolean responsesDiffer(CharacterizationFixture.Response left, CharacterizationFixture.Response right) {
        return left.status() != right.status()
                || !Objects.equals(left.headers(), right.headers())
                || !Objects.equals(left.body(), right.body());
    }

    static String fixtureName(Path path) {
        return path.getFileName().toString().replaceFirst("\\.json$", "");
    }

    static CatalogArea areaFor(Path path) {
        String directory = path.getParent().getFileName().toString();
        return Arrays.stream(CatalogArea.values())
                .filter(area -> area.fixtureDirectory().equals(directory))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown characterization directory: " + directory));
    }

    private static String toJson(CharacterizationFixture fixture) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("method", fixture.request().method());
        request.put("path", fixture.request().path());
        request.put("queryParameters", fixture.request().queryParameters());
        request.put("authVariant", fixture.request().authVariant().name());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", fixture.response().status());
        response.put("headers", fixture.response().headers());
        response.put("body", fixture.response().body());

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("name", fixture.name());
        document.put("request", request);
        document.put("response", response);
        return JsonWriter.<Map<String, Object>>standard().writeToString(document);
    }

    private static String canonicalizeJson(String body) {
        String trimmed = body.strip();
        if (trimmed.startsWith("{")) {
            return JsonWriter.<Map<String, Object>>standard().writeToString(JsonParserFactory.getJsonParser().parseMap(trimmed));
        }
        if (trimmed.startsWith("[")) {
            return JsonWriter.<List<Object>>standard().writeToString(JsonParserFactory.getJsonParser().parseList(trimmed));
        }
        return trimmed;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return value == null ? Map.of() : (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> toStringListMap(Object value) {
        Map<String, Object> source = castMap(value);
        Map<String, List<String>> result = new LinkedHashMap<>();
        source.forEach((key, nestedValue) -> result.put(key, ((List<Object>) nestedValue).stream().map(String::valueOf).toList()));
        return result;
    }
}
