# Data Model: Enforce Product-Ownership DRM at Domain Level

**Feature**: `005-domain-drm-enforcement` | **Date**: 2026-11-14

There is no schema change. This feature adds domain and application types and makes one data
correction.

## Domain (`torg-codex-domain`, framework-free)

### `ProductBoundEntry` (new interface, `domain.model`)

| Member | Type | Notes |
|---|---|---|
| `id()` | `UUID` | already present on all 14 records |
| `name()` | `String` | already present |
| `products()` | `Set<String>` | codex ids; `null` or empty means *unassigned* |
| `entityType()` | `EntityType` | new default/explicit method naming the catalog area |

These 14 records implement it: `Cosm`, `Item`, `Miracle`, `MiracleList`, `Perk`, `PerkGroup`,
`Power`, `PowerList`, `Race`, `Shard`, `Spell`, `SpellList`, `Threat`, `Vehicle`.
`Article`, `Publication` and `Tag` do **not** implement it; they are reference data (FR-013).

### `ReferencesEntries<SELF>` (new interface, `domain.model`)

| Member | Type | Notes |
|---|---|---|
| `referencedType()` | `EntityType` | `SPELL`, `MIRACLE` or `POWER` |
| `referencedEntries()` | `List<UUID>` | maps to `spells()`, `miracles()` or `powers()` |
| `withReferencedEntries(List<UUID>)` | `SELF` | returns a copy with the filtered list, keeping the order |

Implemented by `SpellList`, `MiracleList` and `PowerList`.

### `OwnedProducts` (new value object, `domain.access`)

| Member | Notes |
|---|---|
| `FREE_TIER = "core-rulebook"` | single source of the free-tier id; `NotLoggedInUserDetails.FREE_PRODUCT_ID` refers to it |
| `of(Set<String> codexIds)` | `null`-safe; result = `codexIds ∪ {FREE_TIER}`; unmodifiable |
| `anonymous()` | `{FREE_TIER}` |
| `codexIds()` | `Set<String>`, used for `Censor.of(...)` |
| `ownsAnyOf(Set<String>)` | `true` if the intersection is non-empty |

Invariant: it always contains `FREE_TIER`. Unknown ids are kept but have no effect.

### `CatalogAccessPolicy` (new, `domain.access`, stateless)

`AccessDecision decide(ProductBoundEntry entry, OwnedProducts owned)`:

| Condition (evaluated in order) | Decision |
|---|---|
| `entry.products()` is `null` or empty | `UNASSIGNED` |
| `owned.ownsAnyOf(entry.products())` | `GRANTED` |
| otherwise | `TEASER` |

### `EntryAccess<T extends ProductBoundEntry>` (new sealed interface, `domain.access`)

| Variant | Fields | Produced when |
|---|---|---|
| `Granted<T>` | `T entry` | decision `GRANTED` (embedded references already filtered) |
| `Teaser<T>` | `UUID id`, `String name`, `EntityType type`, `SortedSet<String> requiredProducts` | decision `TEASER`; `requiredProducts = entry.products()` |
| `Absent<T>` | — | entry not found **or** decision `UNASSIGNED` (indistinguishable, FR-008) |

## Application (`torg-codex-application`, framework-free)

### Driving port `RestrictedCatalogQuery<T extends ProductBoundEntry>` (new, `port.in`)

| Method | Returns | Semantics |
|---|---|---|
| `findAll()` | `List<T>` | only `GRANTED` entries, with embedded references filtered |
| `findByCosm(String slug)` | `List<T>` | the same as `findAll`, limited to the cosm |
| `findById(UUID id)` | `EntryAccess<T>` | see the variants above |

The existing `CatalogQuery<T>` is kept for `Article`, `Publication` and `Tag` only.

### Driven ports (`port.out`)

| Port | Method(s) | Implemented by |
|---|---|---|
| `CallerProductsPort` (new) | `OwnedProducts current()` | `ProductOwnershipResolver` (`torg-codex`, `api.security`) |
| `CatalogAccessObserver` (new) | `void entryWithoutProducts(EntityType area, UUID id, String name)` | `CatalogAccessObservabilityAdapter` (`torg-codex`) |
| `CatalogPersistencePort<T>` (extended) | `+ default List<T> findAllById(Collection<UUID> ids)` (distinct ids through existing `findById`, dropping absent results; no constraint on `T`) | overridden with one `JpaRepository.findAllById` query in `JpaSpell`, `JpaMiracle`, `JpaPower` `PersistenceAdapter`s; only these overrides back embedded-reference resolvers |
| `CatalogReferencePersistencePort` (changed) | `findCosmBySlug` returns `Optional<CatalogEntryReference>` | `JpaCatalogReferenceAdapter` |

`CatalogEntryReference(UUID id, String name, Set<String> products)` is a new port-out record. It
implements `ProductBoundEntry` with `entityType() = COSM`.

### Service `RestrictedCatalogQueryService<T>` (new, `service`)

Constructor collaborators:
- `EntityType area`
- `CatalogPersistencePort<T>`
- `CallerProductsPort`
- `CatalogAccessObserver`
- `CatalogAccessPolicy`
- optional `ReferencedEntryResolver` (only for the three list areas)

Processing, per call:
1. Resolve `owned = callerProducts.current()` once.
2. Load the candidates from persistence.
3. For each candidate, call `decide(entry, owned)`. For `UNASSIGNED`, call
   `observer.entryWithoutProducts(area, id, name)` and drop the entry. This happens once per entry
   per call, without deduplication (FR-016).
4. Listings keep the `GRANTED` entries. `findById` maps the decision to an `EntryAccess` variant.
5. If the area references other entries, it collects the referenced ids of all kept entries and
   calls `resolver.accessibleIds(ids, owned)` once. That call does one batched `findAllById`,
   applies the policy, and reports `UNASSIGNED` referenced entries to the observer. It then
   rebuilds each entry with `withReferencedEntries(filtered)`.

### Service `CatalogReferenceQueryService` (changed)

`findCosmBySlug(slug)`:
- product-less cosm → `Optional.empty()` and `observer.entryWithoutProducts(COSM, …)`;
- otherwise → `CatalogReference(id, name)`.

The caller's ownership is irrelevant here, because references expose only teaser-level data
(FR-009).

## Web adapter (`torg-codex`)

### `EntryTeaserDto` (new, `api.dto`)

| Field | Type | Source |
|---|---|---|
| `id` | `UUID` | `Teaser.id` |
| `name` | `String` | `Teaser.name` |
| `type` | `String` | `Teaser.type.tag()` (e.g. `spell`, `spell-list`, `group`) |
| `requiredProducts` | `List<RequiredProductDto>` | `Teaser.requiredProducts`, sorted by codex id |

`RequiredProductDto(String codexId, PublicationRefDto publication)` uses `publication = null` if
no publication exists for the codex id (spec edge case: the teaser must not fail).

## Data correction (FR-018) — Liquibase `2026-11-14-001-migrate.yaml` → `2026-11-14-001-migrate.sql`

| Step | Table | Effect | Guard |
|---|---|---|---|
| 1 | `torg_cosm_products` | insert `(id, 'core-rulebook')` for slugs `aysle, core-earth, cyberpapacy, living-land, nile-empire, orrorsh, pan-pacifica, tharkold, universal, unknown` | only cosms with **no** product row |
| 2 | `torg_perk_group_products` | insert distinct `(group.id, perk_products.product)` where `torg_perk.perk_group = lower(replace(group.name, ' ', '-'))` | `NOT EXISTS` the same row |
| 3 | `torg_perk_group_products` | delete every assignment not in that group's derived perk-product union, including stale non-`groups` grants | runs after step 2; use a correlated `NOT EXISTS` over the same group/perk/product join |

On the public free-tier dataset, which has no cosm, perk or perk-group rows, all three steps are
no-ops.

The final products of each group must equal the derived set, not a superset. A group with no
product-bearing perks ends with no products and fails closed under FR-008; do not retain
`groups` or another stale grant as a fallback. Tests seed both a stale non-`groups` assignment
and an empty-derived-set group, and verify exact equality after each of two executions.

The SQL lives in a separate `2026-11-14-001-migrate.sql`, which the YAML changeset runs through
`sqlFile`. This lets `CatalogDataFixMigrationIT` execute it against seeded rows, twice, to prove
idempotency. Adding the YAML changeset and changing `db.changelog-master.yaml` means updating
`torg-codex-data/src/test/resources/liquibase-changelog-manifest.sha256`, which
`LiquibaseChangelogGuardTest` checks.

The reference projection, changed persistence-port signature, adapter mapping, application
service, composition wiring and affected tests are one atomic implementation task (T023).
In particular, the old `Optional<CatalogReference>` delegation cannot remain after the port
changes to `Optional<CatalogEntryReference>`.

## State and lifecycle

There is no persisted state. Ownership is evaluated per request (spec assumption). The
observability counter is monotonic per application instance.
