# Phase 0 Research: Multi-Cosm Assignment for Catalog Entries

**Feature**: 004-multi-cosm-assignment
**Date**: 2026-09-12

All Technical Context entries were resolvable from the existing codebase and constitution. No
`NEEDS CLARIFICATION` markers remain.

---

## R1: Persistence shape for a many-valued cosm assignment

**Decision**: Model cosm assignments as `@ElementCollection(fetch = FetchType.EAGER)` of
`Set<String>` slug values, stored in a per-entity collection table `torg_<entity>_cosms`, exactly
mirroring the existing `products` collection.

**Rationale**:

- The codebase already uses this precise pattern eight times for product ownership
  (`torg_perk_products`, `torg_vehicle_products`, ...), including the JPQL query idiom
  `:product MEMBER OF p.products`. Reusing it means zero new concepts.
- FR-010 requires unresolvable cosm references to be preserved and surfaced. A foreign-keyed
  `@ManyToMany` to the `Cosm` entity could not store a slug that has no matching cosm row; the
  migration would either fail or silently drop data.
- `torg-codex-data` must stay persistence-only (Principle I). A string-slug collection keeps the
  entity self-contained with no cross-aggregate JPA association.
- The slug-for-filtering / name-for-display separation is already in place, and slugs are the
  stored reference form everywhere.

**Alternatives considered**:

- **`@ManyToMany` to `Cosm`**: Rejected. Breaks FR-010, introduces referential coupling between
  aggregates, and forces the migration to resolve every legacy value up front.
- **Keep the single `cosm` column and store a comma-separated string**: Rejected. This is the
  current defect in a new costume — it is not queryable, `findByCosm` would need `LIKE '%slug%'`
  with false-positive risk on slug prefixes, and it fails the user's explicit instruction that the
  data be mapped "in JPA technology".
- **PostgreSQL array column with a Hibernate type**: Rejected. Non-standard JPA, no precedent in
  the codebase, and no benefit over a collection table at this data volume.

---

## R2: Query strategy for cosm filtering

**Decision**: Replace derived `findByCosm(String cosm)` with an explicit JPQL query
`@Query("SELECT e FROM Entity e WHERE :cosm MEMBER OF e.cosms")`, keeping the method name and
signature unchanged. `findByCosmAndProduct` (present on `Perk` and `Threat`) gets the same
treatment, combining two `MEMBER OF` predicates.

**Rationale**:

- The driving port `CatalogQuery<T>.findByCosm(String)` and the driven port
  `CatalogPersistencePort<T>.findByCosm(String)` keep their exact signatures, so no port,
  service, or controller signature changes. The change is contained in the persistence adapter
  layer, which is precisely what hexagonal architecture is for (Principle I).
- `MEMBER OF` is already the established idiom for the parallel `products` collection.
- Exact slug matching removes the substring false-positive class entirely.

**Alternatives considered**:

- **Spring Data derived `findByCosmsContaining(String)`**: Works, but renames the method and
  therefore ripples into the ports. Rejected for zero benefit.
- **Native SQL against the collection table**: Rejected; no reason to bypass JPQL.

---

## R3: Migration strategy under ADR-012 (Expand / Migrate / Contract)

**Decision**: This feature ships **Expand + Migrate + Contract in a single deployment**, ordered in
the consolidated changelog `data-fix-lists.yaml`. The legacy
`cosm` and `unlocking_perk` columns are dropped once the backfill has run. The prose conversion is a
pure in-place data repair and carries no schema change, so it lives among the migrate changesets.

Phases:

| Changeset | Phase | Action |
|-----------|-------|--------|
| `2026-09-13-001-expand` | Expand | Create the eight `torg_<entity>_cosms` collection tables |
| `2026-09-13-002-expand` | Expand | Create the three `torg_<entity>_unlocking_perks` collection tables |
| `2026-09-13-003-migrate` | Migrate | Backfill single-valued `cosm` values into the collections |
| `2026-09-13-004-migrate` | Migrate | Split encoded `cosm` list values into individual rows |
| `2026-09-13-005-migrate` | Migrate | Backfill and split `unlocking_perk` values into the collections |
| `2026-09-13-006-migrate` | Migrate | Convert encoded prose values to Markdown (`customChange`, R10) |
| `2026-09-13-002-contract` | Contract | Drop the legacy `cosm` and `unlocking_perk` columns |
| `2026-09-13-002-contract` | Contract | Drop the legacy `cosm` column from all eight tables |

**Rationale**:

- **Deviation from Principle IV, decided by the user (clarification 2026-09-12, FR-014)**: no
  backward compatibility is required and no production installation needs a rolling update, so the
  side-by-side window the three-deployment split protects has no value here. The deviation is
  recorded with justification in the plan's Complexity Tracking.
- The contract changeset runs strictly after both migrate changesets, and is placed in its own
  changelog included last, so a failed backfill aborts the deployment before any column is dropped
  (FR-015).
- Applied changesets must never be altered (Principle IV), and the shipped CSV data files are
  immutable, so backfill must be additive SQL that runs *after* `loadData`.
  `data-fix-lists.yaml` is included last in `db.changelog-master.yaml` for exactly this reason.
- Both migrate changesets are naturally idempotent because they insert with a
  `NOT EXISTS` / `ON CONFLICT DO NOTHING` guard, satisfying FR-007.

**Alternatives considered**:

- **Defer the column drop to a later release**: Rejected by user decision. It keeps a dead,
  unread column in eight tables for an unbounded interval and forces the entities to keep carrying
  the legacy field solely for a rollout scenario that cannot occur.
- **Fix the CSV files at source**: Rejected. It changes Liquibase `loadData` checksums on already
  applied changesets, and the bulk of the game data is proprietary and git-ignored, so the public
  build could not be kept in sync.

---

## R4: Parsing the encoded list values in SQL

**Decision**: Use a generic PostgreSQL `regexp_split_to_table` over the encoded value rather than
enumerating the 20 combinations found in the public data set.

Shape of the migrate step, per entity table:

```sql
-- noinspection SqlResolve @ table/"torg_perk_cosms"
-- noinspection SqlResolve @ table/"torg_perk"
INSERT INTO torg_perk_cosms (perk_id, cosm)
SELECT p.id, btrim(part, ' ''')
FROM torg_perk p,
     LATERAL regexp_split_to_table(btrim(p.cosm, '[]'), ',') AS part
WHERE p.cosm LIKE '[%]'
  AND btrim(part, ' ''') <> ''
ON CONFLICT DO NOTHING;
```

**Rationale**:

- The public free-tier data set contains 62 encoded rows in 20 distinct combinations, but the
  **proprietary data set is git-ignored and cannot be inspected here**. Hard-coding the 20 known
  combinations would silently leave proprietary rows broken. A generic parser handles any
  combination.
- The encoded format is uniform and verified: `['slug', 'slug', ...]` — square brackets, single
  quotes, comma-plus-space separation.
- `btrim(..., ' ''')` strips surrounding whitespace and single quotes in one pass.
- The underlying column is `text` (see `torg-data-entity.yaml`), so long encoded values are stored
  intact and nothing is truncated.

**Alternatives considered**:

- **Enumerate the 20 known combinations as explicit inserts**: Rejected, incomplete for
  proprietary data.
- **Parse in Java during a one-off startup task**: Rejected. Migrations belong in Liquibase and
  must be executed by `db-updater` before rollout (Principle IV), not by the application.

---

## R5: Presentation of several cosms in the retained `cosm` field

**Decision**: `TorgMappingSupport.toCosmRef` takes the slug set and folds it into the existing
`CosmRefDto(UUID id, String name)`:

- empty set → `null`
- exactly one slug → resolve it; `CosmRefDto(cosm.id(), cosm.name())`, or
  `CosmRefDto(null, slug)` when unresolvable — identical to today
- several slugs → `CosmRefDto(null, joined)` where `joined` is the display names (falling back to
  the raw slug when unresolvable) sorted alphabetically and joined with `", "`

**Rationale**:

- Directly implements the user's decision recorded as FR-013 and FR-004a-c: the field is kept,
  not replaced, and multiple cosms become a comma-separated value.
- A `null` id for the multi-cosm case is the honest signal that no single cosm identifies the
  entry (FR-004b), and the DTO record already permits it — the current unresolved-slug fallback
  uses `null` too, so no consumer can assume a non-null id.
- Alphabetical sorting by display name gives the deterministic, stable ordering FR-011 and SC-005
  require, independent of `Set` iteration order and independent of the meaningless source order
  documented in the spec assumptions.
- No cosm display name contains a comma, so `", "` stays an unambiguous separator (spec edge case).

**Alternatives considered**:

- **Preserve source order**: Rejected. The spec establishes that source order is not meaningful
  (the same combination appears in different orders), and `@ElementCollection Set` does not
  preserve it anyway.
- **Pick a "first" cosm for the id**: Rejected. The spec states there is no primary cosm, and an
  arbitrary choice would mislead consumers that follow the id.

---

## R6: Domain model change

**Decision**: The domain records change their `String cosm` component to `Set<String> cosms`.
MapStruct entity mappers map it by name; the web mappers call the updated `toCosmRef`.

**Rationale**:

- Principle I requires separate persistence and domain models but the domain must still express
  the real cardinality; leaving `String cosm` in the domain would push the multi-value concern
  back into the adapters.
- Domain modules stay free of Spring and JPA — a `Set<String>` introduces no dependency.
- MapStruct handles `Set<String>` ↔ `Set<String>` without custom code (Principle I, ADR-015).

**Alternatives considered**:

- **Keep `String cosm` in the domain and join in the mapper**: Rejected. It hides cardinality from
  the domain and blocks any future domain-level cosm logic.

---

## R7: ADR requirement

**Decision**: Add `018_multi-valued-references-and-encoded-list-repair.adoc` to
`docs/modules/arc42/pages/09_architecture_decisions`, registered in `_nav.adoc` and `_include.adoc`
following the established pattern. It records three decisions: the many-valued persistence shape for
cosm and unlocking-perk references, the retained-field contract semantics, and the use of a Liquibase
`customChange` for data repair that SQL cannot express safely (R10).

**Rationale**: ADR-001 and the constitution's Additional Constraints require an accepted ADR before
architecture-level implementation is merged. Changing the persistence cardinality of two core
references across eleven aggregates, fixing the externally visible contract semantics of two fields,
and introducing the first Java-backed changeset in the repository are all architecture-level.
ADR-017 is the current highest number.

---

## R8: Test strategy

**Decision**:

- Unit tests (JUnit 5 + AssertJ + Mockito) for the `toCosmRef` folding logic: empty, single,
  single-unresolvable, multiple, multiple-with-one-unresolvable, duplicate slug, ordering
  stability.
- Unit tests for the unlocking-perk folding: empty, single (must be byte-identical to today),
  multiple, duplicate, ordering stability.
- Unit tests for the prose tokenizer (R11), driven by the table in that section, plus the three
  mixed-quoting values and the multiline value measured in the shipped data, plus idempotency:
  running the conversion twice yields the same result.
- Repository/adapter coverage through the existing Testcontainers-backed data tests for
  `MEMBER OF` filtering, for both cosm and unlocking perk.
- `LiquibaseImportIT` continues to validate that the changelog applies against real PostgreSQL, and
  now also that the `customChange` executes successfully inside it.
- `LiquibaseChangelogGuardTest` baseline manifest must be regenerated because new changelog files
  are added and the master changelog changes.
- `CharacterizationReplayTest` fixtures must be recaptured via `CharacterizationFixtureCaptureTest`
  with `-Dcharacterization.capture=true`; expected diffs are limited to the 62 cosm entries, the 5
  unlocking-perk entries, the 97 prose entries and row ordering. SC-005a requires single-cosm
  entries to remain byte-identical, and FR-024 requires untouched prose to remain byte-identical.

**Rationale**: Principle VIII is non-negotiable and demands automated coverage for schema updates
and bug fixes. The characterization suite is the repository's regression net for SC-003 and
SC-005a and is the cheapest way to prove no existing result changed.

**Alternatives considered**:

- **Rely on characterization fixtures alone**: Rejected. Fixtures prove observed behaviour but not
  the folding edge cases (duplicates, unresolvable slugs), which the public data set does not
  exercise.

---

## R9: Persistence shape for many-valued unlocking-perk references

**Decision**: Mirror R1 exactly. Add
`@ElementCollection(fetch = FetchType.EAGER) @CollectionTable(name = "torg_<entity>_unlocking_perks",
joinColumns = @JoinColumn(name = "list_id")) Set<String> unlockingPerks` to the three list entities
(`MiracleList`, `PowerList`, `SpellList`), storing the raw perk slug in a `slug` column. Replace
`findByUnlockingPerk(String)` with the `MEMBER OF` idiom exactly as in R2.

**Rationale**:

- The defect is identical in kind, so an identical solution keeps the codebase uniform and lets the
  same migration helper, the same folding helper and the same test patterns serve both families.
- `list_id` is the join column already used by `torg_miracle_list_products`,
  `torg_power_list_products` and `torg_spell_list_products`, so the new tables follow the
  established convention without inventing a name.
- A `@ManyToOne` to a `Perk` entity is rejected for the same reason as in R1: FR-020 requires
  unresolvable references to survive, and a foreign key would reject them.

**Finding that shapes the scope**: `findByUnlockingPerk` exists on all three repositories but is
**not reachable from any port, service or controller** — no application code calls it today. The
lookup is therefore repaired at the repository level and verified by a data-layer test; there is no
API behaviour to regress and no new endpoint is introduced by this feature.

**Alternatives considered**:

- **Leave `unlockingPerk` single-valued and fix only the display**: Rejected. It would leave
  `findByUnlockingPerk` permanently wrong and contradicts the user's instruction to treat the field
  equivalently to `cosm`.

---

## R10: Mechanism for converting the prose fields

**Decision**: Implement the prose conversion as a Liquibase **`customChange`** — a Java class in
`torg-codex-data` implementing `liquibase.change.custom.CustomTaskChange` — rather than as SQL.

**Rationale**: Measurement of the shipped data shows SQL string splitting cannot do this safely:

| Property measured across the 97 encoded prose values | Count | Consequence for an SQL split |
|---|---|---|
| Statements containing `, ` (comma + space) | 12 values | A split on `, ` corrupts them |
| Values mixing single-quoted and double-quoted statements | 3 values | A single-quote-only pattern fails |
| Statements containing a literal `"` inside a single-quoted statement | 1 value | Naive quote matching fails |
| Statements containing an embedded newline | 1 value | Requires dot-matches-newline handling |
| Statements containing `', ` (apostrophe + comma + space) | 0 values | — |

A split on the three-character sequence `', ` would survive the first row of that table but still
break on the three mixed-quoting values. Getting this wrong silently rewrites prose that readers see,
and the damage is invisible in a diff of 97 rows. A real tokenizer that tracks the opening quote
character and scans to its matching close is short, obvious, and unit-testable in isolation.

This stays fully within ADR-012 and Principle IV: a `customChange` **is** a Liquibase changeset,
tracked in `DATABASECHANGELOG` with a checksum like any other, and it runs inside the same
`db-updater` execution.

**Alternatives considered**:

- **`regexp_replace` / `regexp_split_to_table` in SQL**: Rejected on the evidence above. It is the
  right tool for the cosm and unlocking-perk families (R4) because those values are slugs — no
  spaces, no commas, no apostrophes, uniform quoting — but prose is none of those things.
- **Convert at render time in the markup pipeline**: Rejected. It leaves the stored data wrong,
  violates FR-021, contradicts the user's decision, and adds a permanent cost to every render for a
  one-off data defect.
- **Fix the CSV files at source**: Rejected. It changes `loadData` checksums on already-applied
  changesets, and the separately delivered data set cannot be edited from this repository (FR-027).

---

## R11: Tokenizer semantics for the encoded prose values

**Decision**: The conversion recognises a value as encoded only when, after trimming, it starts with
`[` and ends with `]` **and** tokenizes cleanly. Tokenizing scans for statements delimited by either
`'` or `"`; the delimiter is whichever quote character opens the statement, and the statement ends at
the next occurrence of that same character. Anything that fails to tokenize is left untouched.

| Input | Result | Requirement |
|---|---|---|
| `['a', 'b']` | `- a\n- b` | FR-022 |
| `['only one']` | `only one` | FR-023 |
| `["alters user's voice", 'tests are Favored']` | `- alters user's voice\n- tests are Favored` | FR-025 |
| `['contains a, comma', 'b']` | `- contains a, comma\n- b` | FR-025 |
| `[]` | empty | edge case "Empty prose list" |
| `see [this table] for details` | unchanged | FR-024 |
| `- already\n- converted` | unchanged | FR-026 |

**Rationale**: Treating the opening quote as the delimiter is exactly how the values were produced,
so it round-trips every measured case, including the three mixed-quoting values and the value
containing `"oath breakers"` inside a single-quoted statement. Refusing to convert anything that does
not tokenize cleanly makes the change fail safe: an unrecognised value keeps its current, already
imperfect rendering rather than being mangled into something new.

Idempotency (FR-026) falls out of the same guard: a converted value no longer starts with `[`, so a
second run skips it.

**Alternatives considered**:

- **Accept a value as encoded purely by the `[`…`]` shape and split unconditionally**: Rejected. It
  converts prose that merely opens and closes with brackets, violating FR-024.

---

## R12: Markdown shape produced for prose fields

**Decision**: Two or more statements become a Markdown bullet list, one `- ` item per statement, in
the original order, joined by a single newline. Exactly one statement becomes that statement's text
verbatim, with no list marker. Zero statements become an empty value.

**Rationale**:

- The single-statement case is the majority (44 of 93 item values) and a one-item bullet list reads
  as a formatting accident, which is why the user chose plain text for it.
- Statement text is copied verbatim, so embedded markup and `<entity:...>` references keep working;
  the markup pipeline resolves references *before* CommonMark runs (ADR-011), and a bullet list is
  ordinary CommonMark that the pipeline already renders.
- The original order is preserved rather than sorted. Unlike cosm slugs, these are authored prose
  statements whose sequence carries meaning — a threat's two quotes are not interchangeable.

**Note on `quote`**: the three affected threat quotes become bullet lists like every other prose
field, per FR-022. This is uniform and matches the user's instruction; if bullets ever read poorly
for quotes specifically, that is a presentation change, not a data change, and can be revisited
without touching the stored values.
