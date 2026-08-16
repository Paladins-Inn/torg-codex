---
description: "Task list for 003-data-persistence-boundary (consolidates feature 002)"
---

# Tasks: Enforce Data-Persistence-Only Module Boundary
*(Consolidated: Feature 002 Markup-to-Domain as Phase A, Feature 003 Architecture Enforcement as Phase B)*

**Input**: `specs/003-data-persistence-boundary/` — plan.md, spec.md, research.md, data-model.md, quickstart.md, contracts/data-persistence-boundary-rule.md, `.specify/memory/constitution.md`

**Branch**: `003-data-persistence-boundary`

**Tech stack**: Java 25, Spring Boot 4.0.6, Maven 4 wrapper, JUnit 5, ArchUnit 1.4.1, `org.commonmark:commonmark:0.24.0`, 4-module Maven reactor

**Format**: `[ID] [P?] [Story?] Description — file path`
- `[P]` = parallelizable (different files, no incomplete dependencies)
- `[US1]`/`[US2]`/`[US3]` = maps to user story from spec.md

**Critical starting-state invariants** *(do not undo concurrent/user worktree changes)*:
- ADR-016 code move is **complete**: `de.paladinsinn.security.*` and `de.paladinsinn.drivethru.*` live exclusively in `torg-codex-application`.
- `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/markup/` **still exists** — Phase A (markup relocation) is pending.
- `torg-codex-domain` has **no** `markup` package — Phase A is pending.
- `DataPersistenceBoundaryArchitectureTest` **does not exist** — Phase B is pending.
- Two stale tests in `AdapterConventionArchitectureTest` are **still failing** — Phase B is pending.
- `torg-codex-domain/pom.xml` does **not** declare `commonmark` yet — Task T002 adds it.
- Constitution is at **v2.0.0** (markup still assigned to `torg-codex-application`) — Task T021 amends to v2.1.0.

---

## Phase 1: Setup — Verify Starting Baseline

**Purpose**: Confirm the pre-implementation state matches the research decisions before any code changes.

- [X] T001 Verify baseline: run `./mvnw compile -DskipTests` and confirm all 4 modules compile clean; run `./mvnw test -pl torg-codex -Dtest="AdapterConventionArchitectureTest"` and confirm exactly 2 test failures (`driveThruRpgOutboundAdapterLivesUnderDataAdapterOutHttp`, `domainEventBridgeOutboundAdapterLivesUnderDataAdapterOutEvent`) — do not fix, document as known pre-feature baseline (research Decision 5)

**Checkpoint**: All 4 modules compile; 2 stale ArchUnit failures documented as baseline.

---

## Phase 2: Foundational — Phase A: Markup Domain Service Relocation

**Purpose**: Relocate the 9 markup pipeline classes from `torg-codex-data` to `torg-codex-domain`,
create a Spring framework-binding adapter in `torg-codex-application`, and update all 33 consumer
import paths. This phase is the **prerequisite for all user stories** — Phase B architecture tests
cannot be added cleanly until `torg-codex-data` is fully free of the markup package.

**⚠️ CRITICAL**: Each task must leave `./mvnw compile -DskipTests` passing before the next begins.
The FR-010 markup carve-out in spec 003 becomes unnecessary only after this phase completes.

- [X] T002 Add `org.commonmark:commonmark:0.24.0` as a production dependency (no `<scope>` tag, defaults to compile) to `torg-codex-domain/pom.xml`; verify `./mvnw compile -pl torg-codex-domain -DskipTests` passes — `torg-codex-domain/pom.xml`

- [X] T003 Create `de.paladinsinn.torg.codex.domain.markup` package and introduce all 9 framework-free markup classes in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/markup/` with new package declaration; remove `@Service` from `TorgMarkupService` and `@Component` from `ConditionalBlockProcessor`, `EntityReferenceProcessor`, `GameTokenProcessor`, `MarkdownProcessor`, `RawHtmlProcessor` — **also remove** their `import org.springframework.stereotype.*` import statements; `Censor`, `EntityType`, `GameTokenRegistry` require only package-declaration update; **keep** `torg-codex-data` copies in place (temporary dual-state for compilability); verify `./mvnw compile -pl torg-codex-domain -DskipTests` — `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/markup/` (9 new files)

- [X] T004 Verify DomainPurityArchitectureTest passes after domain markup creation: `./mvnw test -pl torg-codex -Dtest="DomainPurityArchitectureTest"` must be GREEN — if any `org.springframework.*`, `jakarta.persistence.*`, or `org.hibernate.*` import remains in any `domain.markup.*` class, remove it before continuing — `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/markup/` (verify only)

- [X] T005 Neutralize Spring stereotypes on the **legacy data-module copies** in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/markup/`: remove `@Service`/`@Component` annotations and their `import org.springframework.stereotype.*` statements from `TorgMarkupService`, `ConditionalBlockProcessor`, `EntityReferenceProcessor`, `GameTokenProcessor`, `MarkdownProcessor`, `RawHtmlProcessor` so the legacy copies no longer register as Spring beans (preventing duplicate-bean conflicts when `MarkupConfiguration` is loaded in T006); verify `./mvnw compile -pl torg-codex-data -DskipTests` — `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/markup/*.java` (6 files modified)

- [X] T006 **[TDD — write test FIRST, expect RED]** Create `torg-codex-application/src/test/java/de/paladinsinn/torg/codex/markup/spring/MarkupConfigurationTest.java`; use `@SpringJUnitConfig(MarkupConfiguration.class)` (or `@Import(MarkupConfiguration.class)` with a minimal context) to load `MarkupConfiguration` and assert each of the 5 processor beans (`ConditionalBlockProcessor`, `EntityReferenceProcessor`, `RawHtmlProcessor`, `GameTokenProcessor`, `MarkdownProcessor`) and `TorgMarkupService` are present and injectable; test must compile but **fail** at this step since `MarkupConfiguration` does not yet exist; verify compilation: `./mvnw test-compile -pl torg-codex-application` passes — `torg-codex-application/src/test/java/de/paladinsinn/torg/codex/markup/spring/MarkupConfigurationTest.java` (new file)

- [X] T007 Create `de.paladinsinn.torg.codex.markup.spring.MarkupConfiguration` in `torg-codex-application/src/main/java/de/paladinsinn/torg/codex/markup/spring/MarkupConfiguration.java`; annotate `@Configuration`; declare `@Bean` methods: `conditionalBlockProcessor()→new ConditionalBlockProcessor()`, `entityReferenceProcessor()→new EntityReferenceProcessor()`, `rawHtmlProcessor()→new RawHtmlProcessor()`, `gameTokenProcessor()→new GameTokenProcessor()`, `markdownProcessor()→new MarkdownProcessor()`, `torgMarkupService(ConditionalBlockProcessor, EntityReferenceProcessor, RawHtmlProcessor, GameTokenProcessor, MarkdownProcessor)→new TorgMarkupService(...)`; all imports from `de.paladinsinn.torg.codex.domain.markup.*`; `GameTokenRegistry` is a static utility — do NOT create a `@Bean` for it; verify `./mvnw test -pl torg-codex-application` passes (MarkupConfigurationTest GREEN — now RED→GREEN) — `torg-codex-application/src/main/java/de/paladinsinn/torg/codex/markup/spring/MarkupConfiguration.java` (new file)

- [X] T008 Update all 33 production files to reference `de.paladinsinn.torg.codex.domain.markup.*` (import-path changes only — no logic changes): (1) `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/model/TorgEntity.java` — change `data.markup.Censor` → `domain.markup.Censor`; (2) 15 mapper files `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/mapper/ArticleMapper.java`, `CosmMapper.java`, `ItemMapper.java`, `MiracleListMapper.java`, `MiracleMapper.java`, `PerkGroupMapper.java`, `PerkMapper.java`, `PowerListMapper.java`, `PowerMapper.java`, `RaceMapper.java`, `ShardMapper.java`, `SpellListMapper.java`, `SpellMapper.java`, `ThreatMapper.java`, `VehicleMapper.java` plus `TorgMappingSupport.java` — change `data.markup.Censor` → `domain.markup.Censor`; (3) `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/security/CurrentUserCensorFactory.java` — change both `data.markup.Censor` → `domain.markup.Censor` AND `data.markup.TorgMarkupService` → `domain.markup.TorgMarkupService`; (4) 15 controller files `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/controller/ArticleController.java`, `CosmController.java`, `ItemController.java`, `MiracleController.java`, `MiracleListController.java`, `PerkController.java`, `PerkGroupController.java`, `PowerController.java`, `PowerListController.java`, `RaceController.java`, `ShardController.java`, `SpellController.java`, `SpellListController.java`, `ThreatController.java`, `VehicleController.java` — change `data.markup.Censor` → `domain.markup.Censor`; verify `./mvnw compile -pl torg-codex -DskipTests` passes (33 files updated)

- [X] T009 Move 6 markup unit tests from `torg-codex-data` to `torg-codex-domain`: copy `ConditionalBlockProcessorTest.java`, `EntityReferenceProcessorTest.java`, `GameTokenProcessorTest.java`, `MarkdownProcessorTest.java`, `RawHtmlProcessorTest.java`, `TorgMarkupServiceTest.java` from `torg-codex-data/src/test/java/de/paladinsinn/torg/codex/data/markup/` to `torg-codex-domain/src/test/java/de/paladinsinn/torg/codex/domain/markup/`; update `package` declaration in each file to `de.paladinsinn.torg.codex.domain.markup`; update any `import de.paladinsinn.torg.codex.data.markup.*` to `de.paladinsinn.torg.codex.domain.markup.*`; confirm each test uses no `@SpringBootTest`/`@SpringJUnitConfig` (tests must instantiate objects directly, no Spring context); verify `./mvnw test -pl torg-codex-domain` runs 6 markup tests GREEN — `torg-codex-domain/src/test/java/de/paladinsinn/torg/codex/domain/markup/` (6 new files)

- [X] T010 Delete legacy markup package and finalize POM: (1) delete all 9 files in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/markup/` (Censor.java, ConditionalBlockProcessor.java, EntityReferenceProcessor.java, EntityType.java, GameTokenProcessor.java, GameTokenRegistry.java, MarkdownProcessor.java, RawHtmlProcessor.java, TorgMarkupService.java) and the 6 test files in `torg-codex-data/src/test/java/de/paladinsinn/torg/codex/data/markup/`; (2) remove the `<dependency>` block for `org.commonmark:commonmark` from `torg-codex-data/pom.xml`; verify `./mvnw clean test -pl torg-codex-domain,torg-codex-data -DskipTests` passes; no `data.markup` production classes remain; 6 markup tests GREEN in domain — `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/markup/` (package deleted), `torg-codex-data/src/test/java/de/paladinsinn/torg/codex/data/markup/` (deleted), `torg-codex-data/pom.xml` (commonmark removed)

- [X] T011 **[Phase A gate — must pass before any Phase B work]** Run all pre-existing architecture tests: `./mvnw test -pl torg-codex -Dtest="DomainPurityArchitectureTest,ApplicationPurityArchitectureTest,ModuleBoundaryArchitectureTest,CensoringSingleMechanismArchitectureTest,AdapterConventionArchitectureTest,FreezeListFormatTest,FreezeListEnforcementArchitectureTest"` — all must be GREEN; **resolve any failure before starting Phase B** (note: `AdapterConventionArchitectureTest` will still show the 2 stale failures — that is expected and will be fixed in T014; all other listed tests must be GREEN)

**Checkpoint**: `torg-codex-data` has no `markup` package, `torg-codex-domain` owns the 9 markup classes (Spring-free), `MarkupConfiguration` wires them in `torg-codex-application`, all 33 consumer imports updated, 6 markup unit tests GREEN in domain. Phase B can now begin.

---

## Phase 3: User Story 1 — Build Catches Reintroduction of Forbidden Dependencies (Priority: P1) 🎯 MVP

**Goal**: Add `DataPersistenceBoundaryArchitectureTest` so any future addition of `de.paladinsinn.security.*`, `de.paladinsinn.drivethru.*`, or `org.springframework.security.*` imports to `torg-codex-data` production classes causes an immediate build failure naming the offending class and rule (FR-001, FR-002, spec acceptance scenarios 1-3).

**Independent Test**: `./mvnw test -pl torg-codex -Dtest="DataPersistenceBoundaryArchitectureTest"` all 3 methods GREEN; deliberate import regression causes BUILD FAILURE (SC-003).

### Implementation for User Story 1

- [X] T012 [US1] Create `DataPersistenceBoundaryArchitectureTest.java` with 3 `@Test` methods using `ArchitectureTestSupport.assertNoUnfrozenViolations(...)`: (1) `dataMustNotReferenceSecurityIntegrationClasses` — `noClasses().that().resideInAnyPackage("de.paladinsinn.torg.codex.data..").should().dependOnClassesThat().resideInAnyPackage("de.paladinsinn.security..").as("torg-codex-data must not reference Security integration classes")`; (2) `dataMustNotReferenceDriveThruRpgIntegrationClasses` — `noClasses().that().resideInAnyPackage("de.paladinsinn.torg.codex.data..").should().dependOnClassesThat().resideInAnyPackage("de.paladinsinn.drivethru..").as("torg-codex-data must not reference DriveThruRPG integration classes")`; (3) `dataMustNotImportSpringSecurityFrameworkClasses` — `noClasses().that().resideInAnyPackage("de.paladinsinn.torg.codex.data..").should().dependOnClassesThat().resideInAnyPackage("org.springframework.security..").as("torg-codex-data must not import Spring Security framework classes")`; NO `allowEmptyShould(true)` (data package is non-empty); NO FreezeList entries (FR-011) — `torg-codex/src/test/java/de/paladinsinn/torg/codex/architecture/DataPersistenceBoundaryArchitectureTest.java` (new file)

- [X] T013 [US1] Verify all 3 `DataPersistenceBoundaryArchitectureTest` methods pass GREEN with zero violations: `./mvnw test -pl torg-codex -Dtest="DataPersistenceBoundaryArchitectureTest"` — SC-002 baseline confirmed; if any violation appears, resolve at source (no FreezeList suppression)

- [X] T014 [US1] Deliberate regression probe (SC-003): temporarily add `import de.paladinsinn.security.DriveThruUserDetails;` to any `torg-codex-data` production class (e.g., a comment-only import in `TorgDataConfiguration.java`); run `./mvnw test -pl torg-codex -Dtest="DataPersistenceBoundaryArchitectureTest"` — confirm BUILD FAILURE with message naming the offending class and rule `"torg-codex-data must not reference Security integration classes"`; revert the change; rerun and confirm BUILD SUCCESS — `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/TorgDataConfiguration.java` (temporary edit, then reverted)

**Checkpoint**: `DataPersistenceBoundaryArchitectureTest` with 3 methods passes GREEN; deliberate regression is detected and named correctly. User Story 1 independently testable and complete.

---

## Phase 4: User Story 2 — Architecture Tests Correctly Reflect ADR-016 Boundary (Priority: P1)

**Goal**: Replace the 2 stale `AdapterConventionArchitectureTest` methods that assert pre-ADR-016 package locations with accurate assertions matching the current `de.paladinsinn.drivethru.*` locations (FR-003; spec acceptance scenarios 1-3).

**Independent Test**: `./mvnw test -pl torg-codex -Dtest="AdapterConventionArchitectureTest"` — 5 methods, 0 failures (SC-004).

### Implementation for User Story 2

- [X] T015 [US2] In `AdapterConventionArchitectureTest.java`, replace 2 stale test methods: (1) rename `driveThruRpgOutboundAdapterLivesUnderDataAdapterOutHttp` → `driveThruRpgOutboundAdapterLivesUnderDriveThruAdapterOutHttp`; change assertion to verify `DriveThruRpgProductAdapter` resides in package `de.paladinsinn.drivethru.adapter.out.http` (was: `de.paladinsinn.torg.codex.data.adapter.out.http`); (2) rename `domainEventBridgeOutboundAdapterLivesUnderDataAdapterOutEvent` → `domainEventBridgeOutboundAdapterLivesUnderDriveThruAdapterOutEvent`; change assertion to verify `SpringDomainEventPublisherAdapter` resides in package `de.paladinsinn.drivethru.adapter.out.event` (was: `de.paladinsinn.torg.codex.data.adapter.out.event`); 3 unchanged methods (`controllersRemainInInboundAdapterPackage`, `outboundAdaptersRemainInDataAdapterOutPackages`, `everyCatalogAreaControllerIntroducedInPhase4dExistsUnderApiController`) must remain intact — `torg-codex/src/test/java/de/paladinsinn/torg/codex/architecture/AdapterConventionArchitectureTest.java` (2 methods replaced)

- [X] T016 [US2] Verify all 5 `AdapterConventionArchitectureTest` methods pass GREEN: `./mvnw test -pl torg-codex -Dtest="AdapterConventionArchitectureTest"` — 0 failures; SC-004 satisfied; confirm previously failing `driveThruRpgOutboundAdapterLivesUnderDriveThruAdapterOutHttp` and `domainEventBridgeOutboundAdapterLivesUnderDriveThruAdapterOutEvent` now pass

**Checkpoint**: All 5 `AdapterConventionArchitectureTest` methods pass GREEN. Architecture test suite trustworthy again. User Story 2 independently testable and complete.

---

## Phase 5: User Story 3 — Module POM Dependency Boundary Verifiable and Clean (Priority: P2)

**Goal**: Confirm and document that `torg-codex-data/pom.xml` contains no Spring Security, `de.paladinsinn.security.*`, or `de.paladinsinn.drivethru.*` production-scope dependencies; and that `commonmark` has fully migrated to `torg-codex-domain` (FR-004, FR-005, FR-006, SC-005).

**Independent Test**: `./mvnw dependency:tree -pl torg-codex-data | grep -i security` returns no output; `commonmark` absent in data dependency tree, present in domain.

### Verification for User Story 3

- [X] T017 [P] [US3] Inspect `torg-codex-data/pom.xml` and confirm absence of `spring-boot-starter-security`, `spring-security-core`, `spring-security-web`, and any `de.paladinsinn.security.*` or `de.paladinsinn.drivethru.*` artifact at `compile` or `runtime` scope (FR-004, FR-005); if any such entry is found, remove it — `torg-codex-data/pom.xml` (inspection; remove if violations exist)

- [X] T018 [US3] Run POM boundary checks (SC-005): (1) `./mvnw dependency:tree -pl torg-codex-data | grep -i security` — expect no output; (2) `./mvnw dependency:tree -pl torg-codex-data | grep commonmark` — expect no output; (3) `./mvnw dependency:tree -pl torg-codex-domain | grep commonmark` — expect exactly one line with `org.commonmark:commonmark:0.24.0`; document results

- [X] T019 [P] [US3] Inspect `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/TorgDataConfiguration.java` — confirm `@ComponentScan` covers only `de.paladinsinn.torg.codex.data` and that `@EntityScan`/`@EnableJpaRepositories` roots are limited to persistence packages; confirm no `de.paladinsinn.security` or `de.paladinsinn.drivethru` package root appears in any scan annotation (FR-006) — `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/TorgDataConfiguration.java` (inspection only)

**Checkpoint**: POM boundary clean; `commonmark` exclusively in domain; `@ComponentScan` persistence-only. User Story 3 independently testable and complete.

---

## Phase 6: Governance — A8 Required Deliverables (Non-Optional)

**Purpose**: Record the markup-to-domain ownership decision via ADR and amend the constitution.
These are governance commitments from plan Task A8 — they are **mandatory**, not optional.

- [X] T020 Create ADR-017 at `docs/modules/arc42/pages/09_architecture_decisions/017_markup-domain-service.adoc` following the AsciiDoc structure and status keywords of existing ADRs (use `016_application-integration-boundaries.adoc` as template); status: `Accepted`; date: today; record: (a) markup-to-domain ownership decision supersedes Constitution v2.0.0 Principle I and Principle VI regarding markup assignment; (b) framework-binding adapter pattern — pure domain classes in `de.paladinsinn.torg.codex.domain.markup`, Spring `@Configuration` wiring at `de.paladinsinn.torg.codex.markup.spring.MarkupConfiguration` in `torg-codex-application`, following the same pattern as `de.paladinsinn.drivethru.*` and `de.paladinsinn.security.*`; (c) references specs/002-markup-to-domain/spec.md and specs/003-data-persistence-boundary/spec.md as motivation; (d) records that `commonmark:0.24.0` moves to `torg-codex-domain` — `docs/modules/arc42/pages/09_architecture_decisions/017_markup-domain-service.adoc` (new file)

- [X] T021 Register ADR-017 in `docs/modules/arc42/pages/09_architecture_decisions/_include.adoc` (add include entry) and `_nav.adoc` (add nav entry) following the existing pattern for ADR-016 — `docs/modules/arc42/pages/09_architecture_decisions/_include.adoc`, `_nav.adoc` (2 files modified)

- [X] T022 Amend `.specify/memory/constitution.md` from v2.0.0 to v2.1.0 (MINOR — markup ownership redefined; no principle removed): (1) prepend a new Sync Impact Report comment block above the existing ones documenting the v2.0.0→v2.1.0 change (markup processing moves to `torg-codex-domain`); (2) in Principle I, update `torg-codex-data` must-not list and `torg-codex-application` ownership table — markup processing is now owned by `torg-codex-domain`, not `torg-codex-application`; (3) update Principle VI first sentence to say the markup pipeline is owned by `torg-codex-domain` (framework-free) with Spring bean wiring provided by `MarkupConfiguration` in `torg-codex-application`; (4) update the version footer from `2.0.0` to `2.1.0` and set Last Amended to today — `.specify/memory/constitution.md` (amended)

**Checkpoint**: ADR-017 recorded and registered. Constitution at v2.1.0 reflecting markup ownership in `torg-codex-domain`. Governance deliverables complete.

---

## Phase 7: Polish & Full Build Verification

**Purpose**: Verify the full end state: all architecture tests pass, full JDK 25 Maven build clean, zero FreezeList violations for new rules, zero REST behavior change.

- [X] T023 [P] Run full architecture test suite: `./mvnw test -pl torg-codex -Dtest="*ArchitectureTest"` — expect **28 methods GREEN** total: `DataPersistenceBoundaryArchitectureTest` (3 new), `AdapterConventionArchitectureTest` (5, 2 corrected + 3 unchanged), `DomainPurityArchitectureTest` (1), `ModuleBoundaryArchitectureTest` (5), `ApplicationPurityArchitectureTest` (2), `TransactionBoundaryArchitectureTest` (3), `CensoringSingleMechanismArchitectureTest` (4), `FreezeListEnforcementArchitectureTest` (4), `FreezeListFormatTest` (1) = 28 total (quickstart Architecture Test Count Checkpoint)

- [X] T024 [P] Verify zero new FreezeList entries cover the data-purity rules: run `./mvnw test -pl torg-codex -Dtest="FreezeListFormatTest,FreezeListEnforcementArchitectureTest"` GREEN; inspect `specs/architecture-migration/freeze-list.md` and confirm no entry references `DataPersistenceBoundaryArchitectureTest` or its rule names `"torg-codex-data must not reference Security integration classes"` / `"torg-codex-data must not reference DriveThruRPG integration classes"` / `"torg-codex-data must not import Spring Security framework classes"` (FR-011, SC-002)

- [X] T025 Run full JDK 25 Maven wrapper build (requires Docker for Testcontainers): `./mvnw clean verify` — **all** of the following must pass: (a) all 4 reactor modules compile; (b) 6 markup unit tests GREEN in `torg-codex-domain` (no Spring context); (c) all architecture tests GREEN in `torg-codex` (28 methods); (d) all integration tests (`*IT`) GREEN under Failsafe with Testcontainers PostgreSQL; (e) characterization replay tests byte-identical to pre-feature baseline (zero REST API behavior change, SC-006); (f) `CensoringSingleMechanismArchitectureTest` all 4 tests GREEN (censoring chain unchanged); (g) zero `DataPersistenceBoundaryArchitectureTest` failures; (h) zero `AdapterConventionArchitectureTest` failures

**Checkpoint**: `./mvnw clean verify` GREEN on all 4 modules. Feature complete and verified.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — run immediately.
- **Foundational (Phase 2, Phase A)**: Depends on Setup → BLOCKS all user story phases (US1, US2, US3 all depend on Phase A completing to ensure markup is gone from `torg-codex-data`).
- **US1 (Phase 3)**: Depends on Phase 2 completion (T011 gate passed).
- **US2 (Phase 4)**: Depends on Phase 2 completion. Independent of US1 (different test files).
- **US3 (Phase 5)**: Depends on Phase 2 completion (commonmark POM change in T002/T010). Independent of US1 and US2.
- **Governance (Phase 6)**: Depends on Phase 2 completion. Independent of US1, US2, US3 (different files).
- **Polish (Phase 7)**: Depends on all prior phases completing.

### User Story Dependencies (within Foundational Phase 2)

Internal task ordering within Phase 2:

```
T002 (add commonmark to domain POM)
  ↓
T003 (create domain markup classes — framework-free)
  ↓
T004 (verify DomainPurityArchitectureTest GREEN)
  ↓
T005 (write MarkupConfigurationTest — TDD RED)
T005 also requires T003 (domain markup classes must exist to compile the test)
  ↓
T005 neutralization of legacy data copies must happen before T007 (MarkupConfiguration)
T005 (neutralize legacy data.markup Spring stereotypes)
  ↓
T006 (write MarkupConfigurationTest) → T007 (implement MarkupConfiguration — RED→GREEN)
  ↓
T008 (update 33 import paths — requires domain markup classes exist AND data copies still present)
  ↓
T009 (move tests to domain) → T010 (delete data.markup package + remove commonmark from data POM)
  ↓
T011 (Phase A gate — all pre-existing architecture tests GREEN)
```

Note: T004 (architecture test run) can be performed after T003; T005 and its prerequisite T005-neutralize
must be sequential as documented above.

### Within Phase 3 (US1)

1. T012 (write `DataPersistenceBoundaryArchitectureTest`) → test-first, verifies GREEN immediately (no violations to fix)
2. T013 (verify GREEN baseline)
3. T014 (deliberate regression probe, SC-003)

### Within Phase 4 (US2)

1. T015 (replace 2 stale test methods in `AdapterConventionArchitectureTest`)
2. T016 (verify all 5 methods GREEN)

### Within Phase 6 (Governance A8)

1. T020 (create ADR-017) → T021 (register in _include and _nav) → T022 (amend constitution)
   (T020 can be done in parallel with Phase 3-5 user story work since it touches different files)

---

## Parallel Opportunities

### Phase 2 (Foundational) — Parallelizable groups

```
# Group 1 — after T002: independent of each other, different modules:
T003 (domain markup classes)    # torg-codex-domain production
T004 runs immediately after T003 (verify)

# T005 (neutralize legacy data copies) must precede T007 but not T003:
# T003 and T005 touch different files and CAN run in parallel:
T003 || T005  # different module source trees — parallel safe

# Group 2 — after T003 and T005:
T006 (write MarkupConfigurationTest)  # test-first in torg-codex-application
→ T007 (create MarkupConfiguration)  # sequential: implement after test written

# T008 (33 import updates) — the 33 files can be updated in sub-batches:
# Sub-batch A: TorgEntity (torg-codex-data) + 16 mapper files (torg-codex)
# Sub-batch B: CurrentUserCensorFactory + 15 controller files (torg-codex)
# Sub-batches are independent (different directories), but must all complete before T010
```

### Phase 3-5 (User Stories) — Parallel after T011 gate

```
# Once T011 (Phase A gate) passes, all 3 user story phases can proceed in parallel:
T012-T014 (US1: DataPersistenceBoundaryArchitectureTest)  ||
T015-T016 (US2: AdapterConventionArchitectureTest fix)    ||
T017-T019 (US3: POM boundary verification)
```

### Phase 6 (Governance) — Parallel with user story phases

```
# Governance tasks touch docs and constitution only — no source code conflicts:
T020-T022 (ADR-017 + constitution v2.1.0)  ||  T012-T019 (US1+US2+US3 phases)
```

---

## Implementation Strategy

### MVP First (Phase 2 + US1 + US2)

The minimum verifiable increment that satisfies spec 003's primary requirements:

1. Complete Phase 1: Baseline verification
2. Complete Phase 2: Markup relocation (T002–T011 gate)
3. Complete Phase 3 (US1): `DataPersistenceBoundaryArchitectureTest` (T012–T014)
4. Complete Phase 4 (US2): Stale test correction (T015–T016)
5. **STOP and validate**: `./mvnw test -pl torg-codex -Dtest="*ArchitectureTest"` all GREEN; US1 and US2 spec acceptance scenarios met

### Incremental Delivery

```
Phase 1+2 complete → Foundation: markup relocated, architecture test gate clean
  ↓
Phase 3 (US1) → Build catches forbidden imports in torg-codex-data (primary governance goal)
  ↓
Phase 4 (US2) → Architecture test suite trustworthy (stale tests removed)
  ↓
Phase 5 (US3) → POM boundary documented and verified
  ↓
Phase 6 (Governance) → ADR-017 recorded, constitution v2.1.0 ratified
  ↓
Phase 7 → Full build verified clean (./mvnw clean verify GREEN)
```

### Invariants to Maintain Throughout

- `./mvnw compile -DskipTests` must pass after every task (no broken builds between tasks)
- **Never** add a FreezeList entry for the 3 data-purity rules (FR-011)
- **Never** change Liquibase changesets, JPA entity fields, or REST endpoint responses (FR-008, FR-009)
- **Never** rename or change public API of `de.paladinsinn.security.*` or `de.paladinsinn.drivethru.*` classes (FR-007)
- The `@Transient Censor censor` field in `TorgEntity` is preserved; only its import path changes (research Decision 13)

---

## User Story Coverage Summary

| User Story | Tasks | Test Count | Independent Test Command |
|---|---|---|---|
| **US1 (P1)**: Build catches forbidden `torg-codex-data` imports | T012–T014 | 3 ArchUnit methods | `./mvnw test -pl torg-codex -Dtest="DataPersistenceBoundaryArchitectureTest"` |
| **US2 (P1)**: Stale adapter location tests corrected | T015–T016 | 5 methods (2 corrected) | `./mvnw test -pl torg-codex -Dtest="AdapterConventionArchitectureTest"` |
| **US3 (P2)**: POM boundary verifiable and clean | T017–T019 | n/a (dependency tree inspection) | `./mvnw dependency:tree -pl torg-codex-data \| grep -i security` |
| **Foundational (Phase A)**: Markup relocation | T002–T011 | 6 unit tests moved; 1 new MarkupConfigurationTest | `./mvnw test -pl torg-codex-domain` |
| **Governance (A8)**: ADR + constitution | T020–T022 | n/a (docs) | ADR-017 reviewed; constitution v2.1.0 ratified |

**Total tasks**: 25 (T001–T025)
**Net new test files**: 2 (`DataPersistenceBoundaryArchitectureTest.java`, `MarkupConfigurationTest.java`)
**Net modified test files**: 2 (6 tests moved to domain, `AdapterConventionArchitectureTest.java` 2 methods replaced)
**New production files**: 10 (9 domain markup classes + `MarkupConfiguration.java`)
**Deleted production files**: 9 (data.markup package)
**Import-path updates**: 33 production files, no logic changes

---

## Verification Commands Reference

```bash
# Phase 1 — baseline
./mvnw compile -DskipTests
./mvnw test -pl torg-codex -Dtest="AdapterConventionArchitectureTest"   # expect 2 failures

# Phase A intermediate checks
./mvnw compile -pl torg-codex-domain -DskipTests                         # after T002, T003
./mvnw test -pl torg-codex -Dtest="DomainPurityArchitectureTest"          # after T003 (A4 verify)
./mvnw test -pl torg-codex-application                                    # after T007 (GREEN)
./mvnw compile -pl torg-codex -DskipTests                                 # after T008
./mvnw test -pl torg-codex-domain                                         # after T009 (6 tests GREEN)
./mvnw clean test -pl torg-codex-domain,torg-codex-data -DskipTests       # after T010

# Phase A gate (T011 — must all be GREEN before Phase B)
./mvnw test -pl torg-codex -Dtest="DomainPurityArchitectureTest,ApplicationPurityArchitectureTest,ModuleBoundaryArchitectureTest,CensoringSingleMechanismArchitectureTest,AdapterConventionArchitectureTest,FreezeListFormatTest,FreezeListEnforcementArchitectureTest"

# Phase B — US1
./mvnw test -pl torg-codex -Dtest="DataPersistenceBoundaryArchitectureTest"   # expect GREEN

# Phase B — US2
./mvnw test -pl torg-codex -Dtest="AdapterConventionArchitectureTest"         # expect 5 GREEN, 0 fail

# US3 POM checks (SC-005)
./mvnw dependency:tree -pl torg-codex-data | grep -i security   # expect no output
./mvnw dependency:tree -pl torg-codex-data | grep commonmark     # expect no output
./mvnw dependency:tree -pl torg-codex-domain | grep commonmark   # expect org.commonmark:commonmark:0.24.0

# Polish — full architecture suite (T023)
./mvnw test -pl torg-codex -Dtest="*ArchitectureTest"    # expect 28 GREEN

# Full build (T025 — requires Docker)
./mvnw clean verify
```
