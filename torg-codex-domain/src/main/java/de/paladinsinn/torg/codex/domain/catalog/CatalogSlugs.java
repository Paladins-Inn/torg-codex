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

package de.paladinsinn.torg.codex.domain.catalog;

import java.text.Normalizer;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Pure, deterministic human-readable slug normalizer and UUID parser for {@code torg-codex}
 * URLs and cross-references (D4, FR-005, FR-008).
 */
public final class CatalogSlugs {

    private static final int SHORT_UUID_PREFIX_LENGTH = 8;
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("(^-+|-+$)");
    private static final Pattern CANONICAL_UUID = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private CatalogSlugs() {
    }

    /**
     * Converts an arbitrary display name or tag into a lowercase ASCII kebab-case slug.
     *
     * @param input the raw name or slug
     * @return normalized slug, or {@code ""} when {@code input} has no alphanumeric characters
     */
    public static String normalize(final String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String preFolded = input.trim()
                .replace("ß", "ss")
                .replace("ẞ", "ss")
                .replace("æ", "ae")
                .replace("Æ", "ae")
                .replace("œ", "oe")
                .replace("Œ", "oe")
                .replace("ø", "o")
                .replace("Ø", "o");
        String decomposed = Normalizer.normalize(preFolded, Normalizer.Form.NFKD);
        String withoutMarks = COMBINING_MARKS.matcher(decomposed).replaceAll("");
        String lower = withoutMarks.toLowerCase(Locale.ROOT);
        String hyphenated = NON_ALPHANUMERIC.matcher(lower).replaceAll("-");
        return EDGE_HYPHENS.matcher(hyphenated).replaceAll("");
    }

    /**
     * Checks whether {@code candidate} is a canonical 36-character UUID string.
     *
     * @param candidate the identifier string
     * @return {@code true} if {@code candidate} matches canonical UUID syntax
     */
    public static boolean isUuid(final String candidate) {
        return candidate != null && CANONICAL_UUID.matcher(candidate.trim()).matches();
    }

    /**
     * Parses {@code candidate} as a {@link UUID} when it matches canonical UUID syntax.
     *
     * @param candidate the identifier string
     * @return parsed UUID, or {@link Optional#empty()}
     */
    public static Optional<UUID> tryParseUuid(final String candidate) {
        if (!isUuid(candidate)) {
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(candidate.trim()));
    }

    /**
     * Computes deterministic, collision-free canonical slugs for all entries in a single catalog area.
     *
     * <p>Entries are ordered by canonical lowercase UUID string so slug assignment is completely
     * independent of database query ordering. The first entry claiming a base slug keeps it;
     * any subsequent colliding entry receives {@code <base-slug>-<first-8-uuid-hex>}.
     *
     * @param entries           the area entries
     * @param idExtractor       UUID extractor
     * @param baseSlugExtractor base slug extractor
     * @param <T>               entry type
     * @return immutable map from entry UUID to unique canonical slug
     */
    public static <T> Map<UUID, String> resolveCanonicalSlugs(
            final Collection<T> entries,
            final Function<T, UUID> idExtractor,
            final Function<T, String> baseSlugExtractor) {
        Objects.requireNonNull(entries, "entries must not be null");
        Objects.requireNonNull(idExtractor, "idExtractor must not be null");
        Objects.requireNonNull(baseSlugExtractor, "baseSlugExtractor must not be null");

        var sorted = entries.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(e -> idExtractor.apply(e).toString().toLowerCase(Locale.ROOT)))
                .toList();

        Set<String> claimed = new HashSet<>();
        Map<UUID, String> resolved = new LinkedHashMap<>(sorted.size());
        for (T entry : sorted) {
            UUID id = idExtractor.apply(entry);
            String shortHex = id.toString().substring(0, SHORT_UUID_PREFIX_LENGTH).toLowerCase(Locale.ROOT);
            String base = normalize(baseSlugExtractor.apply(entry));
            if (base.isEmpty()) {
                base = shortHex;
            }
            String candidate = base;
            if (!claimed.add(candidate)) {
                candidate = base + "-" + shortHex;
                if (!claimed.add(candidate)) {
                    candidate = base + "-" + id.toString().toLowerCase(Locale.ROOT);
                    claimed.add(candidate);
                }
            }
            resolved.put(id, candidate);
        }
        return Map.copyOf(resolved);
    }
}
