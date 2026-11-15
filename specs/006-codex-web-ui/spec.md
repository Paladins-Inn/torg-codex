# Feature Specification: Torg Codex Web User Interface

**Feature Branch**: `006-codex-web-ui`

**Created**: 2026-11-15

**Status**: Draft

**Input**: User description: "I need an UI for torg-codex. The used frameworks should be bootstrap5 using bootstrap icons hand htmlx and jte. The application should replace https://torg-codex.com and the navigation should contain the different categories leading to tabular views of the contents available. For the single elements there should be a detailed view. The DRM needs to be obliged for all displays." Additional input: "login/logout should be a seperate page and not handled like the current https://torg-codex.com"; "every access to a category should add to a metric counter. every access/filter to a cosm should add to a metric counter. every access/filter to any grouping should add to a group specific counter. every access to a detail page should add to a metric counter for that entry."; "The interface should be generic as possible. All entries should adhere to a common core concept to make the implementation more easy."; "create a logging policy following https://www.lichti.de/Software/2021-05-24-logging-in-java-ii.html and https://www.lichti.de/Software/2017-01-03-logging-in-java.html - create a concept page and an adr."; "prepare for a seperate admin interface to maintain and add data to the system that uses a seperate theme. It is not specified yet but we should be able to seperate both UI in the same application."

## Clarifications

### Session 2026-11-15

- Q: How should user login and logout be presented compared to the legacy `https://torg-codex.com` inline navigation form? → A: Login and logout MUST be handled on a dedicated page rather than via an inline credential form embedded in the navigation bar.
- Q: Should the new usage metric counters (category, cosm, grouping, and entry detail accesses) record visits through the Web UI only, or across both the Web UI and the REST endpoints? → A: Both; usage counters MUST be recorded centrally in the core catalog services across all inbound channels without a channel label.
- Q: When a single entry is requested, how should the detail metric counter handle outcomes? → A: Record four separate outcomes: DRM-granted detail (for product-bound entries), public reference detail (for unrestricted reference entries: Articles, Publications, Tags), teaser (unowned product-bound entries), and not-found (missing or unassigned entries, recorded per category without untrusted identifier labels to keep metric cardinality bounded).
- Q: Which dimensions count as a grouping for generic filtering and group-specific counters, and what aggregate counters are required? → A: Track all catalog groupings and structured filter facets (Perk Groups, Spell/Miracle/Power Lists, Tags, Item Types, Vehicle Types, Clearance Levels, and Race filters, bounded to known catalog values and excluding free-text search). In addition, expose both sets of aggregate roll-up counters: four access-outcome total counters (DRM-granted, public-reference, teaser, not-found) and four metric-family total counters (all category accesses, all cosm accesses/filters, all grouping accesses/filters, and all detail accesses).
- Q: How broadly should the common core entry concept and generic UI rendering apply across catalog areas? → A: Fully generic views across all 17 catalog areas (the 14 product-bound areas plus Articles, Publications, and Tags) driven by common core entry metadata descriptors for navigation, tabular views, filtering, detail views, and purchase teasers.
- Q: How should individual entries be identified in UI detail URLs and in-text cross-reference links under the common core entry concept? → A: Use human-readable slugs as canonical UI URLs across all 17 categories while also accepting UUIDs seamlessly.
- Q: What logging policy and severity rules govern operational and domain events? → A: Logs belong to Operations and Support (not developer debugging) and follow strict operational severity tiers: `ERROR` for technical system-impairing failures (24/7 on-call), `WARN` for single-request technical disruptions or catalog data defects (business-hours admin review), `INFO` for normal completions and all domain/business errors (such as invalid login credentials, unowned-entry teasers, and not-found lookups — never `WARN` or `ERROR`), `DEBUG` for 2nd-level flow diagnostics, `TRACE` for 3rd-level full traces, and an optional dedicated `business` logger when separate domain audit streams are needed.
- Q: How should the application accommodate a future separate administrative UI with its own theme? → A: Structure the reader UI with explicit separation of route namespaces (reserving `/admin` for the future admin interface), layout shells, navigation, and theme stylesheets inside the same application so a separately themed admin interface for maintaining and adding catalog data can be introduced later without restructuring the reader UI; admin maintenance functionality itself remains out of scope for this feature.
- Q: How do non-admin reader users authenticate? → A: Non-admin reader login on the dedicated Login page is exclusively via DriveThruRPG API key (establishing product ownership for the session); OIDC role-based login is reserved for the future `/admin` interface.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Browse Catalog Categories in Tabular Views (Priority: P1)

A player or game master visits Torg Codex to browse game reference material. From a persistent global navigation menu present on every page, they select any of the 17 codex categories (Races, Perks, Perk Groups, Spells, Spell Lists, Miracles, Miracle Lists, Psionic Powers, Power Lists, Items/Gear, Vehicles, Cosms, Eternity Shards, Threats, Publications, Articles, or Tags) and view the available contents in a structured, readable table tailored via category metadata descriptors to that category's key summary attributes. Within a category table, they can narrow or sort entries (for example by search term, cosm, clearance level, or category-specific groupings) so they quickly find relevant entries during character creation or game play.

**Why this priority**: Navigating categories and scanning available entries in tabular form is the primary purpose of replacing `https://torg-codex.com` and delivers immediate reference value as a standalone slice.

**Independent Test**: Open the landing page, navigate through each category in the main navigation menu, verify that each category renders a tabular listing of only the visitor's accessible entries with category-appropriate columns, and verify that filtering and text search narrow the displayed table rows accurately.

**Acceptance Scenarios**:

1. **Given** a visitor opens Torg Codex, **When** they inspect the global navigation, **Then** all supported codex categories and reference sections are organized logically and reachable from any page.
2. **Given** a visitor selects a catalog category from the navigation, **When** the category view loads, **Then** the available entries are presented in a tabular layout displaying that category's key summary columns (such as name, clearance, cosm, axiom, or type/group where applicable) sorted in a consistent default order.
3. **Given** a visitor is viewing a category table, **When** they enter a search term or select category filters (such as cosm or clearance), **Then** the table updates to show only accessible entries matching all active criteria, or displays a clear empty-state message when none match.
4. **Given** a visitor views the landing page, **When** the page loads, **Then** they see an overview of codex categories with entry counts reflecting their currently accessible content, along with their publication unlock status.

---

### User Story 2 - Inspect Single Elements in a Detailed View (Priority: P1)

A player or game master selects a single entry from any category table, embedded list (such as a spell within a spell list), or in-text cross-reference link to read its complete rules and description. The detailed view presents all of the element's attributes, game statistics, formatted rich text (with game symbols and clickable cross-references), related elements, and the source publication(s) in which the entry appears.

**Why this priority**: Tabular summaries alone are insufficient at the gaming table; inspecting full rules, statistics, and cross-references for individual codex elements is co-equal with category browsing for a usable replacement of `https://torg-codex.com`.

**Independent Test**: Select an accessible entry in each catalog category from its tabular view and via an in-text cross-reference link; verify that its detailed view displays all domain attributes, formatted rules text, navigable related-entry links, and publication attribution.

**Acceptance Scenarios**:

1. **Given** a visitor views a category table, **When** they activate any listed entry, **Then** they are taken to that entry's detailed view showing its complete attributes, game statistics, formatted body text, and source publication attribution.
2. **Given** an accessible entry's text or embedded list references another accessible codex entry, **When** the visitor follows that reference link, **Then** the referenced entry's detailed view opens.
3. **Given** an accessible entry contains product-gated passages (`<IF:…>` blocks), **When** its detailed view is displayed, **Then** passages gated to products the visitor owns (including the free tier) are shown and passages gated exclusively to unowned products are omitted.
4. **Given** a visitor opens the URL of an entry directly using either its canonical human-readable slug or its UUID (via bookmark or shared link), **When** the visitor is entitled to read the entry, **Then** the full detailed view loads with global navigation intact.

---

### User Story 3 - Enforce Product-Ownership DRM Across Every Display (Priority: P1)

An anonymous visitor or a signed-in user with a specific set of owned publications uses the web interface. Across every screen — landing page counts, category tables, filtered/searched results, embedded element lists, cross-references, and single-element detail pages — the interface strictly honors product-ownership DRM. Unowned entries never appear in tables, counts, or embedded lists; direct navigation to an unowned entry displays only a purchase teaser naming the entry and the publication(s) that unlock it; and entries without any product assignment remain completely invisible.

**Why this priority**: Protecting proprietary Torg Eternity sourcebook content from unauthorized disclosure is a non-negotiable legal and constitutional requirement across all displays.

**Independent Test**: Compare the UI as an anonymous visitor versus a signed-in owner of a non-free publication (e.g., `sourcebook-aysle`): verify that sourcebook-only entries are absent from anonymous tables/counts and present for the owner, that opening a sourcebook-only entry URL anonymously shows only the purchase teaser without protected fields, and that product-less entries return a not-found state for everyone.

**Acceptance Scenarios**:

1. **Given** the catalog contains entries assigned only to `core-rulebook` and entries assigned only to `sourcebook-aysle`, **When** an anonymous visitor views landing-page counts, category tables, or filtered category tables, **Then** only `core-rulebook` entries are counted and listed, and zero `sourcebook-aysle`-only entries appear.
2. **Given** an entry assigned exclusively to `sourcebook-aysle`, **When** an anonymous visitor navigates directly to that entry's detail URL or follows an in-text cross-reference to it, **Then** the UI displays a purchase teaser showing only the entry's name and the publication(s) required to unlock it (with purchase links when known), and exposes none of its statistics, attributes, or rules text.
3. **Given** a list entry (such as a spell list, miracle list, or power list) that is accessible to the visitor but references individual spells/miracles/powers from unowned publications, **When** the visitor opens that list's detailed view, **Then** unowned referenced entries are omitted from the embedded list.
4. **Given** an entry with no product assignment at all, **When** any visitor browses category tables or navigates directly to that entry, **Then** it never appears in any table and direct navigation responds identically to a non-existent entry ("Not Found").

---

### User Story 4 - Sign In and Sign Out via Dedicated Pages (Priority: P2)

A user who owns Torg Eternity publications wants to unlock their purchased content in the web interface, and later sign out on a shared or personal device. Instead of an inline credential input cluttering the navigation bar on every page, the navigation bar displays the user's current authentication status and links to a dedicated Login page when signed out, or to a dedicated Logout page/flow when signed in.

**Why this priority**: Anonymous visitors can already browse free-tier Core Rulebook content without signing in, so dedicated authentication pages rank right behind core browsing and DRM enforcement while unlocking full value for product owners.

**Independent Test**: Click the sign-in link in the navigation bar, verify a dedicated login page opens (and no credential input field exists inside the global navigation bar), sign in with valid credentials to see owned-product content appear in tables and detail views, then navigate to the dedicated logout page, confirm sign-out, and verify the session reverts to free-tier visibility.

**Acceptance Scenarios**:

1. **Given** an anonymous visitor is on any codex page, **When** they inspect the global navigation bar, **Then** no inline credential input form is rendered in the navigation bar; instead, a clear Sign In navigation action leads to the dedicated Login page.
2. **Given** a visitor is on the dedicated Login page, **When** they submit valid credentials, **Then** their product ownership is established for the session and they are redirected to the codex with all owned publications' entries now accessible.
3. **Given** a visitor is on the dedicated Login page, **When** they submit invalid credentials, **Then** they remain on the Login page and see a clear, user-friendly error message without exposing sensitive credential or system details.
4. **Given** a signed-in user selects the Sign Out action in the navigation bar, **When** they visit the dedicated Logout page and complete sign-out, **Then** their session is terminated, confirmation or redirect to the public codex occurs, and subsequent displays show only free-tier content.

---

### Edge Cases

- **No matching entries after filtering/searching**: The tabular view stays intact with filter controls accessible and shows a clear "no matching entries" state with an easy way to reset filters, rather than a blank area or error page.
- **Direct navigation to a non-existent entry identifier**: Displays a styled "Not Found" page within the standard site navigation rather than raw technical error traces.
- **Direct navigation to an entry without any product assignment**: Behaves identically to a non-existent entry ("Not Found"), never revealing a teaser or existence hint.
- **Cross-reference inside accessible text pointing to an unowned entry**: Following the link leads cleanly to the referenced entry's purchase teaser view without exposing protected content.
- **Cross-reference inside accessible text pointing to a missing or unassigned entry**: Following the link leads to the standard "Not Found" view.
- **Signed-in user with zero purchased products**: Still retains full access to all free-tier (`core-rulebook`) entries, identical to an anonymous visitor.
- **Signing out while viewing a sourcebook-only detail page or filtered category**: After logout completes, the user is returned to a safe public view (or sees the purchase teaser if revisiting the protected entry URL) with zero residual cached protected content.
- **Teaser for an entry whose required product has no matching publication metadata**: Displays the required product identifier gracefully without failing to render the teaser page.
- **Entries belonging to multiple cosms or multiple publications**: Multi-cosm entries appear when filtering by any of their assigned cosms and display all visible cosms clearly; multi-product entries are accessible if the visitor owns at least one assigned product.
- **Entry addressed by slug versus UUID**: Navigating to `/<category>/<slug>` (canonical) or `/<category>/<uuid>` resolves to the same catalog entry across all 17 categories and applies identical DRM and metric rules.
- **Reserved administrative route namespace**: Navigating under the reserved administrative path space (e.g., `/admin/...`) never collides with reader category or slug routes.
- **Small-screen / mobile viewports**: Wide tables remain usable via horizontal scrolling or responsive column prioritization, and the global navigation collapses cleanly into an accessible menu toggle.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide a web user interface that serves as the primary browser-accessible replacement for `https://torg-codex.com`, including a landing overview page, global category navigation, tabular category views, single-element detail views, purchase teaser views, and dedicated authentication pages.
- **FR-002**: Every page MUST include a consistent, responsive global navigation structure providing access to all 17 catalog categories: the 14 product-bound categories (Cosms, Races, Perks, Perk Groups, Spells, Spell Lists, Miracles, Miracle Lists, Powers/Psionics, Power Lists, Items/Gear, Vehicles, Eternity Shards, Threats) and all 3 public reference categories (Publications, Articles, and Tags), as well as the landing page.
- **FR-003**: Selecting any catalog category from the navigation MUST display the caller's accessible entries for that category in a structured tabular view with human-readable column headers appropriate to that category's summary attributes.
- **FR-004**: Category tabular views MUST allow visitors to interactively filter and search the table (including text search by name and filtering by relevant attributes such as cosm, clearance level, item type, or perk group where applicable) and update the displayed table rows without losing the surrounding filter state.
- **FR-005**: Category tabular views and detail views MUST have clean, bookmarkable, shareable URLs using human-readable slugs as the canonical detail URL format across all 17 categories while also accepting UUID identifiers seamlessly, so refreshing the browser, following legacy `torg-codex.com` bookmarks, or opening a shared link reproduces the intended view (subject to the recipient's DRM entitlements).
- **FR-006**: Every row in a category tabular view MUST provide direct navigation to the canonical slug-based detailed view of that single element.
- **FR-007**: The detailed view of an accessible element MUST display its full set of domain attributes and game statistics, its formatted rich-text content (including game symbols and clickable cross-references to other codex elements), any accessible embedded element references (such as spells in a spell list, miracles in a miracle list, or powers in a power list), and attribution to its source publication(s).
- **FR-008**: Human-readable slug cross-reference links rendered inside element descriptions and articles MUST resolve within the web UI to the referenced element's detailed view when accessible, to its purchase teaser view when unowned, or to a "Not Found" state when missing or unassigned.
- **FR-009**: All UI displays — including landing-page counts and publication progress, category tabular views, filtered/searched table results, embedded reference lists, and single-element detail views — MUST enforce the central domain-level product-ownership DRM policy and single ownership-resolution mechanism without bypass or duplication.
- **FR-010**: Listings, category tables, search/filter results, and embedded reference lists MUST contain only entries the current visitor is entitled to read (always including the free tier `core-rulebook`); unowned entries and teasers MUST NEVER appear as rows in tabular listings, and unowned data MUST NEVER be sent hidden in page markup or client-side attributes.
- **FR-011**: When a visitor navigates directly to a single element that exists and has assigned products, but the visitor owns none of those products, the UI MUST render a dedicated purchase teaser view showing only the element's name, its category/type, and the publication(s) that grant access (including links to acquire known publications), and MUST NOT render any protected statistics, attributes, or text.
- **FR-012**: Entries without any product assignment MUST be treated as non-existent across the entire UI: omitted from all counts and tables, and rendering the standard "Not Found" view upon direct navigation.
- **FR-013**: All rich-text fields displayed in the UI (across product-bound entries and Articles) MUST apply in-text product-gated passage censoring (`<IF:…>`) for the visitor's effective owned products and render only sanitized safe HTML, preserving the established markup rendering order.
- **FR-014**: User login and logout MUST be provided on dedicated pages rather than via an inline credential input form inside the global navigation bar. The global navigation bar MUST display the visitor's current sign-in state and link to the dedicated Login page when anonymous or to the dedicated Logout page when authenticated.
- **FR-015**: The landing page MUST present a summary of available codex categories (with entry counts scoped strictly to the visitor's accessible content) alongside publication ownership status (highlighting unowned publications so visitors can discover what additional sourcebooks exist).
- **FR-016**: All entries across all 17 catalog areas (14 product-bound areas plus Articles, Publications, and Tags) MUST adhere to a single common core entry concept and metadata-descriptor model exposing uniform UUID and canonical human-readable slug identity, display name, category descriptor, dynamic tabular columns, filterable grouping/cosm associations, ordered detail attribute/section descriptors, embedded reference lists, and rich-text prose, so that category navigation, tabular listings, filtering, detail views, and purchase teaser views are rendered through a single generic UI pipeline without per-category duplication.
- **FR-017**: The core catalog services MUST record channel-agnostic usage metric counters across all inbound accesses (without recording caller identity, credentials, or owned-product sets):
  - Granular counters: (a) a category counter incremented per category read, (b) a cosm counter incremented per cosm access or cosm filter (for known cosms), (c) a group-specific counter incremented per grouping access or filter across all catalog groupings and structured filter facets (Perk Groups, Spell/Miracle/Power Lists, Tags, Item Types, Vehicle Types, Clearance Levels, and Race filters, validated against known catalog values and excluding free-text search strings), and (d) a single-entry detail counter recording four outcomes separately: DRM-granted detail, public reference detail, teaser, and not-found (counted per category without untrusted missing identifiers).
  - Aggregate roll-up counters: (e) four outcome total counters summing DRM-granted, public-reference, teaser, and not-found outcomes across all categories/groups, and (f) four metric-family total counters summing all category accesses, all cosm accesses/filters, all grouping accesses/filters, and all single-entry detail accesses.
- **FR-018**: Operational logging across all UI and catalog interactions MUST treat Operations and Support as the sole target audience and enforce strict severity tiers: `ERROR` only for technical system-impairing failures (24/7 on-call), `WARN` only for single-request technical disruptions or catalog data defects (business-hours review), `INFO` for completed transactions and all domain/user-level outcomes (including invalid login credentials, unowned-entry teasers, and not-found lookups — never `WARN` or `ERROR`, logging completion only on high-rate paths), `DEBUG` for 2nd-level flow diagnostics, and `TRACE` for 3rd-level full traces, never logging credentials or caller product ownership.
- **FR-019**: The reader UI MUST isolate its URL route space (reserving `/admin` for future administrative use), page layout shell, navigation structure, and visual theme stylesheet from a future administrative data-maintenance UI within the same application, while keeping the common core entry model reusable by both interfaces.
- **FR-020**: Automated tests MUST verify navigation across all 17 categories, generic metadata-driven tabular and detail rendering, slug and UUID URL resolution, interactive filtering, dedicated login/logout flows, reader-vs-admin theme/namespace isolation, granular and aggregate usage metric counters across all four outcomes, operational log-level compliance (no `WARN`/`ERROR` on domain rejections), and strict DRM differentiation.

### Key Entities *(include if feature involves data)*

- **Common Catalog Entry**: The unified representation implemented across all 17 catalog areas, providing UUID and canonical human-readable slug identifiers, display name, category metadata descriptor, dynamic summary column values, filterable cosm and grouping memberships, ordered detail attributes/sections, embedded entry references, and rich-text prose.
- **Catalog Category**: One of the 17 codex areas — 14 product-bound areas (Cosms, Races, Perks, Perk Groups, Spells, Spell Lists, Miracles, Miracle Lists, Powers, Power Lists, Items, Vehicles, Shards, Threats) and 3 public reference areas (Publications, Articles, Tags) — described by metadata that drives generic navigation, tabular columns, filters, and detail sections.
- **Tabular Entry Summary**: A single row in a generic category table representing an accessible entry with category-described summary columns and a link to its canonical slug-based Detailed View.
- **Element Detailed View**: The complete generic presentation of a single accessible catalog entry, combining metadata-driven statistics/attribute sections, censored and sanitized rich-text prose, navigable slug cross-references, and source publication links.
- **Purchase Teaser View**: The restricted presentation shown on direct single-element access (by slug or UUID) when the visitor lacks ownership of all granting products, exposing solely the element's name, category, and unlocking publication(s).
- **Catalog Usage Metrics**: Channel-agnostic counters recorded in the core catalog services comprising granular counters (per category, per cosm, per grouping/facet value, and per entry across DRM-granted, public-reference, teaser, and category-bounded not-found outcomes) plus two sets of four aggregate roll-up counters (four outcome totals and four metric-family totals).
- **UI Surface & Theme Boundary**: The structural separation between the reader-facing codex UI (reader layout shell, navigation, and reader theme) and the reserved `/admin` surface (separate admin layout shell and admin theme for future catalog maintenance within the same application).
- **Publication Overview**: Reference information about official and third-party Torg Eternity books, indicating whether the current visitor owns each publication and providing discovery links for unowned books.
- **Visitor Session**: The current browser visitor's authentication state (anonymous free-tier vs. signed-in product owner), established and cleared exclusively via the dedicated Login and Logout pages.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Visitors can reach the tabular view of 100% of all 17 catalog categories (14 product-bound categories plus Publications, Articles, and Tags) in at most 2 clicks from any reader page via the global navigation, and 100% of accessible entries resolve identically via both their canonical human-readable slug URL and their UUID URL.
- **SC-002**: Across all 14 product-bound catalog categories, an anonymous visitor sees 100% of free-tier (`core-rulebook`) entries, 0 entries belonging exclusively to non-free publications in any table, count, or page source, and 0 protected fields when opening a non-free entry URL directly (receiving only the purchase teaser).
- **SC-003**: A signed-in visitor who owns a publication sees 100% of that publication's entries (plus free-tier entries) in category tables and can open their complete detailed views, with gated in-text passages matching their owned publications.
- **SC-004**: Interactive filtering and searching within a category table updates the displayed rows in under 1 second under normal single-instance load without requiring the user to re-enter filter selections.
- **SC-005**: Players and game masters can locate a specific accessible entry from the landing page (by choosing its category, filtering the table, and opening its detailed view) in under 30 seconds.
- **SC-006**: 0 inline credential input fields appear in the global navigation bar; 100% of sign-in and sign-out actions occur through dedicated Login and Logout pages.
- **SC-007**: All primary reader pages (landing, category tables, element detail, purchase teaser, login, and logout) render legibly without horizontal page overflow on both desktop (1280px+) and mobile (375px+) viewports, with reader theme assets and layout templates cleanly isolated from the reserved `/admin` UI namespace.
- **SC-008**: 100% of category, cosm, grouping, and single-entry reads increment the corresponding granular counters and both sets of four aggregate roll-up counters accurately across all four outcomes (DRM-granted, public-reference, teaser, not-found) without creating unbounded metric labels from unknown inputs, and 0 domain/user-level rejections (invalid login, purchase teaser, not-found) emit `WARN` or `ERROR` log entries.

## Assumptions

- The web UI is part of the existing Torg Codex self-contained service, sharing the same application runtime, domain-level DRM rules (feature `005-domain-drm-enforcement`), and markup censoring pipeline without introducing synchronous cross-service calls to itself.
- Legacy `https://torg-codex.com` features that have no backing domain model in `torg-codex` (specifically "Card Decks" at `/decks`) are out of scope for this feature; all 17 catalog areas (14 product-bound areas plus Publications, Articles, and Tags) are in scope.
- Administrative catalog maintenance and data-entry screens under `/admin` are out of functional scope for this feature; only the architectural/layout/theme boundary and reserved `/admin` namespace separation are prepared now so the future admin UI can be added cleanly within the same application.
- Existing REST endpoints (`/api/*`) remain backward-compatible in response contract alongside the new browser UI routes while sharing the core catalog usage metric counters.
- Primary UI labels and navigation terminology follow the English game terminology established by Torg Eternity and `https://torg-codex.com` (e.g., Storm Knights, Perks, Spells, Miracles, Psionics, Gear, Cosms, Eternity Shards, Threats).
- Non-admin reader authentication on the dedicated Login page uses the DriveThruRPG API-key login mechanism exclusively to establish owned publications for the session; role-based OIDC authentication is reserved for the future `/admin` interface.
- The landing page's "missing/available publications" overview lists known publications from the `Publications` reference catalog compared against the visitor's owned product set; per-publication element breakdown counts on missing book cards are included only to the extent derivable without exposing protected entry names or contents.
