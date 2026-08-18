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

package de.paladinsinn.torg.codex.domain.markup;

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
