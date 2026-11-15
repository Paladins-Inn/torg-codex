# Phase 1 Data Model: Torg Codex Web User Interface

**Feature**: `006-codex-web-ui` | **Date**: 2026-11-15 | **Spec**: [spec.md](./spec.md)

No relational database schema change is required (`ddl-auto: validate` and existing Liquibase changelogs remain untouched). This document specifies the framework-free domain abstractions, metadata descriptors, application browse/metric models, and inbound web view models.

---

## 1. Domain Layer (`torg-codex-domain`)

### 1.1 `CatalogArea` (`de.paladinsinn.torg.codex.domain.catalog.CatalogArea`)

Unified enumeration of all **17** catalog areas (keeps `EntityType` untouched for the 14 product-bound areas per `ArchUnit A3`):

| Enum Constant | Canonical Path (`slug`) | Alias Path(s) | Singular Tag | Navigation Section | Product-Bound (`EntityType`) | Bootstrap Icon |
|---|---|---|---|---|---|---|
| `COSM` | `cosms` | — | `cosm` | `GAME_MASTER` | `EntityType.COSM` | `bi-globe2` |
| `RACE` | `races` | — | `race` | `STORM_KNIGHTS` | `EntityType.RACE` | `bi-people` |
| `PERK` | `perks` | — | `perk` | `STORM_KNIGHTS` | `EntityType.PERK` | `bi-stars` |
| `PERK_GROUP` | `groups` | `perk-groups` | `group` | `STORM_KNIGHTS` | `EntityType.GROUP` | `bi-collection` |
| `SPELL` | `spells` | — | `spell` | `STORM_KNIGHTS` | `EntityType.SPELL` | `bi-magic` |
| `SPELL_LIST` | `spell-lists` | — | `spell-list` | `STORM_KNIGHTS` | `EntityType.SPELL_LIST` | `bi-journal-bookmark` |
| `MIRACLE` | `miracles` | — | `miracle` | `STORM_KNIGHTS` | `EntityType.MIRACLE` | `bi-sun` |
| `MIRACLE_LIST` | `miracle-lists` | — | `miracle-list` | `STORM_KNIGHTS` | `EntityType.MIRACLE_LIST` | `bi-journal-plus` |
| `POWER` | `powers` | — | `power` | `STORM_KNIGHTS` | `EntityType.POWER` | `bi-lightning-charge` |
| `POWER_LIST` | `power-lists` | — | `power-list` | `STORM_KNIGHTS` | `EntityType.POWER_LIST` | `bi-journal-code` |
| `ITEM` | `items` | — | `item` | `GEAR_AND_VEHICLES` | `EntityType.ITEM` | `bi-shield-shaded` |
| `VEHICLE` | `vehicles` | — | `vehicle` | `GEAR_AND_VEHICLES` | `EntityType.VEHICLE` | `bi-truck-front` |
| `SHARD` | `shards` | — | `shard` | `GAME_MASTER` | `EntityType.SHARD` | `bi-gem` |
| `THREAT` | `threats` | — | `threat` | `GAME_MASTER` | `EntityType.THREAT` | `bi-exclamation-octagon` |
| `PUBLICATION` | `publications` | — | `publication` | `REFERENCE` | `Optional.empty()` | `bi-book` |
| `ARTICLE` | `articles` | — | `article` | `REFERENCE` | `Optional.empty()` | `bi-file-earmark-text` |
| `TAG` | `tags` | — | `tag` | `REFERENCE` | `Optional.empty()` | `bi-tags` |

**Validation / Routing Invariant**: Reserved path `"admin"` (and `"api"`, `"login"`, `"logout"`, `"webjars"`, `"themes"`, `"actuator"`) MUST NEVER match any `CatalogArea` path or alias.

### 1.2 `NavigationSection` (`de.paladinsinn.torg.codex.domain.catalog.NavigationSection`)

Ordered top-level navigation groupings for the reader UI:
1. `STORM_KNIGHTS` ("Storm Knights": Races, Perks, Perk Groups, Spells, Spell Lists, Miracles, Miracle Lists, Psionics/Powers, Power Lists)
2. `GEAR_AND_VEHICLES` ("Gear & Vehicles": Items/Gear, Vehicles)
3. `GAME_MASTER` ("Game Master": Cosms, Eternity Shards, Threats)
4. `REFERENCE` ("Reference": Publications, Articles, Tags)

### 1.3 `GroupingDimension` & `DetailAccessOutcome` (`de.paladinsinn.torg.codex.domain.catalog`)

- **`GroupingDimension`**:
  - `PERK_GROUP` (`perk-group`, query param `group`)
  - `SPELL_LIST` (`spell-list`, query param `list`)
  - `MIRACLE_LIST` (`miracle-list`, query param `list`)
  - `POWER_LIST` (`power-list`, query param `list`)
  - `TAG` (`tag`, query param `tag`)
  - `ITEM_TYPE` (`item-type`, query param `type`)
  - `VEHICLE_TYPE` (`vehicle-type`, query param `type`)
  - `CLEARANCE` (`clearance`, query param `clearance`: `alpha`, `beta`, `gamma`, `delta`, `omega`)
  - `RACE` (`race`, query param `race`)
- **`DetailAccessOutcome`**:
  - `DRM_GRANTED` (`drm-granted`)
  - `PUBLIC_REFERENCE` (`public-reference`)
  - `TEASER` (`teaser`)
  - `NOT_FOUND` (`not-found`)

### 1.4 `CatalogSlugs` (`de.paladinsinn.torg.codex.domain.catalog.CatalogSlugs`)

Pure deterministic slug normalizer:
- `normalize(String input)`: NFKD decomposition → strip `\p{M}` diacritics → lowercase `Locale.ROOT` → replace `[^a-z0-9]+` with `"-"` → strip leading/trailing `"-"`.
- `isUuid(String input)`: matches canonical 36-char UUID pattern before calling `UUID.fromString`.
- Collision disambiguation within an area: if `N > 1` entries in the same `CatalogArea` produce identical base slugs, entries after the lexicographically first `UUID` receive `<base-slug>-<uuid-first-8-chars>` as their canonical slug while `id.toString()` lookup always resolves unambiguously.

### 1.5 `CatalogEntry` (`de.paladinsinn.torg.codex.domain.model.CatalogEntry`)

Common core interface implemented by `ProductBoundEntry` (all 14 DRM-bound domain records) and by `Article`, `Publication`, and `Tag`:

- `UUID id()` — immutable persistence UUID.
- `String name()` — human-readable display name.
- `CatalogArea catalogArea()` — area of the entry (default on `ProductBoundEntry` maps from `entityType()`).
- `default String slug()` — `CatalogSlugs.normalize(name())`; overridden by `Cosm` (`return slug;`) and `Publication` (`return codexId;`).
- `default Optional<ClearanceLevel> clearance()` — returns `Optional.ofNullable(clearanceLevel())` where present, `Optional.empty()` on `Publication`.
- `default Set<String> cosmSlugs()` — returns normalized cosm slugs associated with the entry (`Set.of(slug)` on `Cosm`; `cosms` set on `Perk`, `Item`, `SpellList`, `MiracleList`, `PowerList`, `Shard`, `Threat`, `Vehicle`; empty set otherwise).
- `default Set<String> productCodexIds()` — returns `products()` on `ProductBoundEntry`, `Set.of(codexId)` on `Publication`, empty set on `Article` and `Tag`.

### 1.6 `CatalogCategoryDescriptor<E extends CatalogEntry>` (`de.paladinsinn.torg.codex.domain.catalog`)

Immutable metadata descriptor registered per `CatalogArea` in `CatalogCategoryRegistry`:
- `CatalogArea area`
- `Class<E> entryClass`
- `List<ColumnDescriptor<E>> columns` — each column has `key`, `header`, `CellKind` (`PRIMARY_LINK`, `TEXT`, `CLEARANCE_BADGE`, `COSM_BADGES`, `AXIOM`, `DIFFICULTY_NUMBER`, `GROUP_BADGE`, `BOOLEAN_FLAG`, `EXTERNAL_LINK`), `Function<E, Object> extractor`, and `boolean sortable`.
- `List<FilterFacetDescriptor> facets` — ordered structured facets enabled on this category's table view (`COSM`, `GroupingDimension` subset).
- `List<DetailSectionDescriptor<E>> detailSections` — ordered detail sections:
  - `ATTRIBUTES` (list of labeled scalar/badge/map fields, e.g., Axiom, Casting Time, DN, Range, Duration, Required Skills, Attribute Limits, Threat Stats, Vehicle Stats/Weaponry)
  - `PROSE` (labeled raw Torg-markup field extractors — e.g., `text`, `worldLaws`, `notes`, `additionalFeatures`, `specialAbilities` — rendered exclusively through `Censor` at the web boundary)
  - `EMBEDDED_LIST` (referenced `CatalogArea` + `List<UUID>` extractor, e.g., spells in `SpellList`, miracles in `MiracleList`, powers in `PowerList`, parent in `Tag`)

#### Summary Table Columns & Filter Facets Across All 17 Categories

| Category | Tabular Summary Columns (First column links to `/<category>/<slug>`) | Structured Filter Facets (plus `q` Name Search) |
|---|---|---|
| `Cosms` | Name, Slug, Clearance, Axioms Summary | Clearance |
| `Races` | Name, Cosm, Clearance | Cosm, Clearance |
| `Perks` | Name, Clearance, Group, Cosms, Contradiction, Prerequisites | Clearance, Cosm, Perk Group, Race |
| `Perk Groups` | Name, Clearance | Clearance |
| `Spells` | Name, Clearance, Axiom, DN, Casting Time, Range, Duration | Clearance, Spell List |
| `Spell Lists` | Name, Clearance, Cosms, Unlocking Perks, Spell Count | Clearance, Cosm |
| `Miracles` | Name, Clearance, Axiom, DN, Casting Time, Range, Duration | Clearance, Miracle List |
| `Miracle Lists` | Name, Clearance, Cosms, Unlocking Perks, Miracle Count | Clearance, Cosm |
| `Powers` | Name, Clearance, Axiom, DN, Casting Time, Range, Duration | Clearance, Power List |
| `Power Lists` | Name, Clearance, Cosms, Unlocking Perks, Power Count | Clearance, Cosm |
| `Items` | Name, Type, Cosms, Clearance, Tech/Magic Axiom, Price, Bonus/Range | Item Type, Cosm, Clearance |
| `Vehicles` | Name, Type, Cosms, Clearance, Axiom, Top Speed, Toughness, Price | Vehicle Type, Cosm, Clearance |
| `Shards` | Name, Cosms, Clearance, Price/Tapping | Cosm, Clearance |
| `Threats` | Name, Cosms, Clearance, Size/Toughness | Cosm, Clearance |
| `Publications` | Name, Codex ID, Publisher/Third-Party, DriveThruRPG ID | — |
| `Articles` | Name, Clearance | Clearance |
| `Tags` | Name, Clearance, Parent Tag | Clearance |

---

## 2. Application Layer (`torg-codex-application`)

### 2.1 `CatalogFilterCriteria` (`de.paladinsinn.torg.codex.application.port.in.CatalogFilterCriteria`)

Immutable query value object:
- `String query` — optional case-insensitive substring filter on `name` (trimmed; never recorded in metrics).
- `Set<String> cosms` — requested cosm filter slugs.
- `Map<GroupingDimension, Set<String>> groupings` — requested structured facet values.
- `String sortKey` — column key (defaults to `"name"`).
- `SortDirection direction` — `ASC` (default) or `DESC`.

### 2.2 `CatalogBrowseResult` & `CatalogDetailResult`

- **`CatalogBrowseResult`**:
  - `CatalogArea area`
  - `CatalogCategoryDescriptor<?> descriptor`
  - `List<CatalogEntry> entries` (already DRM-filtered, facet-filtered, and sorted, with collision-safe canonical slugs resolved)
  - `int totalAccessibleInCategory` (count before facet/text narrowing)
  - `Map<String, String> availableCosms` (`slug -> displayName` for accessible cosms)
  - `Map<GroupingDimension, List<FacetOption>> availableFacets` (bounded options derived from accessible catalog data/enums, with active state)
  - `CatalogFilterCriteria appliedCriteria`
- **`CatalogDetailResult`** (sealed interface):
  - `GrantedProductEntry(ProductBoundEntry entry, String canonicalSlug, List<LinkedEntryRef> embeddedRefs, List<PublicationRef> publications)`
  - `PublicReferenceEntry(CatalogEntry entry, String canonicalSlug, List<LinkedEntryRef> relatedRefs)`
  - `RestrictedTeaser(EntryAccess.Teaser<?> teaser, String canonicalSlug, List<RequiredPublicationRef> requiredPublications)`
  - `Absent(CatalogArea area)`

### 2.3 `CodexHomeOverview`

- `List<CategoryCardSummary> categories` (all 17 areas: area, displayName, icon, accessibleCount)
- `boolean authenticated`
- `int ownedPublicationCount`
- `int totalPublicationCount`
- `double unlockedPercentage`
- `List<PublicationCardSummary> missingPublications` (codexId, name, coverUrl, driveThruUrl)
- `List<PublicationCardSummary> ownedPublications`

---

## 3. Web Adapter View Models (`torg-codex` — `de.paladinsinn.torg.codex.web.codex.model`)

- **`NavContextViewModel`**: active `CatalogArea` (nullable), grouped `NavigationSection` menus, `boolean authenticated`, `String statusLabel` (e.g., `"Core Rulebook (Free Tier)"` vs `"Signed In (N Books)"`).
- **`TableViewModel`**: `CatalogArea` metadata, active `CatalogFilterCriteria`, `availableCosms`, `availableFacets`, column headers with sort URLs, `List<TableRowViewModel>` (canonical detail URL + typed `List<TableCellViewModel>`), `totalMatching`, `totalAccessible`.
- **`DetailViewModel`**: `CatalogArea` metadata, `name`, `canonicalUrl`, optional `ClearanceLevel`, `List<CosmBadgeViewModel>`, `List<AttributeGroupViewModel>`, `List<RenderedProseSection>` (carrying `SanitizedHtml` rendered via `CurrentUserCensorFactory.create()`), `List<LinkedEntryViewModel>`, `List<PublicationLinkViewModel>`.
- **`TeaserViewModel`**: `name`, `CatalogArea` label, `List<RequiredProductViewModel>` (codexId, optional publication title, cover URL, and external DriveThruRPG purchase URL).
