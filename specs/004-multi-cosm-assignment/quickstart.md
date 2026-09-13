# Quickstart: Validating Multi-Cosm Assignment

**Feature**: 004-multi-cosm-assignment
**Date**: 2026-09-13 (extended to all three defect families)

How to prove the feature works end-to-end. See [spec.md](./spec.md) for requirements,
[data-model.md](./data-model.md) for the schema, and
[contracts/cosm-reference-api.md](./contracts/cosm-reference-api.md) plus
[contracts/list-and-prose-fields.md](./contracts/list-and-prose-fields.md) for the API contracts.

---

## Prerequisites

- JDK 25 at `/usr/lib/jvm/temurin-25-jdk-amd64`
- A running Docker daemon (Testcontainers-backed data tests and `LiquibaseImportIT`)
- Maven wrapper from the repository root

```bash
export JAVA_HOME=/usr/lib/jvm/temurin-25-jdk-amd64
```

> **Note**: `./mvnw -pl torg-codex test` without `-am` resolves stale sibling jars from `~/.m2`
> and fails with misleading "package does not exist" errors. Always use `-am`, or pre-install the
> dependency modules first.

---

## Step 1 — Build and static gates

```bash
./mvnw -DskipTests install
```

**Expected**: `BUILD SUCCESS`, and Checkstyle reports zero violations. Checkstyle runs in
`process-sources`.

---

## Step 2 — Migration integrity

```bash
./mvnw -pl torg-codex-data test
```

**Expected**:

- `LiquibaseChangelogGuardTest` passes. It fails until the baseline manifest
  `torg-codex-data/src/test/resources/liquibase-changelog-manifest.sha256` is regenerated for the
  new changelog file and the modified master changelog.
- `PersistedDataSnapshotComparisonTest` passes.

Then apply the changelog against real PostgreSQL:

```bash
./mvnw -pl torg-codex-data -Dit.test=LiquibaseImportIT verify
```

**Expected**: the changelog applies cleanly. Validates FR-006.

**Idempotency (FR-007)**: re-running the import must leave the collection tables unchanged — the
migrate changesets are guarded against duplicate inserts.

---

## Step 3 — Migration result in the database

Against a database that has the changelog applied:

```sql
-- No encoded value survives in any collection (SC-001, FR-005)
SELECT count(*) FROM torg_perk_cosms WHERE cosm LIKE '[%' OR cosm LIKE '%]';
-- expected: 0

-- The 62 previously broken rows are expanded (public free-tier data set)
SELECT count(*) FROM (
  SELECT perk_id FROM torg_perk_cosms GROUP BY perk_id HAVING count(*) > 1
) multi;                                                      -- expected: 58

-- The legacy column is gone from all eight tables (SC-008, FR-014)
SELECT count(*) FROM information_schema.columns
 WHERE column_name = 'cosm'
   AND table_name IN ('torg_item', 'torg_miracle_list', 'torg_perk', 'torg_power_list',
                      'torg_shard', 'torg_spell_list', 'torg_threat', 'torg_vehicle');
-- expected: 0

-- A known multi-cosm entry carries all its cosms
SELECT cosm FROM torg_perk_cosms
 WHERE perk_id = '959f00d3-1692-4d2f-8682-407dd5214b1a' ORDER BY cosm;
-- expected: aysle, core-earth, cyberpapacy, living-land, nile-empire, orrorsh

-- Single-cosm entries are carried over exactly once (FR-008)
SELECT count(*) FROM torg_vehicle v
 WHERE (SELECT count(*) FROM torg_vehicle_cosms c WHERE c.vehicle_id = v.id) <> 1
   AND v.cosm IS NOT NULL AND v.cosm <> '';
-- expected: 0
```

### 3.1 Unlocking-perk collections

```sql
-- No encoded value survives (SC-009, FR-017)
SELECT count(*) FROM torg_miracle_list_unlocking_perks
 WHERE perk_slug LIKE '[%' OR perk_slug LIKE '%]';
-- expected: 0   (repeat for torg_power_list_unlocking_perks, torg_spell_list_unlocking_perks)

-- The 5 previously broken miracle lists are expanded (public free-tier data set)
SELECT count(*) FROM (
  SELECT list_id FROM torg_miracle_list_unlocking_perks
   GROUP BY list_id HAVING count(*) > 1
) multi;
-- expected: 5

-- The legacy columns are gone from all three tables (FR-020)
SELECT count(*) FROM information_schema.columns
 WHERE column_name = 'unlocking_perk'
   AND table_name IN ('torg_miracle_list', 'torg_power_list', 'torg_spell_list');
-- expected: 0

-- MEMBER OF style lookup returns the expanded entries
SELECT count(DISTINCT list_id) FROM torg_miracle_list_unlocking_perks
 WHERE perk_slug = 'miracles';
-- expected: strictly greater than the pre-migration count
```

### 3.2 Prose fields

```sql
-- No prose value is still an encoded list (SC-011, FR-021)
SELECT count(*) FROM torg_item   WHERE additional_features LIKE '[%]';
SELECT count(*) FROM torg_threat WHERE quote            LIKE '[%]';
SELECT count(*) FROM torg_spell_list WHERE notes        LIKE '[%]';
-- expected: 0, 0, 0

-- A multi-statement value became a bullet list (FR-022)
SELECT additional_features FROM torg_item WHERE additional_features LIKE '- %' LIMIT 1;
-- expected: lines each starting with '- ', newline separated

-- A single-statement value became plain text, no bullet (FR-023)
-- pick a row known to have held a one-element list and assert it does NOT start with '- '

-- Values that merely contain brackets were left untouched (FR-024)
-- spot-check a description containing '[' in the middle against the CSV source
```

**Idempotency check (FR-026)**: re-run the changelog against the already-migrated database. The
`customChange` must report zero rows changed and the counts above must be unchanged.

---

## Step 4 — Unit coverage for the presentation folding

```bash
./mvnw -pl torg-codex -am -Dtest=TorgMappingSupportTest -Dsurefire.failIfNoSpecifiedTests=false test
```

**Expected**: all cases green, covering the table in
[contracts §3.1](./contracts/cosm-reference-api.md):

| Case | Expected `cosm` |
|------|-----------------|
| no cosms | `null` |
| one known slug | id + display name |
| one unknown slug | `id = null`, name = raw slug |
| several known slugs | `id = null`, alphabetical display names joined with `", "` |
| several, one unknown | unknown slug contributes its raw text, nothing dropped |
| duplicate slug | appears once |
| same set, different iteration order | identical output (FR-011, SC-005) |

Then the unlocking-perk folding and the prose tokenizer:

```bash
./mvnw -pl torg-codex -am -Dtest=TorgMappingSupportTest -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -pl torg-codex-data -Dtest=EncodedListTokenizerTest test
```

**Expected** for the unlocking-perk folding (see
[list-and-prose contracts §2.1](./contracts/list-and-prose-fields.md)):

| Case | Expected `unlockingPerk` |
|------|--------------------------|
| none | `null` |
| one | that slug verbatim, never a display name |
| several | slugs sorted, joined with `", "` |
| duplicate slug | appears once |

**Expected** for the tokenizer (see research R11):

| Input | Expected |
|-------|----------|
| `['a', 'b']` | `- a\n- b` |
| `['only one']` | `only one` |
| `["has 'inner' quote", 'b']` | both statements intact |
| `['contains a, comma', 'b']` | comma preserved inside statement one |
| `['multi\nline']` | newline preserved |
| `[]` | empty |
| `see [table] for details` | unchanged |
| `- already\n- converted` | unchanged |
| unterminated `['a` | unchanged (fails to tokenize) |

---

## Step 5 — End-to-end API behaviour

```bash
./mvnw -pl torg-codex -am test
```

**Expected**: the full `torg-codex` suite is green, including `CharacterizationReplayTest`
(118 fixtures) and `CensoringDifferentialTest`.

Fixtures were captured against the old behaviour and must be recaptured once:

```bash
./mvnw -pl torg-codex -Dtest=CharacterizationFixtureCaptureTest \
  -Dcharacterization.capture=true -Dsurefire.failIfNoSpecifiedTests=false test
```

**Review the fixture diff before committing.** Acceptable changes are limited to:

- the 62 affected entries gaining a readable, comma-separated cosm
- those entries newly appearing in cosm-filtered fixtures
- the 5 affected miracle lists gaining a comma-separated `unlockingPerk` slug list
- the 97 prose values changing from encoded literals to Markdown or plain text
- row ordering shifts caused by the migration rewriting rows

**Unacceptable** (would violate SC-003 / SC-005a / SC-010 / SC-012):

- any single-cosm entry whose `cosm` object changes
- any single-perk entry whose `unlockingPerk` string changes
- any prose value that was not an encoded list changing at all
- any entry disappearing from a filter result it previously matched

---

## Step 6 — Manual smoke test

Start the application and exercise the contract. `local-requests.http` in `torg-codex/` holds
ready-made requests.

```bash
# Multi-cosm entry is now found under each of its cosms (US1, SC-002)
curl -s 'http://localhost:8080/api/perks?cosm=core-earth' | jq '.[] | select(.name=="Conviction")'
curl -s 'http://localhost:8080/api/perks?cosm=orrorsh'    | jq '.[] | select(.name=="Conviction")'
```

**Expected**: the entry is returned by both, with
`"cosm": {"id": null, "name": "Aysle, Core Earth, Cyberpapacy, Living Land, Nile Empire, Orrorsh"}`.

```bash
# Single-cosm behaviour is untouched (FR-008)
curl -s 'http://localhost:8080/api/vehicles?cosm=core-earth' | jq '.[0].cosm'
```

**Expected**: `{"id": "f56ffdc1-8637-4ebb-835f-4a39e24f21da", "name": "Core Earth"}`.

```bash
# Display names must not match; slugs only (FR-002)
curl -s 'http://localhost:8080/api/perks?cosm=Core%20Earth' | jq 'length'
```

**Expected**: `0`.

```bash
# No encoded value anywhere (SC-004)
curl -s 'http://localhost:8080/api/perks' | grep -c "\['"
```

**Expected**: `0`.

```bash
# Unlocking perk reads as slugs, never display names (US4, FR-019)
curl -s 'http://localhost:8080/api/miracle-lists' | jq -r '.[].id' | while read -r id; do
  curl -s "http://localhost:8080/api/miracle-lists/$id" | jq -r '.unlockingPerk // empty'
done | sort -u
```

**Expected**: only slug-shaped values (lowercase, hyphenated), some of them comma-separated; no
value containing `[`, `'` or an uppercase display name.

```bash
# Prose fields render as Markdown (US5, SC-011)
curl -s 'http://localhost:8080/api/items' | jq -r '.[].id' | head -50 | while read -r id; do
  curl -s "http://localhost:8080/api/items/$id" | jq -r '.additionalFeatures // empty'
done | grep -c "^\["
```

**Expected**: `0`, and spot-checked multi-statement values start with `- `.

---

## Step 7 — Full verification gate

```bash
./mvnw clean verify
```

**Expected**: `BUILD SUCCESS`. Required by the constitution before merging.

---

## Done criteria

| Check | Requirement |
|-------|-------------|
| Zero encoded values in collections and responses | SC-001, SC-004 |
| 62 entries reachable under each of their cosms | SC-002 |
| No previously matching entry lost | SC-003 |
| Repeated requests byte-identical | SC-005 |
| Single-cosm entries byte-identical to before | SC-005a |
| Zero encoded values in `unlockingPerk` | SC-009 |
| Single-perk entries byte-identical to before | SC-010 |
| Zero encoded values in prose fields | SC-011 |
| Non-list prose values byte-identical to before | SC-012 |
| Legacy `cosm` and `unlocking_perk` columns dropped | SC-008, FR-014, FR-020 |
| Migration runs unattended and repeatably | SC-007, FR-006, FR-007, FR-026 |
| ADR-018 written and registered | ADR-001 |
| `./mvnw clean verify` green | Constitution quality gate |
