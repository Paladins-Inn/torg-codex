---
description: "Implementation tasks for Publication Content Tiles"
---

# Tasks: Publication Content Tiles

**Input**: [Specification](spec.md), [plan](plan.md), [research](research.md), [data model](data-model.md), [reader contract](contracts/reader-publication-content.md), and [quickstart](quickstart.md).

**Tests**: Test-first is mandatory under the constitution. Run the new assertions before implementing the behavior, then rerun the same selectors after implementation. If an assertion requires a new type to compile, add the minimum type/signature before confirming it fails for missing behavior.

**Format**: `- [ ] Tnnn [P?] [USn?] Description with repository-relative file paths`. `[P]` marks independent work once its named prerequisites are done.

## Phase 1: Setup

- [X] T001 Create and check out branch `009-publication-content-tiles` from `008-publication-thumbnails` (thumbnail work committed as `4089177`). Verify JDK 25, `./mvnw`, a Testcontainers-compatible Docker daemon, and Chrome/Chromium as described in `specs/009-publication-content-tiles/quickstart.md`. Use the existing Maven reactor and dependencies.
- [X] T002 Run the baseline command from `specs/009-publication-content-tiles/quickstart.md` for `groupDetailListsOnlyAccessibleMembersSortedByNameWithCanonicalLinks` in `torg-codex-application/src/test/java/de/paladinsinn/torg/codex/application/service/CatalogBrowseServiceTest.java`. Record the result; do not broaden feature scope to fix an unrelated failure.

## Phase 2: Foundation

- [X] T003 Add public/schema-only fixture and matching cleanup in `torg-codex/src/test/resources/sql/publication-content-tiles-seed.sql` and `torg-codex/src/test/resources/sql/publication-content-tiles-cleanup.sql`. Use one fixture ID prefix for every row and fixture-unique publication Codex IDs (for example `pct-paid-a`, `pct-paid-b`); give one paid publication a storefront product ID and the other none. Seed a paid-only spell, a spell shared between the two paid publications, free-tier entries in at least two categories assigned to `core-rulebook`, an unrelated entry, and an unassigned spell. Provide a category with zero matches and a known publication with no accessible content for the tested reader. Insert a `core-rulebook` publication row only if none exists (`INSERT … WHERE NOT EXISTS`), with a fixture-prefixed ID. The cleanup deletes by the fixture ID prefix only, so pre-existing rows are never touched.

## Phase 3: User Story 1 - See what a publication contains (P1, MVP)

**Goal**: Show publication tiles only for categories with positive accessible counts; omit the entire grid if no tile remains. Keep a signed-out reader's tiles readable and noninteractive.

**Independent test**: Visit the same seeded publication under anonymous, signed-in non-owner, other-publication owner, and displayed-publication owner states. Compare which tiles appear and their counts. Verify anonymous free-tier tiles have no destination and all-zero detail pages retain the existing publication details.

### Tests first

- [X] T004 [P] [US1] Add application tests in `torg-codex-application/src/test/java/de/paladinsinn/torg/codex/application/service/CatalogBrowseServiceTest.java` for one ordered count per product-bound area, UUID deduplication, exact assignment-key matching, shared and unassigned entries, and all-zero/no-key counts. Assert no count reads on non-publication or missing details, at most one top-level restricted peek per eligible area, and no synthetic category visits.
- [X] T005 [P] [US1] Add the publication detail contract tests in `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/codex/PublicationContentTilesTest.java` using real Spring Security authorities, MockMvc, PostgreSQL Testcontainers, and class-level seed/cleanup. Verify the differential count and visibility across reader states, the "Content in this publication" heading, positive-count labels/icons and homepage-relative order, reference category exclusion, anonymous markup with no destination or link semantics, zero-tile/all-zero grid omission, slug and UUID routes, and normal missing-detail behavior. Assert exact counts only on the fixture-unique publications; for the free-tier publication assert only a tile with a positive count and no link, because other data in the shared database can change its totals.
- [X] T006 [P] [US1] Add a characterization test in `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/codex/HomeCategoryCardsTest.java` asserting the homepage category cards' order, labels, icons, links and counts (including zero-count cards) for anonymous and signed-in readers. It must pass before and after the fragment extraction in T009.

### Implementation

- [X] T007 [US1] Extend the existing public detail result in `torg-codex-application/src/main/java/de/paladinsinn/torg/codex/application/port/in/CatalogDetailResult.java` with immutable ordered area/count values; preserve existing reference-detail fields and callers.
- [X] T008 [US1] Implement the single publication membership rule and publication-only count projection in `torg-codex-application/src/main/java/de/paladinsinn/torg/codex/application/service/CatalogBrowseService.java`. Match nonblank Codex assignment keys exactly against already-granted product-bound entries, count distinct UUIDs, and use only restricted area peeks. Preserve existing reference and usage paths; make the predicate reusable by US2 without introducing an ownership check.
- [X] T009 [P] [US1] Extract shared Bootstrap category card rendering into `torg-codex/src/main/jte/codex/fragments/categoryTile.jte`, using it from `torg-codex/src/main/jte/codex/pages/home.jte`. Depends on T006 passing unchanged. Preserve homepage card markup, links, presentation, counts (including existing zero counts), and the publication thumbnail code.
- [X] T010 [P] [US1] Add publication tile data to `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/model/DetailViewModel.java` and map it in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/mapper/CodexViewMapper.java`; project only positive counts in input order with existing `CatalogArea` labels/icons. Until US2 supplies valid destinations, render these tiles as informational cards.
- [X] T011 [US1] Render the responsive content grid under the heading "Content in this publication" through the shared fragment in `torg-codex/src/main/jte/codex/pages/detail.jte`. Omit the full section when no tile remains, keep existing detail content and non-publication behavior, and exclude publication detail output from htmx history snapshots.
- [X] T012 [US1] Run the User Story 1 checkpoint selector from `specs/009-publication-content-tiles/quickstart.md`; verify positive tiles, signed-out markup, all-zero omission, and the unchanged homepage cards before starting navigation work.

## Phase 4: User Story 2 - Browse a publication by content type (P2)

**Goal**: Make each visible signed-in tile a named link to the ordinary category list filtered to its publication. Preserve selection through navigation and support clearing it, without granting access.

**Independent test**: Follow a signed-in Spells tile and check the named filter and exact results. Sort, combine filters, reload, and clear the publication; then open a known-empty and an unknown publication URL directly to verify both empty states. Reopen the original URL anonymously and compare the results.

### Tests first

- [X] T013 [P] [US2] Add list-service tests in `torg-codex-application/src/test/java/de/paladinsinn/torg/codex/application/service/CatalogBrowseServiceTest.java` for known/omitted/unavailable selections; exact assigned/granted matches and UUID deduplication; scoped totals; combined text, cosm/grouping and sort criteria; unchanged canonical links/facets; and at most one no-usage metadata peek per list.
- [X] T014 [P] [US2] Add full-page and `HX-Request` route contract tests in `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/codex/CodexCategoryBrowseControllerTest.java`. Cover named and unavailable selections, known empty lists, unknown/blank/malformed/repeated values with no unfiltered fallback, safe parameter forwarding, query-preserving aliases, reference-route exclusion, sort/form persistence, clear versus reset, and response cache/fragment headers.
- [X] T015 [P] [US2] Extend `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/codex/PublicationContentTilesTest.java` to follow the rendered tile URLs and prove count-to-list agreement from the same fixtures, including a reader who can see a shared entry through another publication. Revisit the same URL with weaker grants and assert the rendered rows differ and inaccessible entries never appear.
- [X] T016 [P] [US2] Add a real Chrome/Chromium test in `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/codex/PublicationContentTilesBrowserTest.java` using the existing browser-test approach, actual JTE pages and htmx assets, and seeded test sessions. Check named/focus-visible keyboard links while signed in, no Tab stop or activation while signed out, reload and Back/Forward through publication clear, no filtered history snapshot reuse, correct state after clear/reset and resubmission, and no selection or URL change after a failed partial request.

### Implementation

- [X] T017 [US2] Add typed publication request state (omitted, UUID, invalid) and resolved selection (none, selected, unavailable) in `torg-codex-application/src/main/java/de/paladinsinn/torg/codex/application/port/in/CatalogFilterCriteria.java` and `torg-codex-application/src/main/java/de/paladinsinn/torg/codex/application/port/in/CatalogBrowseResult.java`. Preserve no-filter defaults and keep membership keys out of the web-facing projection.
- [X] T018 [P] [US2] Implement publication selection and filtering in `torg-codex-application/src/main/java/de/paladinsinn/torg/codex/application/service/CatalogBrowseService.java`. Resolve a UUID through one public publication peek; use the US1 membership rule on granted entries; deduplicate rows and scope the denominator before other filters. Zero matches for unavailable selections, unchanged category metrics, and unchanged facet/canonical-link context are required.
- [X] T019 [P] [US2] Bind the single hyphenated UUID query parameter in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/CodexCatalogController.java` only for product-bound areas. Distinguish omission from blank, malformed, or repeated values; preserve canonical alias redirection, existing headers, and full-page routing for history restoration.
- [X] T020 [US2] Carry the applied selection's label, safe token, and clear destination through `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/model/FilterPanelViewModel.java`, `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/model/TableViewModel.java`, and `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/mapper/CodexViewMapper.java`. Serialize the token in sort links and build a clear URL that removes only publication and preserves the latest applied search, facets, and sort.
- [X] T021 [P] [US2] Render the active publication name or unavailable status, hidden GET form token, and clear/reset actions in `torg-codex/src/main/jte/codex/fragments/categoryResults.jte`, `torg-codex/src/main/jte/codex/fragments/filterPanel.jte`, and `torg-codex/src/main/jte/codex/pages/category.jte`. Use full page clears and resets when a selection is active; prevent local htmx history snapshots of filtered pages; keep ordinary results-only search, facet, and sort swaps working.
- [X] T022 [P] [US2] Give visible publication tiles canonical, URL-encoded category destinations with the resolved publication UUID only for the existing signed-in reader state in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/mapper/CodexViewMapper.java`, `torg-codex/src/main/jte/codex/pages/detail.jte`, and `torg-codex/src/main/jte/codex/fragments/categoryTile.jte`. Use one named, keyboard-focusable native link per signed-in tile; keep signed-out tiles free of links, link roles, focus, and activation handlers.
- [X] T023 [US2] Run the new US2 service, controller, integration and browser assertions using `specs/009-publication-content-tiles/quickstart.md`. Confirm the real browser run was not skipped and verify that the US1 detail assertions still pass.

## Phase 5: User Story 3 - Recognize a publication by its cover (P3)

**Goal**: Show the homepage publication thumbnail, with the same JPEG, PNG and placeholder fallback, in the header of every publication detail page.

**Independent test**: Open the detail pages of the fixture publication with a storefront product ID and of the one without, signed out and signed in. Compare the thumbnail markup and fallback behavior with the homepage card of the same publication; confirm other detail types show no thumbnail.

### Tests first

- [X] T024 [P] [US3] Add MockMvc tests in `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/codex/PublicationDetailThumbnailTest.java` with the class-level fixture from T003. Verify the JPEG `src`, PNG `data-fallback-src`, `alt=""` and no link for the publication with a product ID; the placeholder without an image element for the one without; identical output signed out and signed in and when no content tile is shown; the script `/js/publication-thumbnails.js` on publication details only; and no thumbnail on a non-publication detail page.
- [X] T025 [P] [US3] Add a publication-detail case to `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/codex/PublicationThumbnailsBrowserTest.java` using its existing local image stubs: JPEG loads, JPEG fails and PNG loads, both fail and the placeholder shows. Assert no broken image and no external image request.

### Implementation

- [X] T026 [US3] Promote the nested thumbnail record from `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/model/HomeViewModel.java` to `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/model/PublicationThumbnailViewModel.java`, and extract the thumbnail frame from `torg-codex/src/main/jte/codex/pages/home.jte` into `torg-codex/src/main/jte/codex/fragments/publicationThumbnail.jte`. The existing `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/codex/HomePublicationThumbnailsTest.java` must pass unchanged in behavior before and after.
- [X] T027 [US3] Add a nullable thumbnail to `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/model/DetailViewModel.java` and map it for publication details only in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/mapper/CodexViewMapper.java` from the resolved `Publication`, reusing the homepage mapping.
- [X] T028 [US3] Render the shared thumbnail fragment in the header of `torg-codex/src/main/jte/codex/pages/detail.jte` for publication details and load `torg-codex/src/main/resources/static/js/publication-thumbnails.js` there with `defer`. Adjust the header layout in `torg-codex/src/main/resources/static/themes/codex/codex.css` only as needed, reusing the existing thumbnail classes.
- [X] T029 [US3] Run `PublicationDetailThumbnailTest`, `HomePublicationThumbnailsTest`, `PublicationThumbnailsBrowserTest` and `HomeCategoryCardsTest` using `specs/009-publication-content-tiles/quickstart.md`; confirm the browser test was not skipped.

## Phase 6: Polish and cross-cutting validation

- [X] T030 [P] Update `docs/modules/arc42/pages/08_concepts/jte-with-markdown.adoc` to document positive-count publication tiles, noninteractive signed-out tiles, publication-filtered GET navigation, the thumbnail on publication details, and continued use of the existing DRM path. Commit the UML diagrams regenerated by the build, such as `docs/modules/arc42/images/uml/classes/torg-codex.puml`.
- [X] T031 [P] Run the full feature and regression selector in `specs/009-publication-content-tiles/quickstart.md`. Verify the homepage and reference details, unfiltered list behavior, access/teaser outcomes, REST contract, restricted-query boundary and the feature 008 thumbnail tests.
- [X] T032 [P] Execute the read-budget check and the informational timing comparison in `specs/009-publication-content-tiles/quickstart.md`; record the median publication-detail and homepage render times and investigate only if the detail page exceeds 1.5× the homepage. Do not equate top-level peeks with individual SQL statements or add an alternate access path.
- [ ] T033 Manual, outside the implementation run: perform the first-attempt usability scenario in `specs/009-publication-content-tiles/quickstart.md` with five representative signed-in readers and a populated category. Record whether at least four succeed unaided; leave this task pending if representative readers are unavailable.
- [X] T034 Run `./mvnw clean verify` from the root `pom.xml` as specified in `specs/009-publication-content-tiles/quickstart.md`. Report any baseline and feature failures separately; the full reactor must pass before merge.

## Dependencies and execution order

- **Setup and foundation**: T001 → T002 → T003. Complete T003 before starting story work.
- **US1**: T004–T006 are independent test tasks. After the test-first step, T007 → T008; T009 needs T006 and can run alongside application work, and T010 can run alongside T009 after T008. T011 needs T009 and T010. T012 is the overview checkpoint.
- **US2**: Start after the US1 checkpoint. T013–T016 can be prepared in parallel. After the test-first step, T017 unblocks T018 and T019 in separate files. T020 follows T018 and T019. T021 and T022 can then run in parallel; T023 needs both.
- **US3**: Independent of the tiles and the filter, but it edits `home.jte`, `detail.jte`, `DetailViewModel.java` and `CodexViewMapper.java`, so run it after US2 to avoid conflicting edits. T024 and T025 in parallel, then T026 → T027 → T028 → T029.
- **Polish**: T030–T032 can proceed after all stories. Avoid concurrent Maven invocations sharing the same checkout/build outputs; batch selectors or use separate worktrees. Run T034 after code and regression work has settled. T033 is manual; if it stays pending, SC-004 is unverified but the build gate is unaffected.

## Parallel examples

- **US1**: One contributor adds the application count tests (T004) while others add the publication-detail contract tests (T005) and the homepage characterization test (T006). After the test-first step, the homepage card extraction (T009) is independent of the application projection (T007–T008).
- **US2**: Separate test authors can cover controller contracts (T014) and actual browser navigation (T016). After selection types are in place, one contributor implements the application filter (T018) while another binds the reader query parameter (T019).
- **US3**: The MockMvc tests (T024) and the browser case (T025) are in different files and can be written together.

## Implementation strategy

**Checkpoint (US1)**: Positive counts only, no empty grid, and readable noninteractive cards. US1 is an internal checkpoint; FR-004 is satisfied only after US2, so release both together. Do not ship a clickable tile that leads to an unfiltered list.

**Full feature**: Deliver US2 next, reusing the US1 membership rule and presentation, then US3 for the thumbnail. Finish the regression and full-reactor gates and report the usability result separately from automated verification.
