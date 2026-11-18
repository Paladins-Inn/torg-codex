# Research: Publication Content Tiles

## R1. Membership is a filter, not an access rule

**Decision**: Run a single application-layer publication membership rule over entries already granted by the existing area sources. An entry belongs to a publication when its assigned `productCodexIds()` contain that publication's nonblank assignment key. Compare the stored keys exactly; do not compare names, slugs, or storefront IDs. Use entry UUIDs for distinct counts and deduplicate matching rows in a publication-scoped list. Apply the rule to all product-bound `CatalogArea` values in their existing declaration order.

**Rationale**: `CatalogBrowseService.AreaSource.restricted` delegates to `RestrictedCatalogQuery`. Its `peekAll` and `findAll` share the same access policy; `findAll` also records a category visit. The policy grants an entry when *any* of its assigned products is accessible. Requiring ownership of the displayed publication instead would drop shared entries that the reader already has access to. `CodexOverviewService` already uses `peekAll` and enum order for homepage counts. Article, Publication, and Tag are public reference areas and are not publication content.

**Alternatives considered**: Unrestricted counts would disclose locked content; counting only titles owned by the reader would violate shared-entry behavior. Repeating the membership predicate in a web mapper would separate tile and list semantics and move catalog logic out of the application layer.

## R2. Keep URL identity separate from the assignment key

**Decision**: Use one `publication=<uuid>` parameter on existing product-bound list routes. The reader web adapter distinguishes omitted, a single valid UUID, and malformed, blank, or repeated values. The application resolves a valid UUID once through the existing public Publication area source's `peekAll`, then uses the resolved publication's name and assignment key. A missing UUID or invalid input yields an active unavailable selection with zero matches and a clear action. Never echo malformed text; carry it forward as `publication=`. Ignore this parameter on reference lists.

**Rationale**: `Publication.slug()` normalizes the Codex ID and can fall back to name; detail routing also supports collision-safe slugs and UUIDs. The resolved publication UUID is unambiguous and works regardless of the path used to reach the detail. It is not the value used for membership. Reusing the metadata peek avoids emitting publication detail-visit or not-found events during sort and filter actions; `CatalogQueryService.findById` currently emits those events.

**Alternatives considered**: A slug requires canonicalization and collision resolution; a bare Codex ID couples the route to the assignment/ownership key. Treating invalid values as omitted would broaden the results. Adding a dedicated no-usage metadata lookup port is unnecessary for this delivery.

## R3. Reuse SSR and keep the GET URL authoritative

**Decision**: Extract the homepage category card/grid presentation into reusable JTE markup without changing homepage output or thumbnail handling. On publication details, omit zero-count tiles for every reader and omit the grid when no tiles remain. Give each displayed tile a server-generated list URL only when the existing reader sign-in signal is true. Render signed-out tiles as plain cards with no interactive semantics. In filtered lists, render the publication status and a dedicated clear link in `#catalog-results`; include the selection token in the existing GET form and in server-generated sort links. Publication removal and reset-all use full-page GETs while a publication selection is active. Retain the current full-page and results-only htmx paths for all other actions.

**Rationale**: `category.jte` renders the filter panel outside `#catalog-results`. Retaining a fixed hidden value is sufficient while search, facets, and sort stay within the same publication, but an in-place clear would leave a stale value in the form. A full clear rebuilds both parts; a dedicated clear preserves the latest applied query, facets, and sort. Put `hx-history="false"` on publication details and publication-filtered full pages to keep caller-dependent output out of htmx's client-side history cache. A cache-miss restore must re-enter the full page route, not restore a fragment or reuse the prior reader's filtered result; preserve `private, no-store`.

**Alternatives considered**: An all-publication picker adds an unrelated browsing flow. Out-of-band updates or whole-panel swaps require additional synchronization when clearing state. Client-side filtering or hiding would neither enforce access nor provide a durable URL.

## R4. Bound the new reads and preserve existing browse semantics

**Decision**: A valid publication detail issues at most one top-level restricted `peekAll()` per eligible area, reusing the resolved detail publication. Count in one pass per granted list, without sorting, canonicalizing content slugs, building facets, or rendering entry markup. A filtered category request reuses the ordinary granted category load and adds at most one public publication metadata peek. Build existing facet context and canonical item links from the same full granted category data as today, then narrow the displayed rows and the total-before-other-filters to the active publication. No migration, additional HTTP call, or shared cache.

**Rationale**: The repository's JPA adapters map existing `findAll()` results to domain values; this design is a bounded set of port calls, not a promise of one SQL statement per area. Restricted queries for reference-bearing entries can already perform extra lookups. Publication details gain homepage-scale read work; avoid multiplying it by using `browse()` once per tile. Preserve existing browse/detail metrics and canonical href behavior even when publication membership hides a colliding row.

**Alternatives considered**: Running the full list path for every tile adds sorting, facets, and synthetic category visits. SQL-side aggregation would need a new restricted read design and verification of identical access semantics. Revisit only if the median publication-detail render exceeds 1.5× the homepage render on the same data; do not introduce a parallel access path.

## R5. Reuse the homepage thumbnail on publication details

**Decision**: Extract the homepage thumbnail frame (image, `data-fallback-src`, placeholder) into a shared JTE fragment and render it in the publication detail header. Promote the nested `HomeViewModel.PublicationThumbnail` record to a shared web view-model type and map it from `Publication.thumbnailUrl()` and `thumbnailFallbackUrl()`. Load `/js/publication-thumbnails.js` on publication details only. The image is decorative (`alt=""`) because the page heading already names the publication.

**Rationale**: Feature 008 already provides the JPEG-then-PNG-then-placeholder behavior, scoped CSS, and a script that initializes every `.publication-thumbnail` frame on the page. The detail mapper already holds the resolved `Publication`, so no new read or port is needed.

**Alternatives considered**: The full-size cover would add a second image source and layout work. Duplicating the markup would let homepage and detail fallbacks drift apart.
