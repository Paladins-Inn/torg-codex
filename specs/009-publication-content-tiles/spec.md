# Feature Specification: Publication Content Tiles

**Feature Branch**: Not created by this command

**Created**: 2026-11-18

**Status**: Draft

**Input**: User description: "on publication detail pages show tiles with the different type of content like on the main page with the number of content entries for that publication. When a tile is clicked open the list page of that content type with a filter set to that publication.When not logged in the tile is not clickable."

## Clarifications

### Session 2026-11-18

- Q: Should a publication detail page show a category tile when its count is zero? → A: No. Omit the tile for both signed-in and signed-out readers.
- Q: Should the publication detail page show the publication's thumbnail? → A: Yes. Show the same thumbnail as on the homepage, with the same fallback, for every reader.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See what a publication contains (Priority: P1)

A reader opens a publication detail page and sees familiar tiles for categories with entries from that publication they can access. Each tile shows how many such entries there are. A signed-out reader can read the visible labels and counts but cannot use the tiles as links.

**Why this priority**: The overview lets readers understand what they can browse from a publication without opening each category separately.

**Independent Test**: Open the same publication signed out, signed in without access to it, and signed in with access. Compare which tiles appear, their counts, and link availability against a known set of publication assignments and access rights.

**Acceptance Scenarios**:

1. **Given** a publication with accessible entries in some categories and none in others, **When** a reader opens its detail page, **Then** the page shows only categories with a positive count, in the homepage's relative order, with the publication-specific count on each tile.
2. **Given** a signed-out reader viewing the free-tier publication, **When** they view its tiles, **Then** categories with accessible free-tier entries show their counts without links or keyboard activation, and categories with zero accessible entries have no tile.
3. **Given** two signed-in readers with different access rights to entries assigned to the same publication, **When** each opens its detail page, **Then** each sees only the counts of entries they can access; an entry available through another publication still counts if it is also assigned to the displayed publication.
4. **Given** a reader with no accessible entries in any category for a known publication, **When** they open its detail page, **Then** the existing publication details still appear but the tile grid is absent.

### User Story 2 - Browse a publication by content type (Priority: P2)

A signed-in reader activates a tile and lands on the existing list page for that content type, with the current publication already selected as a filter.

**Why this priority**: Readers can go from a publication to the entries they want to explore in one action without finding the category or setting the filter themselves.

**Independent Test**: While signed in, activate a visible tile and verify the destination category, selected publication, and exact result set. Open a publication-filtered list directly for a category with no accessible results to verify the empty state.

**Acceptance Scenarios**:

1. **Given** a signed-in reader on a publication with accessible spells, **When** they activate the Spells tile by pointer or keyboard, **Then** the Spells list opens with the publication named as an active filter and shows only accessible spells assigned to it.
2. **Given** a known publication with no spells accessible to the reader, **When** the reader opens a Spells list address filtered to that publication directly, **Then** the list shows its normal empty state with the publication still named as the active filter.
3. **Given** a reader on a publication-filtered list, **When** they reload the page, change the sort order, or apply another supported filter, **Then** the publication remains selected and continues to constrain the results until they clear it.
4. **Given** a saved publication-filtered list address, **When** it is opened by a reader with less access than the original reader, **Then** the list shows only entries the current reader can access and reveals no restricted entries.

### User Story 3 - Recognize a publication by its cover (Priority: P3)

A reader opens a publication detail page and sees the same cover thumbnail that represents the publication on the homepage.

**Why this priority**: The cover makes the page recognizable at a glance, but the page is usable without it.

**Independent Test**: Open the detail page of a publication with a cover image and of one without, signed out and signed in. Compare the thumbnail and its fallback with the same publications on the homepage.

**Acceptance Scenarios**:

1. **Given** a publication whose cover thumbnail is available, **When** any reader opens its detail page, **Then** the page shows that thumbnail, regardless of sign-in state, ownership, or whether any content tile is shown.
2. **Given** a publication whose thumbnail cannot be loaded or that has no cover image, **When** a reader opens its detail page, **Then** the page shows the same fallback as the homepage and no broken image.
3. **Given** a detail page of any other content type, **When** a reader opens it, **Then** no publication thumbnail is added.

### Edge Cases

- An entry assigned to multiple publications contributes once to its category count on each applicable publication page. Unassigned entries and entries assigned only to other publications are excluded.
- A known publication with no accessible entries shows its details without a tile grid; it is not treated as missing. A missing publication detail page retains the existing not-found behavior.
- An unknown publication in a filtered list must not be treated as an unfiltered request. The reader sees no matches and can clear the invalid selection.
- A publication without a cover image, or whose image fails to load, shows the homepage's thumbnail fallback on its detail page.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Reader-facing publication detail pages MUST show a homepage-style grid for product-bound categories with a positive count, using the homepage labels, icons, and relative order. Zero-count categories MUST NOT have tiles for any reader. If no category has a positive count, the grid MUST be omitted. Publication, article, and tag reference categories MUST NOT appear in this grid.
- **FR-002**: Each displayed tile MUST show the number of distinct entries in its category that are both assigned to the displayed publication and accessible to the current reader under existing access rules.
- **FR-003**: While signed out, all publication content tiles MUST remain readable but MUST NOT expose a destination, receive keyboard focus as links, or respond to pointer, touch, or keyboard activation.
- **FR-004**: While signed in, each displayed tile MUST act as a named link to the existing list page for its category with the displayed publication already selected. The link MUST be usable with pointer, touch, and keyboard.
- **FR-005**: The list page MUST identify the active publication by name, apply it together with any other active filters, preserve it across reloads and sorting, and let the reader clear it. A known filter with no matching entries MUST show the normal empty state; an unknown publication selection MUST show no matches rather than broadening the results.
- **FR-006**: Counts and filtered lists MUST follow the same publication membership and access rules. Signing in unlocks tile navigation, not additional entry access by itself. Opening a filtered list directly MUST apply the current reader's existing access rules regardless of how the address was obtained.
- **FR-007**: The homepage, existing publication details outside the new grid and thumbnail, detail pages of other content types, unfiltered category lists, and entry access and purchase-teaser behavior MUST remain unchanged.
- **FR-008**: Every reader-facing publication detail page MUST show the publication's thumbnail using the same image, fallback order, and placeholder as the homepage. The thumbnail MUST appear for every reader, independent of sign-in state, ownership, and the presence of content tiles, and MUST NOT repeat the publication name to assistive technology.

### Key Entities

- **Publication**: An existing source with a detail page; entries may be assigned to one or more publications.
- **Content type**: An existing catalog category whose entries can be assigned to publications, such as Spells, Perks, or Items.
- **Publication content tile**: A category label, icon, and positive count for one publication, with a list destination available only when the reader is signed in.
- **Publication filter**: The selected publication that limits a category list to entries assigned to it, without granting access to those entries.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Across representative publications and signed-out, signed-in non-owner, and owner states, 100% of product-bound categories with a positive expected count show that exact distinct accessible-entry count, including shared entries. No tile appears for any category with an expected count of zero.
- **SC-002**: Every displayed tile opens the correct prefiltered list in one activation while signed in, with no extra filter-setting step. No tile is operable while signed out.
- **SC-003**: For an unchanged catalog and reader access state, 100% of unmodified lists reached from tiles have the same number of matching entries as their originating tiles, and no filtered list includes an inaccessible entry or an entry assigned only to another publication.
- **SC-004**: At least 4 out of 5 representative signed-in readers can locate a named content type with at least one accessible entry for a given publication and open its prefiltered list on their first attempt without assistance.
- **SC-005**: 100% of publication detail pages show either the publication's thumbnail or the homepage fallback, never a broken image, and the result matches what the homepage shows for the same publication.

## Assumptions

- “Like on the main page” means both the familiar tile presentation and the existing convention of counting entries the current reader can access, not exposing totals for inaccessible content.
- A publication association is an existing assignment of an entry to that publication, not an incidental reference in text. No new associations or ownership rules are introduced.
- Sign-in is required to follow a publication tile, not to visit an otherwise available category list directly.
- The feature is limited to the reader UI; it does not change public API behavior or catalog administration.
- “The thumbnail” is the existing homepage publication thumbnail; no new image sizes or sources are introduced.
