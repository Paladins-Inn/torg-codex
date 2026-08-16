---

description: "Implementation tasks for unified product-ownership censoring authorization"
---

# Tasks: Unify Product-Ownership Censoring Authorization

**Input**: Design documents from `/specs/001-unify-censoring-authorization/`

**Prerequisites**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/product-ownership-resolver.md`, `contracts/censoring-single-mechanism-invariant.md`, `quickstart.md`, `checklists/requirements.md`, and `.specify/memory/constitution.md` Principle V

**Tests**: Tests are first-class for this bug fix. Every production-code change is preceded by or paired with a corresponding test task, and differential output assertions are required by Principle V.

**Organization**: Work is grouped into dependency-ordered phases and mapped to the three feature user stories. The P1 stories establish the bug fix and regression safety net; the P2 story removes the duplicate mechanism and documents the single-mechanism invariant.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel because it touches disjoint files and has no dependency on incomplete work.
- **[Story]**: Required for user-story tasks; `[US1]` = owner-visible content, `[US2]` = single mechanism, `[US3]` = differential regression coverage.
- Every task includes an exact repository path or an exact command path.

## Path Conventions

- `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/security/` — security/censoring adapter production code
- `torg-codex/src/test/java/de/paladinsinn/torg/codex/api/security/` — ownership resolver unit tests
- `torg-codex/src/test/java/de/paladinsinn/torg/codex/characterization/` — live characterization and differential tests
- `torg-codex/src/test/resources/characterization/cosms/` — Aysle cosm response fixtures
- `docs/modules/arc42/pages/08_concepts/torg_data/role-mapping.adoc` — security-to-product documentation

---

## Phase 1: Setup and Baseline Verification

**Purpose**: Confirm the current four-module reactor is green and record the pre-change state before any implementation task begins.

- [X] T001 Run the required baseline build from repository root with `JAVA_HOME=/usr/lib/jvm/temurin-25-jdk-amd64 ./mvnw clean verify`; record that the current baseline is green before changing `torg-codex/src/main/java/` or `torg-codex/src/test/java/`.
- [X] T002 [P] Inspect and record the current ownership/censoring call graph in `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/security/CurrentUserCensorFactory.java`, `torg-codex/src/main/java/de/paladinsinn/torg/codex/markup/SecuredMarkupService.java`, and the 15 gated controllers under `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/controller/`.
- [X] T003 [P] Confirm the pre-fix byte-identical characterization state by comparing `torg-codex/src/test/resources/characterization/cosms/anonymous-detail.json` and `torg-codex/src/test/resources/characterization/cosms/owner-detail.json`, and record the Aysle gated-field/id baseline for later verification.

**Checkpoint**: The repository has a confirmed green baseline and the known defect is reproducible/documented without implementation changes.

---

## Phase 2: Foundational Tests and Invariants

**Purpose**: Establish failing tests and enforce the architecture contract before changing production ownership resolution.

- [ ] T004 [P] [US2] Add an architectural single-mechanism test in `torg-codex/src/test/java/de/paladinsinn/torg/codex/architecture/CensoringSingleMechanismArchitectureTest.java` that limits production `SecurityContextHolder`/`GrantedAuthority` ownership derivation to `ProductOwnershipResolver` and its sole `CurrentUserCensorFactory` caller, and rejects `SecuredMarkupService` references.
- [ ] T005 [P] [US3] Add the live differential-test skeleton in `torg-codex/src/test/java/de/paladinsinn/torg/codex/characterization/CensoringDifferentialTest.java` for anonymous and `ROLE_sourcebook-aysle` MockMvc requests to the Aysle cosm detail endpoint; assert the expected owner-only and upsell substrings and unequal `worldLaws`, and verify it fails against the current defect.
- [ ] T006 [P] [US1] Create the new resolver unit-test file `torg-codex/src/test/java/de/paladinsinn/torg/codex/api/security/ProductOwnershipResolverTest.java`, porting the four `SecuredMarkupServiceTest` scenarios from `torg-codex/src/test/java/de/paladinsinn/torg/codex/markup/SecuredMarkupServiceTest.java`: no authentication → empty set, anonymous `ROLE_ANONYMOUS` → `ANONYMOUS`, generic authenticated principal with product roles → stripped product ids, and mixed `ROLE_`/`SCOPE_` authorities → only roles; use generic Spring Security principals rather than `DriveThruUserDetails`, and make the tests fail before implementation.
- [ ] T007 Run the focused pre-implementation tests from repository root with `./mvnw -pl torg-codex test -Dtest=ProductOwnershipResolverTest,CensoringDifferentialTest,CensoringSingleMechanismArchitectureTest`; confirm the new tests fail for the known defect while the existing suite remains the baseline from T001.

**Checkpoint**: Resolver behavior, live differential behavior, and the single-mechanism invariant are specified by failing tests before production rewiring.

---

## Phase 3: User Story 1 — Product Owner Sees Full Gated Content (Priority: P1) 🎯 MVP

**Goal**: Resolve ownership from generic `ROLE_<codex-id>` authorities so any supported principal type receives the correct gated content.

**Independent Test**: `ProductOwnershipResolverTest` passes all four ported scenarios, and `CensoringDifferentialTest` proves that an owner and anonymous Aysle response contain opposite gated variants and differ in `worldLaws`.

### Tests for User Story 1

> **TDD rule**: T006 is written and failing before the implementation below; rerun it after each production change.

- [ ] T008 [US1] Refine `torg-codex/src/test/java/de/paladinsinn/torg/codex/api/security/ProductOwnershipResolverTest.java` with explicit null-authentication, unauthenticated-authentication, unknown/stale `ROLE_` id, repeated-resolution/idempotence, and no-I/O expectations from `contracts/product-ownership-resolver.md`.
- [ ] T009 [US1] Add focused factory integration coverage in `torg-codex/src/test/java/de/paladinsinn/torg/codex/api/security/CurrentUserCensorFactoryTest.java` proving `CurrentUserCensorFactory.create()` passes resolver-owned product ids into `Censor.of(...)` and does not use `DriveThruUserService`; keep this test paired with the factory production change.

### Implementation for User Story 1

- [ ] T010 [US1] Implement the single `ProductOwnershipResolver` component in `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/security/ProductOwnershipResolver.java` with the contract's `Set<String> resolve()` API, reading the current `SecurityContextHolder` authentication, returning an empty set for absent/unauthenticated authentication, stripping only the literal `ROLE_` prefix, filtering non-role authorities, and returning an unmodifiable result without I/O.
- [ ] T011 [US1] Rewire `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/security/CurrentUserCensorFactory.java` to inject/use `ProductOwnershipResolver` for `Censor.of(markupService, ownedProducts)`, remove the `DriveThruUserService` field/import and principal-specific ownership lookup, and preserve the existing `Censor` and markup pipeline contract.
- [ ] T012 Run `./mvnw -pl torg-codex test -Dtest=ProductOwnershipResolverTest,CurrentUserCensorFactoryTest`; confirm the tests from T006/T008/T009 now pass and that the implementation remains principal-type-independent.

**Checkpoint**: The live censor factory uses the generic role resolver, while all existing controller call sites remain unchanged.

---

## Phase 4: User Story 2 — One Unambiguous Censoring Mechanism (Priority: P2)

**Goal**: Remove the dead, duplicate ownership-resolution service and leave exactly one production mechanism.

**Independent Test**: The architecture invariant passes, no Java source references `SecuredMarkupService`, and the 15 gated controllers still use `CurrentUserCensorFactory` only.

### Tests for User Story 2

- [ ] T013 [P] [US2] Extend `torg-codex/src/test/java/de/paladinsinn/torg/codex/architecture/CensoringSingleMechanismArchitectureTest.java` to verify the allowed resolver/factory package and the absence of independent ownership derivation outside it, including the expected 15-controller dependency shape under `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/controller/`.
- [ ] T014 [US2] Run the architecture test from `torg-codex/src/test/java/de/paladinsinn/torg/codex/architecture/CensoringSingleMechanismArchitectureTest.java` against the pre-deletion and post-deletion source states, ensuring it fails for any remaining duplicate implementation and passes for the target state.

### Implementation for User Story 2

- [ ] T015 [US2] Remove the obsolete production duplicate `torg-codex/src/main/java/de/paladinsinn/torg/codex/markup/SecuredMarkupService.java` after its resolver scenarios are covered by `torg-codex/src/test/java/de/paladinsinn/torg/codex/api/security/ProductOwnershipResolverTest.java`; do not replace it with another independent ownership path.
- [ ] T016 [US2] Remove the obsolete test `torg-codex/src/test/java/de/paladinsinn/torg/codex/markup/SecuredMarkupServiceTest.java` after porting its four ownership scenarios, and remove any now-invalid imports or references in test sources.
- [ ] T017 [US2] Update `docs/modules/arc42/pages/08_concepts/torg_data/role-mapping.adoc` to document `CurrentUserCensorFactory → ProductOwnershipResolver → Censor → TorgMarkupService`, remove the fabricated `SecuredMarkupService` controller examples, and preserve the documented role-prefix and rendering-order semantics.
- [ ] T018 Run repository searches and the focused build to verify the invariant: `grep -rl "SecuredMarkupService" --include=*.java .` returns no Java references, the 15 gated controllers under `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/controller/` still use `CurrentUserCensorFactory`, and `./mvnw -pl torg-codex test -Dtest=CensoringSingleMechanismArchitectureTest,ProductOwnershipResolverTest` passes.

**Checkpoint**: There is one role-based ownership mechanism, no dead duplicate class/test, no controller rewiring, and the architecture documentation matches production.

---

## Phase 5: User Story 3 — Regression-Proof Differential Coverage (Priority: P1)

**Goal**: Make the corrected owner-vs-anonymous behavior impossible to hide behind byte-identical characterization snapshots.

**Independent Test**: Live Aysle MockMvc responses differ in the gated field with owner-only/upsell assertions, and regenerated fixtures are no longer byte-identical.

### Tests and Fixture Verification for User Story 3

- [ ] T019 [US3] Complete `torg-codex/src/test/java/de/paladinsinn/torg/codex/characterization/CensoringDifferentialTest.java` using the existing characterization support/auth variant conventions, issuing anonymous and generic-principal `ROLE_sourcebook-aysle` requests to `/api/cosms/6cf031c3-ab0a-4d12-9173-91d74f7c809f`, extracting `worldLaws`, and asserting owner-only text, anonymous upsell text, and a genuine output difference.
- [ ] T020 [US3] Run `./mvnw -pl torg-codex verify -Dit.test=CensoringDifferentialTest,CharacterizationReplayTest` and confirm the dedicated differential test catches a reintroduced factory defect rather than merely replaying stored fixtures.
- [ ] T021 [US3] Regenerate the Aysle characterization fixtures by running `./mvnw -pl torg-codex test -Dtest=CharacterizationFixtureCaptureTest -Dcharacterization.capture=true`, updating `torg-codex/src/test/resources/characterization/cosms/anonymous-detail.json` and `torg-codex/src/test/resources/characterization/cosms/owner-detail.json` (and only any additionally discovered DRM-sensitive detail fixtures).
- [ ] T022 [US3] Verify the regenerated fixture pair in `torg-codex/src/test/resources/characterization/cosms/anonymous-detail.json` and `torg-codex/src/test/resources/characterization/cosms/owner-detail.json` is no longer byte-identical, contains the expected opposite gated variants, and preserves non-gated response content; investigate and reject unexpected fixture changes under other `torg-codex/src/test/resources/characterization/` areas.
- [ ] T023 [US3] Run `./mvnw -pl torg-codex verify -Dit.test=CharacterizationReplayTest,CensoringDifferentialTest` after fixture regeneration and confirm replay locks in the corrected snapshots while the explicit differential assertions remain the regression detector.

**Checkpoint**: Aysle owner and anonymous outputs demonstrably differ, fixtures encode the corrected behavior, and replay plus direct assertions pass.

---

## Phase 6: Final Full-Suite Verification and Polish

**Purpose**: Confirm all requirements, unchanged pipeline/data boundaries, and full reactor health after the complete feature.

- [ ] T024 [P] Run a production-source audit over `torg-codex/src/main/java/de/paladinsinn/torg/codex/` and `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/` confirming no `Censor`, mapper, controller, JPA/entity, or repository bypasses the resolver, and that markup order remains conditional blocks → entity references → raw HTML → game tokens → CommonMark.
- [ ] T025 [P] Run a persistence/contract audit confirming no files changed under `torg-codex-data/src/main/resources/db`, no JPA/domain/business-logic files changed outside the planned security layer, and no REST DTO/controller signatures or media types changed; record the audit against `FR-006`, `FR-009`, and `FR-010`.
- [ ] T026 [P] Run the quickstart validation commands from `specs/001-unify-censoring-authorization/quickstart.md`, including the single-mechanism grep and 15-controller count, and record the expected results.
- [ ] T027 Run the required final build from repository root with `JAVA_HOME=/usr/lib/jvm/temurin-25-jdk-amd64 ./mvnw clean verify`; confirm the four-module reactor, unit tests, architecture tests, Testcontainers/Failsafe integration tests, characterization replay, and differential test all pass.
- [ ] T028 Record final implementation evidence and requirement traceability in `specs/001-unify-censoring-authorization/tasks.md`, including the actual fixture-diff result and any explicitly justified non-changes under `torg-codex-data/`.

**Checkpoint**: The complete build is green, the single-mechanism invariant is verified, differential behavior is tested, and no prohibited schema/pipeline/API changes were introduced.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1**: No feature-task dependency; T001 must complete before implementation begins.
- **Phase 2**: Depends on Phase 1; T004–T006 may be prepared in parallel, then T007 establishes the failing-test baseline.
- **Phase 3 / US1**: Depends on Phase 2; T008–T009 refine tests before T010–T011 production changes, then T012 verifies them.
- **Phase 4 / US2**: Depends on the resolver/factory target from Phase 3; deletion tasks T015–T016 follow the ported tests and T013–T014 invariant coverage.
- **Phase 5 / US3**: Depends on the corrected factory and duplicate removal; T019–T023 must run before final verification.
- **Phase 6**: Depends on all prior phases; T027 is the final merge gate.

### User Story Dependencies

- **US1 (P1)**: Starts after Phase 2; delivers the core ownership bug fix and is the MVP.
- **US2 (P2)**: Depends on US1's resolver/factory wiring so the duplicate can be removed safely; independently verifies the single mechanism.
- **US3 (P1)**: Depends on US1's live behavior and uses the unchanged controller/fixture infrastructure; it completes the regression-proof acceptance gate.

### Parallel Opportunities

- T002 and T003 can run in parallel after T001.
- T004, T005, and T006 touch disjoint test files and can be written in parallel; T007 waits for all three.
- T008 and T009 can be prepared in parallel before T010/T011.
- T013 can be edited while T015/T016 are prepared only if the same architecture-test file is not concurrently edited; otherwise execute T013 then T014 sequentially.
- T024, T025, and T026 are disjoint final audits and can run in parallel.

---

## Implementation Strategy

### MVP First (US1)

1. Complete Phase 1 baseline and Phase 2 failing tests.
2. Implement T010–T011 and pass T012.
3. Validate the Aysle differential behavior from T019 as soon as the factory is wired.
4. Stop at the US1 checkpoint only after owner and anonymous outputs differ.

### Incremental Delivery

1. Add US2 by deleting the dead duplicate and aligning architecture documentation.
2. Add US3 by completing the live differential test and regenerating the Aysle fixtures.
3. Run all audits and the final `JAVA_HOME=/usr/lib/jvm/temurin-25-jdk-amd64 ./mvnw clean verify`.

### Constitution and Requirement Coverage

| Requirement | Covering task IDs |
|---|---|
| FR-001 | T006, T008, T010, T011 |
| FR-002 | T004, T013, T014, T018 |
| FR-003 | T013, T018, T024 |
| FR-004 | T005, T019, T020 |
| FR-005 | T005, T006, T019 |
| FR-006 | T024, T025 |
| FR-007 | T015, T016, T018 |
| FR-008 | T005, T019, T021, T022, T023 |
| FR-009 | T022, T024, T025 |
| FR-010 | T025, T027 |
| FR-011 | T004, T005, T013, T019, T027 |

| Success criterion | Covering task IDs |
|---|---|
| SC-001 | T019, T021, T022, T023 |
| SC-002 | T004, T013, T014, T015, T016, T018 |
| SC-003 | T022, T024, T025, T027 |
| SC-004 | T020, T023, T027 |
| SC-005 | T008, T010, T024 |

## Notes

- All tasks are unchecked because this file is an executable implementation plan, not an implementation record.
- Production-code changes are explicitly paired with tests: T010 with T006/T008, T011 with T009, and T015 with T013/T014 plus the ported resolver tests.
- No implementation, production edit, fixture regeneration, or `/speckit.implement` execution is part of task generation.
