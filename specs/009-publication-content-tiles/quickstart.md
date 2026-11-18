# Quickstart: Verify Publication Content Tiles

Run these checks once the feature is implemented. Use JDK 25, the Maven wrapper, a running Testcontainers-compatible Docker daemon, and Chrome/Chromium for browser behavior. Run from the repository root. Use the public or schema-only fixtures with isolated class-level SQL seed and cleanup; no live DriveThruRPG credentials, proprietary fixtures, or external image requests are needed.

## Baseline and automated checks

Before feature edits, check the existing canonical link assertion on the intended implementation base. Record the outcome separately from feature failures:

```sh
./mvnw -pl torg-codex-application -am \
  '-Dtest=CatalogBrowseServiceTest#groupDetailListsOnlyAccessibleMembersSortedByNameWithCanonicalLinks' \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Extend the existing application browse suite and add reader integration and browser coverage. Planned test selectors after implementing those tests.

User Story 1 checkpoint:

```sh
./mvnw -pl torg-codex -am \
  -Dtest='CatalogBrowseServiceTest,PublicationContentTilesTest,HomeCategoryCardsTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Full feature and regression:

```sh
./mvnw -pl torg-codex -am \
  -Dtest='CatalogBrowseServiceTest,PublicationContentTilesTest,PublicationContentTilesBrowserTest,HomeCategoryCardsTest,PublicationDetailThumbnailTest,HomePublicationThumbnailsTest,PublicationThumbnailsBrowserTest,PublicationThumbnailTest,CodexCategoryBrowseControllerTest,CodexEntryDetailControllerTest,CodexWebUiDifferentialTest,CatalogDrmDifferentialTest,CatalogOpenApiContractTest,CatalogDrmArchitectureTest*' \
  -Dsurefire.failIfNoSpecifiedTests=false test

./mvnw clean verify
```

Confirm the planned tests actually ran; a skipped browser test does not prove keyboard or htmx history behavior. Assert one top-level peek per eligible area on valid publication details, zero synthetic category visits, and at most one metadata lookup per filtered list. The timing comparison is informational: record the median publication-detail and homepage render times on the same data, and investigate only if the detail page exceeds 1.5× the homepage.

## End-to-end acceptance

The fixture uses publications with fixture-unique Codex IDs; exact counts are asserted only on those. One paid fixture publication has a storefront product ID and one has none. In the fixture, assign one Spell only to a paid publication, one Spell to both that publication and a second paid publication, and one Spell only to the free-tier publication. Include an unassigned Spell and an unrelated item or spell. Pick a category with zero matching entries for the paid title. Use the same publication and the same filtered URL across reader states.

| Check | Expected observation |
|---|---|
| View the paid publication signed out, signed in with no related product, as the owner of the other publication, and as the owner of the displayed one | The Spells tile is absent in the first two states and shows `1` and `2` in the last two. When the tile is absent and every other category count is also zero, the entire grid is absent but the publication details remain. Each displayed tile agrees with its destination list. The shared spell counts once on each applicable publication. |
| View the free-tier publication signed out | Its free-tier Spell yields a Spells tile with a positive count (the exact number depends on other loaded data). Only positive-count tiles appear, in homepage relative order. They are readable and absent from the Tab sequence; clicking/tapping them does not navigate. |
| Activate all visible signed-in tile destinations; use keyboard on Spells | Each destination has the right list path and UUID, reachable in one activation, and shows the named publication. Categories with no accessible content have no tile even while signed in. |
| Open the paid list URL directly in a fresh anonymous session | The page shows the normal empty state with the known publication still named. No login bypass or locked rows; current grants are still used. |
| Apply search and a supported facet, sort, reload, and navigate Back/Forward including through a publication-clear transition | Publication selection, URL, and rows agree on full and partial renders. Restoring a filtered URL does not reuse an old caller's cached fragment. A failed partial response does not switch the selection. |
| Clear publication after sorting with other filters active; apply another form change | Only publication is removed, applied search/facets/sort are retained in the clear destination, and the next form change does not resurrect publication. Reset all goes to the bare category URL. |
| Browse an unknown UUID, malformed input, a blank value, and repeated values | No matches, unavailable status, and recovery by clearing. No raw malformed input is rendered. |
| Open the detail pages of the publication with and without a storefront image, signed out and signed in; block the JPEG, then both image URLs | The thumbnail matches the homepage card for the same publication. The PNG replaces a failed JPEG; the placeholder appears when both fail or no image exists. No broken image, no thumbnail on other detail types. |
| Revisit homepage, unfiltered lists, reference details, inaccessible and missing entry detail, and REST responses | Existing output and access outcomes are unchanged except for the new grid on publication details. Verify both slug and UUID detail routes. |

See the [reader contract](contracts/reader-publication-content.md) for request rules and the [data model](data-model.md) for membership semantics.

## First-attempt usability check (SC-004)

Using a representative seeded app with signed-in reader sessions, ask five representative readers to find a named content type for a given publication and open the list for that publication. Choose a type with accessible entries for each participant. Do not name the control or assist. Record whether each participant reaches the correctly prefiltered list on their first attempt. The criterion passes at four of five or better; it is separate from route and browser automation.
