---

description: "Implementation tasks for multi-cosm assignment and encoded-list repair"
---

# Tasks: Multi-Cosm Assignment for Catalog Entries

**Input**: Design documents from `/specs/004-multi-cosm-assignment/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Tests**: Automated tests are required by Constitution Principle VIII and are written before their corresponding implementation tasks.

**Organization**: Tasks are grouped by user story so that each story can be implemented and verified as an independent increment after the shared foundation is complete.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel because it changes different files and has no dependency on another incomplete task
- **[Story]**: Maps the task to a user story from `spec.md`
- Every task names the exact file or directory it changes

## Phase 1: Setup (Architecture and Migration Registration)

**Purpose**: Record the architecture decision and prepare the ordered Liquibase entry points before implementation.

- [ ] T001 Create ADR-018 for multi-valued slug references, the same-release migration exception, and encoded-list prose repair in `docs/modules/arc42/pages/09_architecture_decisions/018_multi-valued-references-and-encoded-list-repair.adoc`
- [ ] T002 [P] Register ADR-018 in `docs/modules/arc42/pages/09_architecture_decisions/_nav.adoc` and `docs/modules/arc42/pages/09_architecture_decisions/_include.adoc`
- [X] T003 Add the ordered `data-fix-lists.yaml` changelog after all data loads in `torg-codex-data/src/main/resources/db/changelog/db.changelog-master.yaml`

---

## Phase 2: Foundational (Shared Multi-Value Storage)

**Purpose**: Establish the shared domain and persistence representation required by every user story.

**Critical**: No user-story implementation starts until this phase is complete.

- [ ] T004 [P] Replace `String cosm` with `Set<String> cosms` in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/Item.java`, `Perk.java`, `Shard.java`, `Threat.java`, and `Vehicle.java`
- [ ] T005 [P] Replace `String cosm` with `Set<String> cosms` and `String unlockingPerk` with `Set<String> unlockingPerks` in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/MiracleList.java`, `PowerList.java`, and `SpellList.java`
- [ ] T006 [P] Add eager `cosms` element collections with explicit collection tables, join columns, value columns, and set semantics to `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/model/Item.java`, `Perk.java`, `Shard.java`, `Threat.java`, and `Vehicle.java`
- [ ] T007 [P] Add eager `cosms` and `unlockingPerks` element collections to `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/model/MiracleList.java`, `PowerList.java`, and `SpellList.java`
- [ ] T008 Update the eight MapStruct persistence mappers for collection-based domain/entity conversion in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/mapper/ItemEntityMapper.java`, `MiracleListEntityMapper.java`, `PerkEntityMapper.java`, `PowerListEntityMapper.java`, `ShardEntityMapper.java`, `SpellListEntityMapper.java`, `ThreatEntityMapper.java`, and `VehicleEntityMapper.java`
- [X] T009 Create the eight cosm and three unlocking-perk collection tables, composite primary keys, owner foreign keys with cascade delete, and slug indexes in `torg-codex-data/src/main/resources/db/changelog/data-fix-lists.yaml`
- [X] T010 Add generic, deduplicating backfill changesets for scalar and encoded-list cosm and unlocking-perk values in `torg-codex-data/src/main/resources/db/changelog/data-fix-lists.yaml`
- [X] T011 Add contract changesets that drop all eight legacy `cosm` columns and all three legacy `unlocking_perk` columns only after successful backfill in `torg-codex-data/src/main/resources/db/changelog/data-fix-lists.yaml`

**Checkpoint**: Domain records, JPA entities, MapStruct persistence mapping, and database storage consistently use slug sets; user-story work can proceed.

---

## Phase 3: User Story 1 — Find Multi-Cosm Content Through a Filter (Priority: P1) MVP

**Goal**: A catalog entry assigned to several cosms is returned by the filter for every assigned cosm.

**Independent Test**: Load catalog data and query a known multi-cosm perk by `core-earth` and a second assigned cosm; confirm it appears in both result sets, while unassigned and other-cosm entries do not.

### Tests for User Story 1

- [ ] T012 [P] [US1] Add PostgreSQL integration coverage for multi-cosm, single-cosm, absent-cosm, duplicate, and unused-cosm filtering in `torg-codex-data/src/test/java/de/paladinsinn/torg/codex/data/equivalence/PersistenceEquivalenceIT.java`
- [ ] T013 [P] [US1] Add API characterization cases proving one known entry is returned under each assigned cosm without changing censoring in `torg-codex/src/test/java/de/paladinsinn/torg/codex/characterization/CharacterizationReplayTest.java`

### Implementation for User Story 1

- [ ] T014 [US1] Replace scalar cosm predicates with `:cosm MEMBER OF entity.cosms` for all cosm queries, including combined product predicates, in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/repository/ItemRepository.java`, `MiracleListRepository.java`, `PerkRepository.java`, `PowerListRepository.java`, `ShardRepository.java`, `SpellListRepository.java`, `ThreatRepository.java`, and `VehicleRepository.java`
- [ ] T015 [US1] Verify unchanged `findByCosm(String)` port flow and adjust only persistence adapter mapping needed for collection-backed results in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/adapter/out/persistence/JpaItemPersistenceAdapter.java`, `JpaMiracleListPersistenceAdapter.java`, `JpaPerkPersistenceAdapter.java`, `JpaPowerListPersistenceAdapter.java`, `JpaShardPersistenceAdapter.java`, `JpaSpellListPersistenceAdapter.java`, `JpaThreatPersistenceAdapter.java`, and `JpaVehiclePersistenceAdapter.java`

**Checkpoint**: Cosm-filter behavior is independently functional and satisfies the P1 MVP.

---

## Phase 4: User Story 2 — Show Every Assigned Cosm (Priority: P2)

**Goal**: Existing API fields show zero, one, or several assigned cosms deterministically without exposing encoded lists or changing the DTO shape.

**Independent Test**: Map no cosms, one known cosm, one unknown cosm, and several mixed known/unknown cosms; verify the exact contract values and stable alphabetical ordering.

### Tests for User Story 2

- [ ] T016 [P] [US2] Create mapper tests for empty, single known, single unknown, multiple known, mixed unknown, duplicate, and iteration-order cases in `torg-codex/src/test/java/de/paladinsinn/torg/codex/api/mapper/TorgMappingSupportTest.java`
- [ ] T017 [P] [US2] Add response-contract replay assertions for unchanged single-cosm fields and readable multi-cosm fields in `torg-codex/src/test/java/de/paladinsinn/torg/codex/characterization/CharacterizationReplayTest.java`

### Implementation for User Story 2

- [ ] T018 [US2] Implement `Set<String>` cosm folding with slug resolution, raw unknown-slug fallback, alphabetical display sorting, comma joining, and null identity for multi-value results in `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/mapper/TorgMappingSupport.java`
- [ ] T019 [US2] Route all eight summary and detail mapper cosm fields through the collection folding helper in `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/mapper/ItemMapper.java`, `MiracleListMapper.java`, `PerkMapper.java`, `PowerListMapper.java`, `ShardMapper.java`, `SpellListMapper.java`, `ThreatMapper.java`, and `VehicleMapper.java`

**Checkpoint**: Multi-cosm output is readable and stable while the public DTO contract remains unchanged.

---

## Phase 5: User Story 3 — Render Encoded Prose as Markdown (Priority: P2)

**Goal**: Entire encoded-list prose values become plain text or Markdown lists without changing ordinary prose or embedded markup.

**Independent Test**: Convert representative single, multiple, empty, quoted, comma-containing, multiline, malformed, ordinary bracketed, and already-converted values twice and compare exact output.

### Tests for User Story 3

- [ ] T020 [P] [US3] Create tokenizer tests for all quote, escape, comma, newline, empty, malformed, partial-list, and already-Markdown cases in `torg-codex-data/src/test/java/de/paladinsinn/torg/codex/data/migration/EncodedListTokenizerTest.java`
- [ ] T021 [P] [US3] Create Liquibase custom-change tests for item additional features, threat quotes, spell-list notes, byte preservation, and second-run idempotency in `torg-codex-data/src/test/java/de/paladinsinn/torg/codex/data/migration/ProseListToMarkdownChangeTest.java`

### Implementation for User Story 3

- [ ] T022 [US3] Implement a complete-value encoded-list tokenizer that preserves statement contents and returns no match for malformed or partial literals in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/migration/EncodedListTokenizer.java`
- [ ] T023 [US3] Implement the Liquibase `CustomTaskChange` that converts one statement to plain text, multiple statements to source-ordered Markdown bullets, an empty list to empty text, and updates only changed rows in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/migration/ProseListToMarkdownChange.java`
- [X] T024 [US3] Register prose repair for `torg_item.additional_features`, `torg_threat.quote`, and `torg_spell_list.notes` after data load and reference backfill in `torg-codex-data/src/main/resources/db/changelog/data-fix-lists.yaml`

**Checkpoint**: All three prose fields are repaired in storage and continue through the existing markup pipeline unchanged.

---

## Phase 6: User Story 4 — Find Lists by Every Unlocking Perk (Priority: P3)

**Goal**: Miracle, power, and spell lists are found through every assigned unlocking-perk slug and expose deterministic comma-separated slugs through the existing field.

**Independent Test**: Query a multi-perk miracle list through each assigned slug and map zero, one, several, duplicate, and unknown assignments to their exact response strings.

### Tests for User Story 4

- [ ] T025 [P] [US4] Add PostgreSQL integration tests for lookup by each unlocking-perk assignment, unknown slugs, and duplicate suppression in `torg-codex-data/src/test/java/de/paladinsinn/torg/codex/data/equivalence/PersistenceEquivalenceIT.java`
- [ ] T026 [P] [US4] Extend folding tests for empty, single, multiple, duplicate, unknown, and iteration-order unlocking-perk sets in `torg-codex/src/test/java/de/paladinsinn/torg/codex/api/mapper/TorgMappingSupportTest.java`

### Implementation for User Story 4

- [ ] T027 [US4] Replace scalar unlocking-perk predicates with `:perk MEMBER OF entity.unlockingPerks` in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/repository/MiracleListRepository.java`, `PowerListRepository.java`, and `SpellListRepository.java`
- [ ] T028 [US4] Implement deterministic unlocking-perk slug folding for zero, one, and many assignments in `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/mapper/TorgMappingSupport.java`
- [ ] T029 [US4] Route miracle-list, power-list, and spell-list detail DTO fields through the unlocking-perk folding helper in `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/mapper/MiracleListMapper.java`, `PowerListMapper.java`, and `SpellListMapper.java`

**Checkpoint**: Multi-perk lookup and presentation work independently for all three list types.

---

## Phase 7: User Story 5 — Maintain Multi-Cosm Data Without Encoding Tricks (Priority: P3)

**Goal**: Fresh loads and upgrades create normalized, deduplicated assignments for public and proprietary data without manual intervention or repeat-run changes.

**Independent Test**: Apply the complete changelog to PostgreSQL, validate the collection tables and removed columns, then rerun against the migrated state and confirm no data changes.

### Tests for User Story 5

- [ ] T030 [US5] Extend full PostgreSQL migration coverage with collection-table schema, 62 cosm records, 5 unlocking-perk records, 97 prose values, unknown-reference preservation, removed legacy columns, and idempotency assertions in `torg-codex-data/src/test/java/de/paladinsinn/torg/codex/data/liquibase/LiquibaseImportIT.java`
- [ ] T031 [P] [US5] Update persisted-data comparison expectations for normalized cosm and unlocking-perk sets plus repaired prose in `torg-codex-data/src/test/java/de/paladinsinn/torg/codex/data/equivalence/PersistedDataSnapshotComparisonTest.java`

### Implementation for User Story 5

- [X] T032 [US5] Regenerate and review the Liquibase baseline checksums for the master file and consolidated changelog in `torg-codex-data/src/test/resources/liquibase-changelog-manifest.sha256`
- [ ] T033 [US5] Validate and, if required, harden backfill guards so fresh loads, upgrades, duplicate input values, unknown slugs, and repeated execution produce the same normalized state in `torg-codex-data/src/main/resources/db/changelog/data-fix-lists.yaml`

**Checkpoint**: The migration is safe for fresh and existing installations and needs no encoded source-data workaround.

---

## Phase 8: Polish & Cross-Cutting Verification

**Purpose**: Confirm compatibility, documentation alignment, performance, architecture boundaries, and the full delivery workflow.

- [X] T034 Recapture and review only contract-approved changes in `torg-codex/src/test/resources/characterization/`
- [ ] T035 [P] Update the persistence and domain class diagrams for set-valued cosm and unlocking-perk fields in `docs/modules/arc42/images/uml/classes/torg-codex-data.puml` and `docs/modules/arc42/images/uml/classes/torg-codex-domain.puml`
- [X] T036 [P] Add migration operation and compatibility notes to the relevant architecture documentation under `docs/modules/arc42/pages/`
- [ ] T037 Run the focused mapper, tokenizer, data-module, and Testcontainers checks documented in `specs/004-multi-cosm-assignment/quickstart.md`
- [ ] T038 Run `./mvnw clean verify` from the repository root and resolve compilation, Checkstyle, architecture-test, unit-test, integration-test, and Hibernate validation failures
- [ ] T039 Execute and record the manual API and SQL smoke checks from `specs/004-multi-cosm-assignment/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 — Setup**: Starts immediately. T001 precedes T002; T003 can proceed in parallel with the ADR work.
- **Phase 2 — Foundation**: Depends on Phase 1. T004–T007 can run in parallel; T008 depends on T004–T007; T009 precedes T010, which precedes T011.
- **US1 (Phase 3)**: Depends on Phase 2. Tests T012–T013 precede implementation T014–T015.
- **US2 (Phase 4)**: Depends on Phase 2. Tests T016–T017 precede T018–T019; it can run in parallel with US1.
- **US3 (Phase 5)**: Depends on Phase 2 and T003. Tests T020–T021 precede T022–T024; it can run in parallel with US1 and US2.
- **US4 (Phase 6)**: Depends on Phase 2. Tests T025–T026 precede T027–T029; it can run in parallel with US1–US3.
- **US5 (Phase 7)**: Depends on US1–US4 because it validates their combined migrated state. T030–T031 precede T032–T033.
- **Polish (Phase 8)**: Depends on all selected user stories. T034–T036 can run in parallel before sequential validation T037–T039.

### User Story Dependencies

```text
Setup -> Foundation -> US1 (P1) -------------------+
                    -> US2 (P2) -------------------|
                    -> US3 (P2) -------------------+-> US5 (P3) -> Polish
                    -> US4 (P3) -------------------|
```

- **US1**: No dependency on another story; this is the suggested MVP.
- **US2**: No dependency on another story after Foundation.
- **US3**: No dependency on another story after Foundation.
- **US4**: No dependency on another story after Foundation.
- **US5**: Validates and completes the combined migration delivered by US1–US4.

### Parallel Opportunities

- Domain record groups T004–T005 and JPA entity groups T006–T007 can be implemented concurrently.
- After Foundation, US1, US2, US3, and US4 can proceed concurrently in separate file groups, with coordination around shared test and mapper files.
- Within US1, T012 and T013 can run concurrently; within US2, T016 and T017 can run concurrently; within US3, T020 and T021 can run concurrently; within US4, T025 and T026 can run concurrently.
- Documentation tasks T035 and T036 can run concurrently with characterization review T034.

## Parallel Execution Examples

### User Story 1

```text
Task: T012 — PostgreSQL cosm-filter integration coverage
Task: T013 — API characterization filter coverage
```

### User Story 2

```text
Task: T016 — Unit tests for cosm folding
Task: T017 — Response-contract replay assertions
```

### User Story 3

```text
Task: T020 — Encoded-list tokenizer tests
Task: T021 — Prose custom-change tests
```

### User Story 4

```text
Task: T025 — Unlocking-perk persistence lookup tests
Task: T026 — Unlocking-perk folding tests
```

## Implementation Strategy

### MVP First

1. Complete Setup and Foundation.
2. Complete US1 tasks T012–T015.
3. Run the focused cosm-filter integration and characterization tests.
4. Demonstrate that one multi-cosm entry is returned under every assigned cosm.

### Incremental Delivery

1. Deliver US1 for correct discovery.
2. Add US2 for compatible, readable cosm presentation.
3. Add US3 for prose repair.
4. Add US4 for multi-perk lookup and presentation.
5. Complete US5 migration validation and cross-cutting verification.

## Notes

- Tests must be written first and observed failing for the intended reason before implementation.
- Applied Liquibase changesets and shipped CSV data files must remain unchanged.
- The same-release contract migration is the feature-specific exception documented by ADR-018; it does not weaken the general migration rule.
- Review characterization fixture diffs manually. Only multi-cosm visibility/presentation, multi-perk output, repaired prose, and migration-related row ordering may change.
- Commit with sign-off after each task or coherent task group as required by `CONTRIBUTING.md`.
