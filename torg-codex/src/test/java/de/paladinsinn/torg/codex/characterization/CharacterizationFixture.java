package de.paladinsinn.torg.codex.characterization;

import java.util.List;
import java.util.Map;

record CharacterizationFixture(
        String name,
        Request request,
        Response response) {

    record Request(
            String method,
            String path,
            Map<String, List<String>> queryParameters,
            CharacterizationAuthVariant authVariant) {
    }

    record Response(
            int status,
            Map<String, List<String>> headers,
            String body) {
    }
}
