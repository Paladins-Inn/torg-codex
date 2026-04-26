package de.paladinsinn.torg.codex.torgcodex.markup;

import java.util.Map;
import java.util.Set;

public final class GameTokenRegistry {

    private static final Map<String, String> TOKEN_CSS_CLASSES = Map.ofEntries(
            // Status effects
            Map.entry("shock", "game-token-shock"),
            Map.entry("wound", "game-token-wound"),
            Map.entry("wounds", "game-token-wound"),
            Map.entry("stymied", "game-token-stymied"),
            Map.entry("stymied2", "game-token-stymied2"),
            Map.entry("vulnerable", "game-token-vulnerable"),
            Map.entry("vulnerable2", "game-token-vulnerable2"),
            Map.entry("restrained", "game-token-restrained"),
            Map.entry("disconnect", "game-token-disconnect"),
            Map.entry("waiting", "game-token-waiting"),
            // Resources
            Map.entry("possibilities", "game-token-possibilities"),
            Map.entry("possibility", "game-token-possibilities"),
            Map.entry("bd", "game-token-bd"),
            // Weapon properties
            Map.entry("reload", "game-token-reload"),
            Map.entry("shotgun", "game-token-shotgun"),
            Map.entry("small", "game-token-small"),
            Map.entry("aim", "game-token-aim"),
            Map.entry("malfunction", "game-token-malfunction"),
            // Conditions
            Map.entry("concentration", "game-token-concentration"),
            Map.entry("dazing", "game-token-dazing"),
            Map.entry("fatigues", "game-token-fatigues"),
            Map.entry("head", "game-token-head")
    );

    private static final Set<String> KNOWN_TOKENS = TOKEN_CSS_CLASSES.keySet();

    private GameTokenRegistry() {}

    public static String cssClassFor(String tokenName) {
        String normalized = tokenName.toLowerCase();
        return TOKEN_CSS_CLASSES.getOrDefault(normalized, "game-token-unknown");
    }

    public static boolean isKnown(String tokenName) {
        return KNOWN_TOKENS.contains(tokenName.toLowerCase());
    }
}
