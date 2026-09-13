# Phase 1 Data Model: Multi-Cosm Assignment for Catalog Entries

**Feature**: 004-multi-cosm-assignment
**Date**: 2026-09-13 (extended to all three defect families)
**Depends on**: [research.md](./research.md)

---

## 1. Entities

### 1.1 Cosm (unchanged)

The reference data table `torg_cosm` is **not modified** by this feature.

| Attribute | Type | Notes |
|-----------|------|-------|
| `id` | UUID | Identity |
| `slug` | varchar(128), not null, unique | Stable reference key used for all filtering and comparison |
| `name` | text | Human-readable display name |
| `clearance_level` | varchar | Existing |

19 cosm rows exist. No cosm is added, merged, or removed.

### 1.2 Catalog Entry (8 types)

Every entry type that carries a cosm reference today. All eight are changed uniformly
(spec assumption: no mixed cardinality model).

| Entity | Table | Collection table | Join column |
|--------|-------|------------------|-------------|
| `Item` | `torg_item` | `torg_item_cosms` | `item_id` |
| `MiracleList` | `torg_miracle_list` | `torg_miracle_list_cosms` | `list_id` |
| `Perk` | `torg_perk` | `torg_perk_cosms` | `perk_id` |
| `PowerList` | `torg_power_list` | `torg_power_list_cosms` | `list_id` |
| `Shard` | `torg_shard` | `torg_shard_cosms` | `shard_id` |
| `SpellList` | `torg_spell_list` | `torg_spell_list_cosms` | `list_id` |
| `Threat` | `torg_threat` | `torg_threat_cosms` | `threat_id` |
| `Vehicle` | `torg_vehicle` | `torg_vehicle_cosms` | `vehicle_id` |

Collection table and join column names mirror the existing `torg_<entity>_products` tables exactly.

### 1.3 Cosm Assignment (new)

The association between a catalog entry and a cosm, stored as a slug value.

| Column | Type | Constraints |
|--------|------|-------------|
| `<entity>_id` | uuid | not null, FK → owning entity `id`, cascade delete |
| `cosm` | varchar(128) | not null |

- Primary key: `(<entity>_id, cosm)` — enforces FR-009 deduplication at the database level.
- The `cosm` value is a **slug**, never a display name.
- No foreign key to `torg_cosm`: unresolvable slugs must survive (FR-010).
- Index on `cosm` to support the filter query.

### 1.4 List Entry (3 types)

Every entry type that carries an unlocking-perk reference today. All three are changed uniformly
(spec assumption: no mixed cardinality model), even though encoded data occurs only in miracle lists.

| Entity | Table | Collection table | Join column |
|--------|-------|------------------|-------------|
| `MiracleList` | `torg_miracle_list` | `torg_miracle_list_unlocking_perks` | `list_id` |
| `PowerList` | `torg_power_list` | `torg_power_list_unlocking_perks` | `list_id` |
| `SpellList` | `torg_spell_list` | `torg_spell_list_unlocking_perks` | `list_id` |

### 1.5 Unlocking Perk Assignment (new)

The association between a list and a perk that unlocks it, stored as a perk slug.

| Column | Type | Constraints |
|--------|------|-------------|
| `list_id` | uuid | not null, FK → owning entity `id`, cascade delete |
| `perk` | varchar(128) | not null |

- Primary key: `(list_id, perk)` — enforces FR-020 deduplication at the database level.
- No foreign key to `torg_perk`: unresolvable slugs must survive (FR-020).
- Index on `perk` to support the lookup query.

### 1.6 Prose Field (repaired in place, no schema change)

| Entity | Table | Column | Encoded rows (public data) |
|--------|-------|--------|----------------------------|
| `Item` | `torg_item` | `additional_features` | 93 of 979 |
| `Threat` | `torg_threat` | `quote` | 3 of 1101 |
| `SpellList` | `torg_spell_list` | `notes` | 1 of 27 |

These stay single `text` columns. Only their **content** changes, from an encoded list to Markdown
(FR-021 to FR-027). No collection table, no entity change, no domain change.

---

## 2. Cardinality and invariants

| Rule | Source |
|------|--------|
| An entry holds 0..n cosm assignments | FR-001 |
| An assignment names exactly one cosm slug | FR-001 |
| The same cosm appears at most once per entry | FR-009, enforced by the composite PK |
| Assignments are an unordered set | Spec assumption (source order not meaningful) |
| No assigned cosm is "primary" | Spec assumption |
| A slug that matches no `torg_cosm.slug` is retained, not dropped | FR-010 |
| A list holds 0..n unlocking-perk assignments | FR-016 |
| The same perk appears at most once per list | FR-020, enforced by the composite PK |
| A perk slug that matches no known perk is retained, not dropped | FR-020 |
| A prose field holds formatted text, never a structured list | FR-021, Key Entities |
| A prose value that is not entirely an encoded list stays byte-identical | FR-024 |

---

## 3. Legacy columns

The existing single-valued `cosm` column (type `text`) on each of the eight catalog tables, and the
single-valued `unlocking_perk` column (type `varchar(128)`) on each of the three list tables, are
**dropped by this feature**, once the backfill has completed.

- Expand + Migrate + Contract in one deployment, by user decision (FR-014, FR-020, research R3).
- The contract changeset runs strictly after all migrate changesets, so a failed backfill aborts
  the deployment before any column is dropped (FR-015).
- The corresponding `String cosm` / `String unlockingPerk` fields are removed from the JPA entities
  and the domain records; nothing reads the legacy values afterwards.
- The prose columns are **not** dropped. They are repaired in place and keep their type.

---

## 4. Migration mapping

Applied per entity table, after `loadData`, in changelog `data-fix-lists.yaml`.

| Legacy `cosm` value | Resulting assignments |
|---------------------|-----------------------|
| `NULL` or empty | none |
| `core-earth` | `{core-earth}` |
| `['core-earth', 'tharkold']` | `{core-earth, tharkold}` |
| `['core-earth', 'core-earth']` | `{core-earth}` (PK collision absorbed) |
| `some-unknown-slug` | `{some-unknown-slug}` (retained, FR-010) |

Encoded values are detected by the pattern `[%]` and split generically (research R4), so
combinations that exist only in the proprietary data set are handled as well.

Public free-tier data set measurements:

| Entity | Encoded rows |
|--------|--------------|
| `Perk` | 58 |
| `MiracleList` | 3 |
| `PowerList` | 1 |
| **Total** | **62** |

20 distinct combinations; maximum 7 cosms on a single entry.

### 4.1 Unlocking-perk mapping

Identical rules, applied to the three list tables.

| Legacy `unlocking_perk` value | Resulting assignments |
|-------------------------------|-----------------------|
| `NULL` or empty | none |
| `miracles` | `{miracles}` |
| `['miracles', 'exemplar-of-light']` | `{exemplar-of-light, miracles}` |
| `unknown-perk` | `{unknown-perk}` (retained, FR-020) |

Public free-tier data set: 5 encoded rows, all on `MiracleList`; none on `PowerList` or `SpellList`.

### 4.2 Prose mapping

Applied in place by the `customChange` described in research R10/R11.

| Stored value | Resulting value |
|--------------|-----------------|
| `['a', 'b']` | `- a`⏎`- b` |
| `['only one']` | `only one` |
| `["alters user's voice", 'tests are Favored']` | `- alters user's voice`⏎`- tests are Favored` |
| `['contains a, comma', 'b']` | `- contains a, comma`⏎`- b` |
| `[]` | empty |
| `see [this table] for details` | unchanged (FR-024) |
| already-converted Markdown | unchanged (FR-026) |

Public free-tier data set: 97 encoded rows (93 items, 3 threats, 1 spell list), of which 45 hold a
single statement and therefore become plain text.

---

## 5. Domain model

Each domain record changes one component:

```text
- String cosm
+ Set<String> cosms     // cosm slugs, unordered, deduplicated
```

Applies to the domain records for `Item`, `MiracleList`, `Perk`, `PowerList`, `Shard`,
`SpellList`, `Threat`, `Vehicle`. The domain module stays free of Spring and JPA.

Additionally, the three list records change a second component:

```text
- String unlockingPerk
+ Set<String> unlockingPerks    // perk slugs, unordered, deduplicated
```

Applies to `MiracleList`, `PowerList`, `SpellList`. Prose fields stay `String` and are unchanged.

---

## 6. Presentation projection

The externally visible `cosm` field keeps its existing shape (FR-013). Folding rules:

| Assignments | `cosm.id` | `cosm.name` |
|-------------|-----------|-------------|
| none | — field is absent/null | — |
| one, resolvable | the cosm's id | the cosm's display name |
| one, unresolvable | `null` | the raw slug |
| several | `null` | display names joined with `", "`, sorted alphabetically |

For the multi-cosm case, an unresolvable slug contributes its raw slug value to the joined string,
so nothing is lost (FR-010).

Sorting is alphabetical by the contributed display string, giving the deterministic and stable
ordering required by FR-011 and SC-005.

### 6.1 Unlocking-perk projection

The `unlockingPerk` field on the three list detail DTOs keeps its existing shape and type (`String`)
per FR-018. Unlike cosms, the values are **never resolved to display names** (FR-019).

| Assignments | `unlockingPerk` |
|-------------|-----------------|
| none | `null` |
| one | that perk's slug, byte-identical to today |
| several | the slugs joined with `", "`, sorted alphabetically |

The single-assignment case is exactly today's value, which is what keeps existing consumers working
and what SC-009 verifies.

### 6.2 Prose projection

None. Prose fields are repaired in storage, so the presentation layer is unchanged and keeps calling
the existing markup rendering. The rendered output changes only because the stored Markdown changed.

---

## 7. State transitions

None. Catalog entries and cosm assignments are read-only reference data loaded by migration; there
is no lifecycle or workflow state.

---

## 8. Out of scope

- Repairing slugs that match no known cosm or perk — they are preserved and surfaced only.
- Any change to product ownership, censoring, or clearance-level handling (FR-012).
- Adding an `?unlockingPerk=` query filter; the repository method exists but is unreachable from any
  port (research R9).
- Repairing prose values that are not entirely an encoded list (FR-024).
- Any change to the markup rendering pipeline itself; only stored prose content changes.
