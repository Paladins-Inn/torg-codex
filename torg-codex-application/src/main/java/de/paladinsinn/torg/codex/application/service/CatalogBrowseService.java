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

package de.paladinsinn.torg.codex.application.service;

import de.paladinsinn.torg.codex.application.port.in.CatalogBrowseQuery;
import de.paladinsinn.torg.codex.application.port.in.CatalogBrowseResult;
import de.paladinsinn.torg.codex.application.port.in.CatalogBrowseResult.FacetOption;
import de.paladinsinn.torg.codex.application.port.in.CatalogBrowseResult.ResolvedEntry;
import de.paladinsinn.torg.codex.application.port.in.CatalogDetailResult;
import de.paladinsinn.torg.codex.application.port.in.CatalogDetailResult.LinkedEntryRef;
import de.paladinsinn.torg.codex.application.port.in.CatalogDetailResult.RequiredProductRef;
import de.paladinsinn.torg.codex.application.port.in.CatalogFilterCriteria;
import de.paladinsinn.torg.codex.application.port.in.CatalogFilterCriteria.SortDirection;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.application.port.in.RestrictedCatalogQuery;
import de.paladinsinn.torg.codex.application.port.out.CatalogUsageObserver;
import de.paladinsinn.torg.codex.domain.access.EntryAccess;
import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import de.paladinsinn.torg.codex.domain.catalog.CatalogCategoryDescriptor;
import de.paladinsinn.torg.codex.domain.catalog.CatalogCategoryRegistry;
import de.paladinsinn.torg.codex.domain.catalog.CatalogSlugs;
import de.paladinsinn.torg.codex.domain.catalog.ColumnDescriptor;
import de.paladinsinn.torg.codex.domain.catalog.DetailSectionDescriptor;
import de.paladinsinn.torg.codex.domain.catalog.DetailSectionDescriptor.EmbeddedReferences;
import de.paladinsinn.torg.codex.domain.catalog.FilterFacetDescriptor;
import de.paladinsinn.torg.codex.domain.catalog.GroupingDimension;
import de.paladinsinn.torg.codex.domain.markup.EntityType;
import de.paladinsinn.torg.codex.domain.model.Article;
import de.paladinsinn.torg.codex.domain.model.CatalogEntry;
import de.paladinsinn.torg.codex.domain.model.ClearanceLevel;
import de.paladinsinn.torg.codex.domain.model.Cosm;
import de.paladinsinn.torg.codex.domain.model.Perk;
import de.paladinsinn.torg.codex.domain.model.ProductBoundEntry;
import de.paladinsinn.torg.codex.domain.model.Publication;
import de.paladinsinn.torg.codex.domain.model.ReferencesEntries;
import de.paladinsinn.torg.codex.domain.model.Tag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Framework-free implementation of {@link CatalogBrowseQuery} serving all 17 catalog areas
 * uniformly over their existing restricted and public driving ports (FR-016, D5).
 */
public final class CatalogBrowseService implements CatalogBrowseQuery {

    private final Map<CatalogArea, AreaSource> sources;
    private final CatalogUsageObserver usageObserver;

    public CatalogBrowseService(
            final List<AreaSource> areaSources,
            final CatalogUsageObserver usageObserver) {
        Objects.requireNonNull(areaSources, "areaSources must not be null");
        this.usageObserver = Objects.requireNonNull(usageObserver, "usageObserver must not be null");
        Map<CatalogArea, AreaSource> map = new EnumMap<>(CatalogArea.class);
        for (AreaSource source : areaSources) {
            map.put(source.area(), source);
        }
        this.sources = Map.copyOf(map);
    }

    /**
     * Typed adapter wrapping either a {@link RestrictedCatalogQuery} (for product-bound areas) or
     * a concrete {@link CatalogQuery} (for Article, Publication, and Tag, complying with ArchUnit A2).
     */
    public interface AreaSource {

        CatalogArea area();

        List<? extends CatalogEntry> findAllWithUsage();

        List<? extends CatalogEntry> peekAll();

        RawLookupResult findByIdentifier(String slugOrId);

        static <P extends ProductBoundEntry> AreaSource restricted(
                final CatalogArea area, final RestrictedCatalogQuery<P> query) {
            return new AreaSource() {
                @Override
                public CatalogArea area() {
                    return area;
                }

                @Override
                public List<? extends CatalogEntry> findAllWithUsage() {
                    return query.findAll();
                }

                @Override
                public List<? extends CatalogEntry> peekAll() {
                    return query.peekAll();
                }

                @Override
                public RawLookupResult findByIdentifier(final String slugOrId) {
                    return switch (query.findByIdentifier(slugOrId)) {
                        case EntryAccess.Granted<P> g -> new RawLookupResult.GrantedProduct(g.entry());
                        case EntryAccess.Teaser<P> t -> new RawLookupResult.Teaser(t);
                        case EntryAccess.Absent<P> ignored -> new RawLookupResult.NotFound();
                    };
                }
            };
        }

        static AreaSource articles(final CatalogQuery<Article> query) {
            return publicSource(CatalogArea.ARTICLE, query::findAll, query::peekAll, query::findByIdentifier);
        }

        static AreaSource publications(final CatalogQuery<Publication> query) {
            return publicSource(CatalogArea.PUBLICATION, query::findAll, query::peekAll, query::findByIdentifier);
        }

        static AreaSource tags(final CatalogQuery<Tag> query) {
            return publicSource(CatalogArea.TAG, query::findAll, query::peekAll, query::findByIdentifier);
        }

        private static <R extends CatalogEntry> AreaSource publicSource(
                final CatalogArea area,
                final java.util.function.Supplier<List<R>> findAll,
                final java.util.function.Supplier<List<R>> peekAll,
                final Function<String, Optional<R>> findByIdentifier) {
            return new AreaSource() {
                @Override
                public CatalogArea area() {
                    return area;
                }

                @Override
                public List<? extends CatalogEntry> findAllWithUsage() {
                    return findAll.get();
                }

                @Override
                public List<? extends CatalogEntry> peekAll() {
                    return peekAll.get();
                }

                @Override
                public RawLookupResult findByIdentifier(final String slugOrId) {
                    return findByIdentifier.apply(slugOrId)
                            .<RawLookupResult>map(RawLookupResult.PublicEntry::new)
                            .orElseGet(RawLookupResult.NotFound::new);
                }
            };
        }
    }

    /**
     * Raw single-entry lookup result before view-level reference enrichment.
     */
    public sealed interface RawLookupResult
            permits RawLookupResult.GrantedProduct,
                    RawLookupResult.PublicEntry,
                    RawLookupResult.Teaser,
                    RawLookupResult.NotFound {

        record GrantedProduct(ProductBoundEntry entry) implements RawLookupResult {
        }

        record PublicEntry(CatalogEntry entry) implements RawLookupResult {
        }

        record Teaser(EntryAccess.Teaser<?> teaser) implements RawLookupResult {
        }

        record NotFound() implements RawLookupResult {
        }
    }

    Map<CatalogArea, AreaSource> sources() {
        return sources;
    }

    @Override
    public CatalogBrowseResult browse(final CatalogArea area, final CatalogFilterCriteria rawCriteria) {
        CatalogFilterCriteria criteria = rawCriteria == null ? CatalogFilterCriteria.empty() : rawCriteria;
        AreaSource source = requireSource(area);
        CatalogCategoryDescriptor<? extends CatalogEntry> descriptor = CatalogCategoryRegistry.forArea(area);

        // Triggers categoryAccessed(area) exactly once via underlying query.findAll().
        List<? extends CatalogEntry> accessible = source.findAllWithUsage();
        Map<UUID, String> canonicalSlugs = CatalogSlugs.resolveCanonicalSlugs(
                accessible, CatalogEntry::id, CatalogEntry::slug);

        Map<String, String> knownCosms = buildKnownCosmLabels(accessible);
        List<FacetOption> cosmOptions = buildCosmFacetOptions(descriptor, knownCosms, criteria.cosms());

        Map<GroupingDimension, FacetContext> facetContexts = buildFacetContexts(descriptor, accessible, criteria);
        Map<GroupingDimension, List<FacetOption>> availableFacets = new EnumMap<>(GroupingDimension.class);
        facetContexts.forEach((dim, ctx) -> availableFacets.put(dim, ctx.options()));

        // Record cosm and grouping filter metrics only for valid known catalog keys (Guarantee B2).
        for (String requestedCosm : criteria.cosms()) {
            if (knownCosms.containsKey(requestedCosm)) {
                usageObserver.cosmAccessed(requestedCosm);
            }
        }
        criteria.groupings().forEach((dim, requestedValues) -> {
            FacetContext ctx = facetContexts.get(dim);
            if (ctx != null) {
                for (String val : requestedValues) {
                    if (ctx.validKeys().contains(val)) {
                        usageObserver.groupingAccessed(dim, val);
                    }
                }
            }
        });

        List<CatalogEntry> filtered = accessible.stream()
                .filter(entry -> matchesCriteria(entry, descriptor, criteria, facetContexts))
                .map(CatalogEntry.class::cast)
                .toList();

        List<CatalogEntry> sorted = sortEntries(filtered, descriptor, criteria.sortKey(), criteria.direction());
        List<ResolvedEntry> resolved = sorted.stream()
                .map(entry -> new ResolvedEntry(entry, canonicalSlugs.getOrDefault(entry.id(), entry.slug())))
                .toList();

        return new CatalogBrowseResult(
                area,
                descriptor,
                resolved,
                accessible.size(),
                cosmOptions,
                Map.copyOf(availableFacets),
                criteria);
    }

    @Override
    public CatalogDetailResult findDetail(final CatalogArea area, final String slugOrId) {
        AreaSource source = requireSource(area);
        CatalogCategoryDescriptor<? extends CatalogEntry> descriptor = CatalogCategoryRegistry.forArea(area);
        RawLookupResult lookup = source.findByIdentifier(slugOrId);

        return switch (lookup) {
            case RawLookupResult.GrantedProduct gp -> {
                ProductBoundEntry entry = gp.entry();
                String canonicalSlug = resolveSingleCanonicalSlug(source, entry);
                Map<String, String> cosmLabels = buildKnownCosmLabels(List.of(entry));
                Map<String, List<LinkedEntryRef>> embedded = resolveEmbeddedReferences(descriptor, entry);
                List<Publication> pubs = resolvePublications(entry.productCodexIds());
                yield new CatalogDetailResult.GrantedProductEntry(
                        area, descriptor, entry, canonicalSlug, cosmLabels, embedded, pubs);
            }
            case RawLookupResult.PublicEntry pe -> {
                CatalogEntry entry = pe.entry();
                String canonicalSlug = resolveSingleCanonicalSlug(source, entry);
                Map<String, List<LinkedEntryRef>> embedded = resolveEmbeddedReferences(descriptor, entry);
                yield new CatalogDetailResult.PublicReferenceEntry(
                        area, descriptor, entry, canonicalSlug, embedded);
            }
            case RawLookupResult.Teaser t -> {
                Map<String, Publication> pubsByCodexId = publicationsByCodexId();
                List<RequiredProductRef> required = t.teaser().requiredProducts().stream()
                        .map(codexId -> new RequiredProductRef(
                                codexId, Optional.ofNullable(pubsByCodexId.get(codexId))))
                        .toList();
                yield new CatalogDetailResult.RestrictedTeaser(area, t.teaser(), required);
            }
            case RawLookupResult.NotFound ignored -> new CatalogDetailResult.Absent(area);
        };
    }

    private record FacetContext(
            List<FacetOption> options,
            Set<String> validKeys,
            Function<CatalogEntry, Set<String>> entryKeysExtractor) {
    }

    private AreaSource requireSource(final CatalogArea area) {
        AreaSource source = sources.get(area);
        if (source == null) {
            throw new IllegalStateException("No AreaSource wired for " + area);
        }
        return source;
    }

    private String resolveSingleCanonicalSlug(final AreaSource source, final CatalogEntry entry) {
        List<? extends CatalogEntry> peers = source.peekAll();
        Map<UUID, String> slugs = CatalogSlugs.resolveCanonicalSlugs(peers, CatalogEntry::id, CatalogEntry::slug);
        return slugs.getOrDefault(entry.id(), entry.slug());
    }

    private Map<String, String> buildKnownCosmLabels(final List<? extends CatalogEntry> currentEntries) {
        Map<String, String> labels = new LinkedHashMap<>();
        AreaSource cosmSource = sources.get(CatalogArea.COSM);
        if (cosmSource != null) {
            for (CatalogEntry entry : cosmSource.peekAll()) {
                if (entry instanceof Cosm cosm && cosm.slug() != null) {
                    labels.put(CatalogSlugs.normalize(cosm.slug()), cosm.name());
                }
            }
        }
        for (CatalogEntry entry : currentEntries) {
            for (String cosmSlug : entry.cosmSlugs()) {
                labels.computeIfAbsent(cosmSlug, EntityType::toDisplayName);
            }
        }
        return Map.copyOf(labels);
    }

    private List<FacetOption> buildCosmFacetOptions(
            final CatalogCategoryDescriptor<? extends CatalogEntry> descriptor,
            final Map<String, String> knownCosms,
            final Set<String> selectedCosms) {
        boolean hasCosmFacet = descriptor.facets().stream().anyMatch(FilterFacetDescriptor::cosmFacet);
        if (!hasCosmFacet) {
            return List.of();
        }
        return knownCosms.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER))
                .map(e -> new FacetOption(e.getKey(), e.getValue(), selectedCosms.contains(e.getKey())))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Map<GroupingDimension, FacetContext> buildFacetContexts(
            final CatalogCategoryDescriptor<? extends CatalogEntry> rawDescriptor,
            final List<? extends CatalogEntry> accessible,
            final CatalogFilterCriteria criteria) {
        CatalogCategoryDescriptor<CatalogEntry> descriptor = (CatalogCategoryDescriptor<CatalogEntry>) rawDescriptor;
        Map<GroupingDimension, FacetContext> contexts = new EnumMap<>(GroupingDimension.class);

        for (FilterFacetDescriptor facet : descriptor.facets()) {
            if (facet.cosmFacet() || facet.dimension() == null) {
                continue;
            }
            GroupingDimension dim = facet.dimension();
            Set<String> active = criteria.groupings().getOrDefault(dim, Set.of());
            contexts.put(dim, createFacetContext(descriptor, accessible, dim, active));
        }
        return contexts;
    }

    private FacetContext createFacetContext(
            final CatalogCategoryDescriptor<CatalogEntry> descriptor,
            final List<? extends CatalogEntry> accessible,
            final GroupingDimension dim,
            final Set<String> active) {
        return switch (dim) {
            case CLEARANCE -> {
                List<FacetOption> opts = new ArrayList<>();
                Set<String> valid = new LinkedHashSet<>();
                for (ClearanceLevel level : ClearanceLevel.values()) {
                    String key = level.getFullName();
                    valid.add(key);
                    opts.add(new FacetOption(key, level.getSymbol() + " (" + EntityType.toDisplayName(key) + ")",
                            active.contains(key)));
                }
                yield new FacetContext(List.copyOf(opts), Set.copyOf(valid),
                        e -> e.clearance().map(c -> Set.of(c.getFullName())).orElse(Set.of()));
            }
            case SPELL_LIST -> buildReverseListFacetContext(CatalogArea.SPELL_LIST, active);
            case MIRACLE_LIST -> buildReverseListFacetContext(CatalogArea.MIRACLE_LIST, active);
            case POWER_LIST -> buildReverseListFacetContext(CatalogArea.POWER_LIST, active);
            case RACE -> buildPerkRaceFacetContext(active);
            case PERK_GROUP -> buildPerkGroupFacetContext(descriptor, accessible, active);
            case ITEM_TYPE, VEHICLE_TYPE, TAG -> buildDirectFacetContext(descriptor, accessible, dim, active);
        };
    }

    private FacetContext buildReverseListFacetContext(final CatalogArea listArea, final Set<String> active) {
        AreaSource listSource = sources.get(listArea);
        if (listSource == null) {
            return new FacetContext(List.of(), Set.of(), e -> Set.of());
        }
        List<? extends CatalogEntry> lists = listSource.peekAll();
        Map<UUID, String> listSlugs = CatalogSlugs.resolveCanonicalSlugs(lists, CatalogEntry::id, CatalogEntry::slug);
        Map<UUID, Set<String>> memberIdToListSlugs = new HashMap<>();
        List<FacetOption> opts = new ArrayList<>();
        Set<String> valid = new LinkedHashSet<>();

        lists.stream()
                .sorted(Comparator.comparing(CatalogEntry::name, String.CASE_INSENSITIVE_ORDER))
                .forEach(listEntry -> {
                    String slug = listSlugs.getOrDefault(listEntry.id(), listEntry.slug());
                    valid.add(slug);
                    opts.add(new FacetOption(slug, listEntry.name(), active.contains(slug)));
                    if (listEntry instanceof ReferencesEntries<?> ref) {
                        for (UUID childId : ref.referencedEntries()) {
                            memberIdToListSlugs.computeIfAbsent(childId, k -> new LinkedHashSet<>()).add(slug);
                        }
                    }
                });
        return new FacetContext(List.copyOf(opts), Set.copyOf(valid),
                entry -> memberIdToListSlugs.getOrDefault(entry.id(), Set.of()));
    }

    private FacetContext buildPerkRaceFacetContext(final Set<String> active) {
        AreaSource raceSource = sources.get(CatalogArea.RACE);
        if (raceSource == null) {
            return new FacetContext(List.of(), Set.of(), e -> Set.of());
        }
        List<? extends CatalogEntry> races = raceSource.peekAll();
        Map<UUID, String> raceSlugs = CatalogSlugs.resolveCanonicalSlugs(races, CatalogEntry::id, CatalogEntry::slug);
        List<FacetOption> opts = new ArrayList<>();
        Set<String> valid = new LinkedHashSet<>();

        races.stream()
                .sorted(Comparator.comparing(CatalogEntry::name, String.CASE_INSENSITIVE_ORDER))
                .forEach(race -> {
                    String slug = raceSlugs.getOrDefault(race.id(), race.slug());
                    valid.add(slug);
                    opts.add(new FacetOption(slug, race.name(), active.contains(slug)));
                });

        return new FacetContext(List.copyOf(opts), Set.copyOf(valid), entry -> {
            if (!(entry instanceof Perk perk) || perk.prerequisites() == null || perk.prerequisites().isBlank()) {
                return Set.of();
            }
            String normalizedPrereq = "-" + CatalogSlugs.normalize(perk.prerequisites()) + "-";
            Set<String> matched = new LinkedHashSet<>();
            for (String raceSlug : valid) {
                if (normalizedPrereq.contains("-" + raceSlug + "-")) {
                    matched.add(raceSlug);
                }
            }
            return matched.isEmpty() ? Set.of() : Set.copyOf(matched);
        });
    }

    private FacetContext buildPerkGroupFacetContext(
            final CatalogCategoryDescriptor<CatalogEntry> descriptor,
            final List<? extends CatalogEntry> accessible,
            final Set<String> active) {
        Map<String, String> keyToLabel = new LinkedHashMap<>();
        AreaSource groupSource = sources.get(CatalogArea.PERK_GROUP);
        if (groupSource != null) {
            List<? extends CatalogEntry> groups = groupSource.peekAll();
            Map<UUID, String> slugs = CatalogSlugs.resolveCanonicalSlugs(groups, CatalogEntry::id, CatalogEntry::slug);
            for (CatalogEntry group : groups) {
                keyToLabel.put(slugs.getOrDefault(group.id(), group.slug()), group.name());
            }
        }
        for (CatalogEntry entry : accessible) {
            for (String key : descriptor.extractDirectGroupingKeys(entry, GroupingDimension.PERK_GROUP)) {
                keyToLabel.computeIfAbsent(key, EntityType::toDisplayName);
            }
        }
        List<FacetOption> opts = keyToLabel.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER))
                .map(e -> new FacetOption(e.getKey(), e.getValue(), active.contains(e.getKey())))
                .toList();
        return new FacetContext(opts, Set.copyOf(keyToLabel.keySet()),
                e -> descriptor.extractDirectGroupingKeys(e, GroupingDimension.PERK_GROUP));
    }

    private FacetContext buildDirectFacetContext(
            final CatalogCategoryDescriptor<CatalogEntry> descriptor,
            final List<? extends CatalogEntry> accessible,
            final GroupingDimension dim,
            final Set<String> active) {
        Map<String, String> keyToLabel = new LinkedHashMap<>();
        if (dim == GroupingDimension.TAG) {
            AreaSource tagSource = sources.get(CatalogArea.TAG);
            if (tagSource != null) {
                for (CatalogEntry tag : tagSource.peekAll()) {
                    keyToLabel.put(tag.slug(), tag.name());
                }
            }
        }
        for (CatalogEntry entry : accessible) {
            for (String key : descriptor.extractDirectGroupingKeys(entry, dim)) {
                keyToLabel.computeIfAbsent(key, EntityType::toDisplayName);
            }
        }
        List<FacetOption> opts = keyToLabel.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER))
                .map(e -> new FacetOption(e.getKey(), e.getValue(), active.contains(e.getKey())))
                .toList();
        return new FacetContext(opts, Set.copyOf(keyToLabel.keySet()),
                e -> descriptor.extractDirectGroupingKeys(e, dim));
    }

    @SuppressWarnings("unchecked")
    private boolean matchesCriteria(
            final CatalogEntry entry,
            final CatalogCategoryDescriptor<? extends CatalogEntry> rawDescriptor,
            final CatalogFilterCriteria criteria,
            final Map<GroupingDimension, FacetContext> facetContexts) {
        if (!criteria.query().isEmpty()) {
            String lowerName = entry.name() == null ? "" : entry.name().toLowerCase(Locale.ROOT);
            if (!lowerName.contains(criteria.query().toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        if (!criteria.cosms().isEmpty()) {
            boolean anyCosm = entry.cosmSlugs().stream().anyMatch(criteria.cosms()::contains);
            if (!anyCosm) {
                return false;
            }
        }
        CatalogCategoryDescriptor<CatalogEntry> descriptor = (CatalogCategoryDescriptor<CatalogEntry>) rawDescriptor;
        for (Map.Entry<GroupingDimension, Set<String>> requested : criteria.groupings().entrySet()) {
            GroupingDimension dim = requested.getKey();
            Set<String> targetValues = requested.getValue();
            if (targetValues.isEmpty()) {
                continue;
            }
            FacetContext ctx = facetContexts.get(dim);
            Set<String> entryValues = ctx != null
                    ? ctx.entryKeysExtractor().apply(entry)
                    : descriptor.extractDirectGroupingKeys(entry, dim);
            boolean matched = entryValues.stream().anyMatch(targetValues::contains);
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private List<CatalogEntry> sortEntries(
            final List<CatalogEntry> entries,
            final CatalogCategoryDescriptor<? extends CatalogEntry> rawDescriptor,
            final String sortKey,
            final SortDirection direction) {
        CatalogCategoryDescriptor<CatalogEntry> descriptor = (CatalogCategoryDescriptor<CatalogEntry>) rawDescriptor;
        ColumnDescriptor<CatalogEntry> sortColumn = descriptor.columns().stream()
                .filter(ColumnDescriptor::sortable)
                .filter(col -> col.key().equalsIgnoreCase(sortKey))
                .findFirst()
                .orElseGet(() -> descriptor.columns().getFirst());

        Comparator<CatalogEntry> comparator = Comparator
                .comparing((CatalogEntry e) -> nullSafeSortValue(sortColumn.textExtractor().apply(e)),
                        CatalogBrowseService::compareNatural)
                .thenComparing(e -> e.name() == null ? "" : e.name(), String.CASE_INSENSITIVE_ORDER);

        if (direction == SortDirection.DESC) {
            comparator = comparator.reversed();
        }
        return entries.stream().sorted(comparator).toList();
    }

    private static String nullSafeSortValue(final String value) {
        return value == null ? "" : value.trim();
    }

    private static int compareNatural(final String a, final String b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 0;
        }
        if (a.isEmpty()) {
            return 1;
        }
        if (b.isEmpty()) {
            return -1;
        }
        return a.compareToIgnoreCase(b);
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<LinkedEntryRef>> resolveEmbeddedReferences(
            final CatalogCategoryDescriptor<? extends CatalogEntry> rawDescriptor,
            final CatalogEntry entry) {
        CatalogCategoryDescriptor<CatalogEntry> descriptor = (CatalogCategoryDescriptor<CatalogEntry>) rawDescriptor;
        Map<String, List<LinkedEntryRef>> result = new LinkedHashMap<>();
        for (DetailSectionDescriptor<CatalogEntry> section : descriptor.detailSections()) {
            if (section instanceof EmbeddedReferences<CatalogEntry> embedded) {
                List<UUID> targetIds = embedded.idsExtractor().apply(entry);
                if (targetIds == null || targetIds.isEmpty()) {
                    continue;
                }
                AreaSource targetSource = sources.get(embedded.targetArea());
                if (targetSource == null) {
                    continue;
                }
                List<? extends CatalogEntry> accessibleTargets = targetSource.peekAll();
                Map<UUID, String> targetSlugs = CatalogSlugs.resolveCanonicalSlugs(
                        accessibleTargets, CatalogEntry::id, CatalogEntry::slug);
                Map<UUID, CatalogEntry> byId = new HashMap<>();
                for (CatalogEntry candidate : accessibleTargets) {
                    byId.put(candidate.id(), candidate);
                }
                List<LinkedEntryRef> refs = new ArrayList<>();
                for (UUID targetId : targetIds) {
                    CatalogEntry resolved = byId.get(targetId);
                    if (resolved != null) {
                        refs.add(new LinkedEntryRef(
                                resolved.id(),
                                resolved.name(),
                                targetSlugs.getOrDefault(resolved.id(), resolved.slug()),
                                embedded.targetArea()));
                    }
                }
                if (!refs.isEmpty()) {
                    result.put(embedded.title(), List.copyOf(refs));
                }
            }
        }
        return Map.copyOf(result);
    }

    private Map<String, Publication> publicationsByCodexId() {
        AreaSource pubSource = sources.get(CatalogArea.PUBLICATION);
        if (pubSource == null) {
            return Map.of();
        }
        Map<String, Publication> byCodexId = new LinkedHashMap<>();
        for (CatalogEntry entry : pubSource.peekAll()) {
            if (entry instanceof Publication pub && pub.codexId() != null) {
                byCodexId.put(pub.codexId(), pub);
            }
        }
        return byCodexId;
    }

    private List<Publication> resolvePublications(final Set<String> codexIds) {
        if (codexIds == null || codexIds.isEmpty()) {
            return List.of();
        }
        Map<String, Publication> byCodexId = publicationsByCodexId();
        return codexIds.stream()
                .sorted()
                .map(byCodexId::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
