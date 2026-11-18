# Implementation Plan: Publication Content Tiles

**Feature**: `009-publication-content-tiles` | **Date**: 2026-11-18 | **Spec**: [spec.md](spec.md)

**Implementation base**: branch `009-publication-content-tiles`, created from `008-publication-thumbnails` after its thumbnail work was committed (`4089177`).

## Summary

Show homepage-style tiles only for product-bound categories with at least one entry from the publication that the reader can access. Compute distinct counts from the caller's granted entries using the same membership rule as category-list filtering. If every count is zero, omit the grid. Only signed-in readers receive tile links. Links enter the existing list route with a publication UUID, and the server maintains the labelled selection across search, facets, sorting, reloads, and htmx navigation. Publication detail pages also show the homepage thumbnail with its existing fallback. No public API, schema, or ownership changes.

## Technical Context

- **Language and build**: JDK 25; four-module Maven reactor, Maven wrapper.
- **Dependencies**: Existing Spring Boot MVC/Security, JTE, Bootstrap 5 and Bootstrap Icons, and htmx. No new dependencies.
- **Storage**: Existing PostgreSQL publication rows and multi-valued product assignments, accessed through current persistence adapters and application ports. No Liquibase changes.
- **Platform**: Server-rendered reader web application; native GET links and existing htmx enhancements, with keyboard, touch, and pointer support.
- **Testing**: JUnit 5, AssertJ, Mockito, Spring MockMvc, PostgreSQL Testcontainers, existing architecture/REST contract suites and headless browser tooling.
- **Read budget**: At most one top-level restricted peek per eligible area on publication details; no per-tile browse or markup rendering. A filtered category adds at most one public publication metadata peek. No browser fetches for tile data and no cross-request count cache. Confirm call counts. The timing comparison with the homepage read path is informational: record both timings and investigate only if the median publication-detail render exceeds 1.5× the homepage render on the same data.
- **Scope**: Publication detail and the existing 14 product-bound category lists only. The homepage, reference lists, entry detail/teaser rendering, navigation, auth, and REST endpoints retain their behavior.

## Constitution Check

| Gate | Decision |
|---|---|
| I. Hexagonal boundaries | Keep count and membership logic behind `CatalogBrowseQuery`, using existing area sources and granted domain entries. The web adapter binds URLs and composes HTML view models only; no new persistence or REST DTO mapping. |
| II. Self-contained systems | Server-rendered composition through the existing JTE stack; no new external connections, events, or services. |
| III. REST/OpenAPI | Existing HTML routes only. Keep the REST contract and controllers unchanged; run the existing REST contract regression test. |
| IV. Database evolution | No schema, import, or migration changes. |
| V. Security and DRM | Retain ADR-019 and the single `ROLE_<codex-id>` mechanism. Signed-in presentation controls link presence only. Prove rendered count and row differences between real ownership states. Keep caller-dependent results out of shared caches. |
| VI. Markup and template safety | Preserve the censor/rendering pipeline. Escape labels and generate URLs through existing adapter conventions; never render raw invalid query values. |
| VII. Observability | Preserve existing structured logging, browse/detail metrics and access anomaly signals. Peeks do not record synthetic category or detail visits. |
| VIII. Test-first and verification | Add failing tests before implementation, use isolated public/schema-only fixtures, run focused suites and full `./mvnw clean verify` before merge, and sign off any commits. |

**Post-design decision**: Pass. The design uses accepted ADRs and does not change module boundaries or add a new architectural mechanism. Document the shipped reader behavior in the arc42 JTE/reader UI concept. No exceptions or complexity waivers are needed. Build verification is a future implementation gate, not evidence from this planning phase.

## Design and verification

1. **Application read model (FR-001–002, FR-005–006)**: Augment the public publication detail result with category counts and browse criteria/results with a typed publication selection. Apply one membership rule to granted entries; canonical detail links and existing facet behavior retain their full granted-category context. Distinguish no filter, known publication, and unavailable selection.
2. **Reader rendering and routes (FR-001, FR-003–005, FR-007)**: Share the category tile presentation without changing the homepage or its publication card logic. Map only positive counts to publication tiles, omit the grid if none remain, and render native named links only when signed in. Extend the category form, result status, and URL builder; use full-page navigation for removing the publication so stale form state cannot reapply it.
3. **Acceptance and regressions (SC-001–004)**: Cover tile omission for zero counts, the all-zero detail page, shared entries, four ownership states, and direct list requests including known empty filters. Verify keyboard activation, htmx/history, invalid filter recovery, and unchanged homepage, unfiltered list, teaser, and REST behavior. Record the first-attempt usability result separately from automated tests. See [quickstart.md](quickstart.md).
4. **Publication thumbnail (FR-008, SC-005)**: Share the homepage thumbnail markup as a JTE fragment and reuse the existing thumbnail value, CSS classes, and deferred fallback script on publication detail pages only. The web mapper derives the URLs from the already resolved `Publication`; no application or domain change. Guard the homepage with characterization tests before each fragment extraction.

**Baseline validation risk**: The prior full reactor run on the current checkout failed in `CatalogBrowseServiceTest.groupDetailListsOnlyAccessibleMembersSortedByNameWithCanonicalLinks` on a slug assertion. Establish the baseline on the intended implementation base before attributing failures to this feature. This plan neither changes that assertion nor waives the full-reactor merge gate.

## Project Structure

### Documentation (this feature)

```text
specs/009-publication-content-tiles/
  spec.md
  plan.md
  research.md
  data-model.md
  quickstart.md
  contracts/reader-publication-content.md
```

### Source Code (planned scope)

```text
torg-codex-application/
  src/main/java/de/paladinsinn/torg/codex/application/port/in/
  src/main/java/de/paladinsinn/torg/codex/application/service/
  src/test/java/de/paladinsinn/torg/codex/application/service/
torg-codex/
  src/main/java/de/paladinsinn/torg/codex/web/codex/
  src/main/jte/codex/{pages,fragments}/
  src/main/resources/static/{js,themes/codex}/
  src/test/java/de/paladinsinn/torg/codex/web/codex/
  src/test/resources/sql/
docs/modules/arc42/pages/08_concepts/jte-with-markdown.adoc
docs/modules/arc42/images/uml/classes/   (regenerated by the build)
```

**Structure decision**: Extend existing application read models and `CatalogBrowseService`, plus `CodexCatalogController`, `CodexViewMapper`, and the reader JTE detail/category/card fragments. Domain models and data adapters are reused unchanged. Task sequencing belongs to `/skill:speckit-tasks`.
