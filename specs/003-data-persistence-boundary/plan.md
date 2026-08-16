# Implementation Plan: Enforce Data-Persistence-Only Module Boundary
*(with consolidated Markup-to-Domain relocation — feature 002 integrated)*

**Branch**: `003-data-persistence-boundary` | **Date**: 2026-08-16 | **Revised**: 2026-08-16
**Spec**: [spec.md](./spec.md) | **Consolidated from**: [../002-markup-to-domain/spec.md](../002-markup-to-domain/spec.md)

**Input**: Feature specification from `specs/003-data-persistence-boundary/spec.md`, cross-referenced with `specs/002-markup-to-domain/spec.md` per user authoritative instruction consolidating feature 002 into this plan.

## Summary

Feature 003 originally targeted two test-only changes: add `DataPersistenceBoundaryArchitectureTest` to enforce that `torg-codex-data` holds no Security/DriveThruRPG production references, and correct two stale tests in `AdapterConventionArchitectureTest`. That scope is preserved as **Phase B** of this plan.

**By user authoritative instruction**, feature 002 (markup-to-domain) is consolidated into this plan as **Phase A** — a prerequisite that must complete before Phase B. Markup is no longer a separate implementation track. The markup processing pipeline (`TorgMarkupService`, `Censor`, `ConditionalBlockProcessor`, `EntityReferenceProcessor`, `RawHtmlProcessor`, `GameTokenProcessor`, `GameTokenRegistry`, `EntityType`, `MarkdownProcessor`) relocates from `torg-codex-data` to `torg-codex-domain` as a framework-independent domain service. Spring annotations (`@Service`, `@Component`) are stripped from all relocated classes. A new framework-binding `@Configuration` in `torg-codex-application` (package `de.paladinsinn.torg.codex.markup.spring`) recreates equivalent Spring bean definitions; injection-based callers are updated only in their import paths.

The combined outcome makes `torg-codex-data` genuinely persistence-only — JPA entities, Spring Data repositories, Liquibase migrations, and persistence adapters — with markup, Security, and DriveThruRPG integration fully absent. `DataPersistenceBoundaryArchitectureTest` then enforces the resulting clean boundary without any carve-out exception (the FR-010 exemption from the original spec 003 becomes moot: markup is gone from data before the rule is introduced).

## Constitution Reconciliation

**Conflict**: Constitution v2.0.0 Principle I states: "`torg-codex-data` MUST NOT contain markup, security, or DriveThruRPG integration. Those concerns MUST be housed in `torg-codex-application`." Principle VI places the markup pipeline in `torg-codex-application`. Feature 002 spec (FR-012, FR-013) requires a new ADR and a constitution amendment to correct the markup assignment from `torg-codex-application` to `torg-codex-domain`.

**Resolution**: The user's explicit instruction is authoritative and supersedes the current constitution text: *"markup implementation moves into torg-codex-domain as a framework-independent domain service."* This plan proceeds on domain ownership. The constitution amendment (version bump to at least v2.1.0) and a new ADR documenting the markup-to-domain decision are **required deliverables of the implementation phase** (Task A8). They are not authored in this plan phase; they are named implementation tasks. No FreezeList entry is used to paper over the conflict.

**Feature 002 status after consolidation**: `specs/002-markup-to-domain/spec.md` remains as the reference specification document for the markup relocation requirements. No new implementation track for feature 002 is created; its user stories, acceptance scenarios, and functional requirements are satisfied within the implementation phase of this feature 003 plan.

## Technical Context

**Language/Version**: Java 25 (`<java.version>25</java.version>` in all module POMs; JDK 25
required — Eclipse Temurin 25.0.3 used locally, confirmed by plan/001 research)

**Primary Dependencies**:
- ArchUnit 1.4.1 (`archunit-junit5`, already in `torg-codex/pom.xml`) — no new dependency for Phase B
- `org.commonmark:commonmark:0.24.0` — moves from `torg-codex-data` to `torg-codex-domain` as a production dependency (pure Java, no Spring coupling — permissible in framework-free domain module)
- Spring Boot 4.0.6 (already in `torg-codex-application`) — provides `@Configuration`/`@Bean` for the markup framework-binding adapter in Phase A
- No net-new external dependency introduced to the reactor

**Storage**: PostgreSQL via Liquibase-managed schema. **N/A for this feature** — no JPA entity, repository, Liquibase changeset, or database schema column is added, modified, or removed.

**Testing**:
- Phase A: `./mvnw test -pl torg-codex-domain` runs markup unit tests (no Testcontainers); `./mvnw test -pl torg-codex -Dtest="DomainPurityArchitectureTest,ApplicationPurityArchitectureTest,ModuleBoundaryArchitectureTest,CensoringSingleMechanismArchitectureTest"` validates architecture test health after markup move
- Phase B: `./mvnw test -pl torg-codex -Dtest="*ArchitectureTest"` runs all ArchUnit tests
- Full: `./mvnw clean verify` runs the complete reactor including Testcontainers integration tests and Failsafe

**Target Platform**: Same Spring Boot web application in the `torg-codex` module; no new deployment unit.

**Project Type**: Existing 4-module Maven reactor (`torg-codex-domain → torg-codex-application → torg-codex-data → torg-codex`).

**Performance Goals**: N/A — markup processing is a pure-Java pipeline; `commonmark` has no runtime Spring coupling. Rendering throughput and behavior are unchanged.

**Production source changes (Phase A)**:
- `torg-codex-domain/pom.xml`: add `commonmark:0.24.0` production dependency
- `torg-codex-domain/src/main/java`: create `de.paladinsinn.torg.codex.domain.markup` package; introduce 9 repackaged markup classes as the temporary migration copy; `@Service`/`@Component` annotations removed from the domain copies
- `torg-codex-application/src/main/java`: create `de.paladinsinn.torg.codex.markup.spring.MarkupConfiguration` — a Spring `@Configuration` class that provides `@Bean` definitions for the five processor classes and `TorgMarkupService`, following the same pattern as `de.paladinsinn.drivethru.*` and `de.paladinsinn.security.*`
- `torg-codex-data/pom.xml`: remove `commonmark:0.24.0` production dependency
- `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/markup/`: **package deleted**
- `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/model/TorgEntity.java`: import updated from `data.markup.Censor` → `domain.markup.Censor`
- Production sources: 33 import-path updates in total — `TorgEntity`, 15 catalog mapper interfaces plus `TorgMappingSupport`, `CurrentUserCensorFactory` (2 imports), and 15 REST controllers — all change `data.markup.Censor` → `domain.markup.Censor`; `CurrentUserCensorFactory` also changes `data.markup.TorgMarkupService` → `domain.markup.TorgMarkupService`

**Test source changes (Phase A)**:
- Move 6 markup unit tests from `torg-codex-data/src/test/java/.../data/markup/` to `torg-codex-domain/src/test/java/.../domain/markup/`; update `package` declarations and import paths; verify no Spring context required

**Test source changes (Phase B)**:
- `torg-codex/src/test/java/.../architecture/DataPersistenceBoundaryArchitectureTest.java`: **new file** (3 test methods; no FreezeList entries)
- `torg-codex/src/test/java/.../architecture/AdapterConventionArchitectureTest.java`: **2 test methods replaced** (stale assertions corrected)

**Constraints**:
- No class rename or API change in `de.paladinsinn.security.*` or `de.paladinsinn.drivethru.*` (FR-007 of spec 003)
- No Liquibase/JPA/schema changes (FR-008 of spec 003 / FR-006 of spec 002)
- Zero externally visible REST API behavior change (FR-009 of spec 003 / FR-007 of spec 002)
- No FreezeList entry for new data-purity rules (FR-011 of spec 003)
- All relocated markup classes in `torg-codex-domain` must have zero Spring/Jakarta CDI imports in production source (FR-002 of spec 002)
- `./mvnw clean verify` must pass (FR-012 of spec 003 / SC-004 of spec 002)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|---|---|---|
| **I. Hexagonal Architecture & Clean Ports** | **PASS** | Markup moves to domain (framework-free), Spring wiring adapter moves to `torg-codex-application`. Constitution v2.0.0 text assigning markup to `torg-codex-application` is superseded by user authority (see Constitution Reconciliation section). A constitution amendment is a named deliverable (Task A8). Module dependency graph unchanged; `torg-codex-data` already depends on `torg-codex-domain` so no new inter-module dependency introduced. Architecture test enforcement complete after Phase B. FR-010's markup carve-out becomes unnecessary because markup exits `torg-codex-data` in Phase A before the enforcement test is added in Phase B. |
| **II. Self-Contained Systems & Async Integration** | **N/A** | No inter-service, AMQP, or event-bus surface touched. |
| **III. Standardized REST APIs & OpenAPI Contracts** | **PASS** | Zero API behavior changes. All 17 catalog endpoint responses identical before and after: `render()` pipeline order preserved (conditional blocks → entity refs → raw HTML → game tokens → CommonMark); `Censor.apply()` logic unchanged; `CurrentUserCensorFactory.create()` behavior unchanged. |
| **IV. Zero-Downtime Evolutionary DB Migrations** | **N/A** | No schema, JPA entity field, repository, or Liquibase changeset changes. |
| **V. Multi-Tier Security, DRM Censorship & Data Protection** | **PASS** | The `CurrentUserCensorFactory` → `ProductOwnershipResolver` → `Censor` censoring chain is preserved exactly. Import paths change (`data.markup` → `domain.markup`) but no behavioral change. `CensoringSingleMechanismArchitectureTest` (all 4 tests) continues to pass: `CurrentUserCensorFactory` is still the sole dependency on `ProductOwnershipResolver`; `GrantedAuthority` derivation remains confined to `api.security`. Censoring behavior for product-gated content (`<IF:id>`, `<IF:!id>`) unchanged. Security and DriveThruRPG remain in `torg-codex-application`. |
| **VI. Deterministic Markdown Pipeline & Template Safety** | **PASS (with noted amendment)** | Pipeline order preserved exactly. Constitution v2.0.0 Principle VI says pipeline belongs in `torg-codex-application`; user authority moves it to `torg-codex-domain`. Constitution amendment is a Task A8 deliverable. Pre-existing acknowledged gaps (OWASP HTML Sanitizer absent; `commonmark` instead of `flexmark-java` per ADR-011) are out of scope for this feature, unchanged, and carry no FreezeList entry. |
| **VII. Production Observability & Quality Standards** | **N/A** | No new operational surface. |
| **VIII. Test-First & Integration Verification (NON-NEGOTIABLE)** | **PASS** | All 6 markup unit tests migrate to `torg-codex-domain` and remain Spring-context-free. New `DataPersistenceBoundaryArchitectureTest` enforces the data boundary. Two corrected `AdapterConventionArchitectureTest` methods guard ADR-016 locations. `./mvnw clean verify` must pass at every incremental phase boundary. |

**Post-Phase-1 re-check**: PASS — all architecture tests pass on the redesigned package layout:
- `DomainPurityArchitectureTest.domainContainsNoSpringOrJpaImports` passes because Spring annotations (`@Service`, `@Component`) are stripped from relocated markup classes; `commonmark` has no Spring or JPA coupling.
- `ApplicationPurityArchitectureTest` checks `de.paladinsinn.torg.codex.application.*` only; the markup Spring config lives at `de.paladinsinn.torg.codex.markup.spring.*` — outside the checked package tree, following the same pattern as `de.paladinsinn.drivethru.*` and `de.paladinsinn.security.*`.
- `ModuleBoundaryArchitectureTest.applicationDependsOnlyOnDomainAndJava` checks `de.paladinsinn.torg.codex.application.*` only; no violation.
- `CensoringSingleMechanismArchitectureTest` passes: `CurrentUserCensorFactory` still the sole factory; no new ownership-resolution mechanism introduced.
- `DataPersistenceBoundaryArchitectureTest` passes cleanly with no FR-010 exemption because markup has been removed from `torg-codex-data` in Phase A before the rule is introduced in Phase B.

## Project Structure

### Documentation (this feature)

```text
specs/003-data-persistence-boundary/
├── plan.md              # This file (consolidated feature 002 + 003)
├── research.md          # Phase 0 findings (decisions 1–16)
├── data-model.md        # Phase 1: boundary model (updated for domain markup)
├── quickstart.md        # Phase 1: validation guide (updated for full scope)
├── contracts/
│   └── data-persistence-boundary-rule.md  # Phase 1: architecture rule invariants (updated)
├── checklists/
│   └── requirements.md  # Spec quality gate (unchanged)
└── tasks.md             # Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code Changes — Phase A: Markup Domain Service Relocation

```text
torg-codex-domain/
├── pom.xml                                       # ADD: commonmark:0.24.0 production dependency
└── src/
    ├── main/java/de/paladinsinn/torg/codex/domain/
    │   └── markup/                               # NEW PACKAGE — moved from data.markup
    │       ├── Censor.java                       # MOVED; package decl updated (no Spring annotation)
    │       ├── ConditionalBlockProcessor.java    # MOVED; package decl updated; @Component removed
    │       ├── EntityReferenceProcessor.java     # MOVED; package decl updated; @Component removed
    │       ├── EntityType.java                   # MOVED; package decl updated (no annotation)
    │       ├── GameTokenProcessor.java           # MOVED; package decl updated; @Component removed
    │       ├── GameTokenRegistry.java            # MOVED; package decl updated (no annotation)
    │       ├── MarkdownProcessor.java            # MOVED; package decl updated; @Component removed
    │       ├── RawHtmlProcessor.java             # MOVED; package decl updated; @Component removed
    │       └── TorgMarkupService.java            # MOVED; package decl updated; @Service removed
    └── test/java/de/paladinsinn/torg/codex/domain/
        └── markup/                               # NEW — tests moved from torg-codex-data
            ├── ConditionalBlockProcessorTest.java
            ├── EntityReferenceProcessorTest.java
            ├── GameTokenProcessorTest.java
            ├── MarkdownProcessorTest.java
            ├── RawHtmlProcessorTest.java
            └── TorgMarkupServiceTest.java

torg-codex-application/
├── pom.xml                                       # UNCHANGED (Spring deps already present)
├── src/main/java/de/paladinsinn/torg/codex/markup/spring/
│   └── MarkupConfiguration.java                  # NEW: Spring @Configuration providing @Bean
│                                                 # definitions for the five processors and service
└── src/test/java/de/paladinsinn/torg/codex/markup/spring/
    └── MarkupConfigurationTest.java              # NEW: isolated bean-wiring test

torg-codex-data/
├── pom.xml                                       # REMOVE: commonmark:0.24.0 production dependency
└── src/main/java/de/paladinsinn/torg/codex/data/
    ├── markup/                                   # DELETED PACKAGE (all 9 classes removed)
    └── model/
        └── TorgEntity.java                       # UPDATE: Censor import data.markup → domain.markup

torg-codex/
└── src/main/java/de/paladinsinn/torg/codex/api/
    ├── mapper/
    │   ├── ArticleMapper.java                    # UPDATE: Censor import data.markup → domain.markup
    │   ├── CosmMapper.java                       # UPDATE: Censor import
    │   ├── ItemMapper.java                       # UPDATE: Censor import
    │   ├── MiracleListMapper.java                # UPDATE: Censor import
    │   ├── MiracleMapper.java                    # UPDATE: Censor import
    │   ├── PerkGroupMapper.java                  # UPDATE: Censor import
    │   ├── PerkMapper.java                       # UPDATE: Censor import
    │   ├── PowerListMapper.java                  # UPDATE: Censor import
    │   ├── PowerMapper.java                      # UPDATE: Censor import
    │   ├── RaceMapper.java                       # UPDATE: Censor import
    │   ├── ShardMapper.java                      # UPDATE: Censor import
    │   ├── SpellListMapper.java                  # UPDATE: Censor import
    │   ├── SpellMapper.java                      # UPDATE: Censor import
    │   ├── ThreatMapper.java                     # UPDATE: Censor import
    │   ├── TorgMappingSupport.java               # UPDATE: Censor import
    │   └── VehicleMapper.java                    # UPDATE: Censor import
    ├── security/
    │   └── CurrentUserCensorFactory.java         # UPDATE: both Censor + TorgMarkupService imports
    └── controller/  (15 gated controllers)       # UPDATE: Censor import in each
        ├── ArticleController.java
        ├── CosmController.java
        ├── ItemController.java
        ├── MiracleController.java
        ├── MiracleListController.java
        ├── PerkController.java
        ├── PerkGroupController.java
        ├── PowerController.java
        ├── PowerListController.java
        ├── RaceController.java
        ├── ShardController.java
        ├── SpellController.java
        ├── SpellListController.java
        ├── ThreatController.java
        └── VehicleController.java
```

### Source Code Changes — Phase B: Architecture Test Enforcement

```text
torg-codex/
└── src/test/java/de/paladinsinn/torg/codex/architecture/
    ├── DataPersistenceBoundaryArchitectureTest.java  # NEW — FR-001/FR-002/complementary
    └── AdapterConventionArchitectureTest.java        # MODIFIED — 2 stale methods replaced
```

## Complexity Tracking

Two planning scope changes from the original feature 003 plan are explicitly tracked:

**Scope expansion (feature 002 consolidation)**: The original plan was test-only. This plan adds Java source moves, POM dependency changes, and a Spring bean configuration. All changes are incremental (each task leaves the build passing) and backward-compatible (no API, schema, or behavioral changes).

**Constitution conflict (markup ownership)**: Constitution v2.0.0 Principle I/VI assigns markup to `torg-codex-application`. User authority overrides this to `torg-codex-domain`. The plan proceeds on domain ownership. No FreezeList entry is used; the required resolution is a named implementation deliverable (ADR + constitution amendment, Task A8).

---

## Implementation Phase Outline

*Tasks below are ordered by dependency. Each task must leave `./mvnw compile -DskipTests`
passing before the next task begins.*

### Phase A — Markup Domain Service Relocation (absorbs feature 002)

**A1 — POM: Transfer `commonmark` dependency**

- Remove `org.commonmark:commonmark:0.24.0` from `torg-codex-data/pom.xml` production dependencies
- Add `org.commonmark:commonmark:0.24.0` to `torg-codex-domain/pom.xml` as a `<scope>compile</scope>` dependency (no `<scope>` needed — defaults to compile)
- Rationale: `commonmark` is a pure-Java library with no Spring or JPA coupling; it is permissible in the framework-free domain module (confirmed in research Decision 12)
- Verification: `./mvnw compile -pl torg-codex-domain` passes

**A2 — Domain: Create markup package and introduce framework-free classes**

- Create `de.paladinsinn.torg.codex.domain.markup` package under `torg-codex-domain/src/main/java`
- Introduce all 9 markup classes with the new package declaration. Keep the existing
  `torg-codex-data` copies temporarily so the reactor remains compilable while consumers
  are repointed; the old copies are deleted only in A6.
- Remove the following Spring/CDI annotations (research Decision 11):
  - `@Service` from `TorgMarkupService`
  - `@Component` from `ConditionalBlockProcessor`, `EntityReferenceProcessor`, `GameTokenProcessor`, `MarkdownProcessor`, `RawHtmlProcessor`
- `Censor`, `EntityType`, `GameTokenRegistry` carry no Spring annotations — move only; no annotation changes required
- No method, field, constructor, or behavioral change to the domain copies
- Update any intra-package imports (all moved classes reference only each other and `commonmark`)
- Verification: `./mvnw compile -pl torg-codex-domain` passes; `./mvnw test -pl torg-codex -Dtest="DomainPurityArchitectureTest"` GREEN

**A3 — Migration bridge: Neutralize legacy data markup beans**

- Do not change consumer imports or `TorgEntity` yet; keeping the old package as a
  compile-time bridge ensures this task is independently buildable.
- Remove Spring stereotypes from the temporary legacy data markup copies so A4's new
  domain beans cannot collide with duplicate bean names. These legacy classes are no
  longer referenced and are deleted in A6.
- Verification: `./mvnw compile -DskipTests` passes for the reactor.

**A4 — Application: Create framework-binding Spring configuration**

- Create class `de.paladinsinn.torg.codex.markup.spring.MarkupConfiguration`
  in `torg-codex-application/src/main/java`
- Annotate with `@Configuration`
- Declare `@Bean` methods:
  - `conditionalBlockProcessor()` → `new ConditionalBlockProcessor()` (or equivalent)
  - `entityReferenceProcessor()` → `new EntityReferenceProcessor()`
  - `rawHtmlProcessor()` → `new RawHtmlProcessor()`
  - `gameTokenProcessor()` → `new GameTokenProcessor()`; `GameTokenRegistry` remains a
    static utility and is not a Spring bean
  - `markdownProcessor()` → `new MarkdownProcessor()`
  - `torgMarkupService(ConditionalBlockProcessor, EntityReferenceProcessor, RawHtmlProcessor, GameTokenProcessor, MarkdownProcessor)` → `new TorgMarkupService(...)`
- Import `de.paladinsinn.torg.codex.domain.markup.*` classes
- Ensure the temporary legacy data markup classes are no longer Spring components before
  loading this configuration; they are removed in A6.
- The class lives in `torg-codex-application` POM artifact but outside `de.paladinsinn.torg.codex.application.*`; this follows the same pattern as `de.paladinsinn.drivethru.*` and `de.paladinsinn.security.*` (research Decision 10)
- The existing `@SpringBootApplication` on
  `de.paladinsinn.torg.codex.TorgCodexApplication` scans
  `de.paladinsinn.torg.codex` and therefore discovers
  `de.paladinsinn.torg.codex.markup.spring.MarkupConfiguration` on the application
  module's classpath; no extra `@Import` or scan-root change is required. Verify this
  with an application-context test rather than introducing duplicate registration.
- Add `torg-codex-application/src/test/java/de/paladinsinn/torg/codex/markup/spring/MarkupConfigurationTest.java`
  (or equivalent) to load `MarkupConfiguration` without the web application and assert
  that each processor and `TorgMarkupService` bean is present and injectable.
- Verification: `./mvnw test -pl torg-codex-application` passes

**A5 — Web module: Update all import paths**

Update 33 production files atomically across the web and data modules:
- `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/model/TorgEntity.java`:
  change `data.markup.Censor` → `domain.markup.Censor`
- **16 MapStruct mapper files** (`TorgMappingSupport.java` + 15 `*Mapper.java` interfaces):
  change `import de.paladinsinn.torg.codex.data.markup.Censor;` →
  `import de.paladinsinn.torg.codex.domain.markup.Censor;`
- **`CurrentUserCensorFactory.java`**:
  change `import de.paladinsinn.torg.codex.data.markup.Censor;` → `domain.markup.Censor`
  change `import de.paladinsinn.torg.codex.data.markup.TorgMarkupService;` → `domain.markup.TorgMarkupService`
- **15 REST controller files**:
  change `import de.paladinsinn.torg.codex.data.markup.Censor;` →
  `import de.paladinsinn.torg.codex.domain.markup.Censor;`
- No logic changes; import path updates only
- Verification: `./mvnw compile -pl torg-codex` passes

**A6 — Tests and cleanup: Move tests, remove the legacy markup package, and transfer the provider**

- Move 6 test files from `torg-codex-data/src/test/java/de/paladinsinn/torg/codex/data/markup/`
  to `torg-codex-domain/src/test/java/de/paladinsinn/torg/codex/domain/markup/`
- Update `package` declaration in each test file
- Update import paths for moved production classes (e.g., `ConditionalBlockProcessor`)
- Verify each test class uses no Spring application context (tests instantiate objects directly — already confirmed in research Decision 14)
- Delete the temporary `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/markup/`
  production package only after all consumers and tests use `domain.markup`.
- Remove `org.commonmark:commonmark` from `torg-codex-data/pom.xml`; it is now owned only
  by `torg-codex-domain` and used by its `MarkdownProcessor` provider.
- Verification: `./mvnw clean test` passes with all 6 markup unit tests GREEN and no
  `data.markup` production classes.

**A7 — Verify all architecture tests pass after Phase A**

```bash
./mvnw test -pl torg-codex -Dtest="DomainPurityArchitectureTest,ApplicationPurityArchitectureTest,ModuleBoundaryArchitectureTest,CensoringSingleMechanismArchitectureTest,AdapterConventionArchitectureTest,FreezeListFormatTest,FreezeListEnforcementArchitectureTest"
```

Expected: all GREEN. Any failure at this step must be resolved before proceeding to Phase B.

**A8 — Required governance deliverables (implementation phase tasks, not plan phase)**

The following are named tasks for the implementation phase (`tasks.md`) — they are governance
commitments, not optional:

1. **New ADR** (ADR-017 or next available number): records the markup-to-domain ownership
   decision; states this supersedes constitution v2.0.0 Principle I and Principle VI markup
   assignments; specifies the framework-binding adapter pattern (`MarkupConfiguration` in
   `torg-codex-application`); references both spec 002 and spec 003 as motivation.
2. **Constitution amendment** (v2.0.0 → v2.1.0 minimum): update Principle I module ownership
   table to assign `markup processing` to `torg-codex-domain` rather than
   `torg-codex-application`; update Principle VI to state the markup pipeline is owned by
   `torg-codex-domain` with Spring bean wiring provided by a framework-binding configuration
   in `torg-codex-application`.

---

### Phase B — Architecture Test Enforcement (original feature 003 scope)

*Prerequisites*: Phase A complete; `./mvnw compile -DskipTests` passes on full reactor.

**B1 — Add `DataPersistenceBoundaryArchitectureTest`**

Create `torg-codex/src/test/java/de/paladinsinn/torg/codex/architecture/DataPersistenceBoundaryArchitectureTest.java`
with the following three test methods (exact ArchUnit rule syntax from research Decision 6):

```java
@Test
void dataMustNotReferenceSecurityIntegrationClasses() {
    ArchitectureTestSupport.assertNoUnfrozenViolations(noClasses()
        .that().resideInAnyPackage("de.paladinsinn.torg.codex.data..")
        .should().dependOnClassesThat().resideInAnyPackage("de.paladinsinn.security..")
        .as("torg-codex-data must not reference Security integration classes"));
}

@Test
void dataMustNotReferenceDriveThruRpgIntegrationClasses() {
    ArchitectureTestSupport.assertNoUnfrozenViolations(noClasses()
        .that().resideInAnyPackage("de.paladinsinn.torg.codex.data..")
        .should().dependOnClassesThat().resideInAnyPackage("de.paladinsinn.drivethru..")
        .as("torg-codex-data must not reference DriveThruRPG integration classes"));
}

@Test
void dataMustNotImportSpringSecurityFrameworkClasses() {
    ArchitectureTestSupport.assertNoUnfrozenViolations(noClasses()
        .that().resideInAnyPackage("de.paladinsinn.torg.codex.data..")
        .should().dependOnClassesThat().resideInAnyPackage("org.springframework.security..")
        .as("torg-codex-data must not import Spring Security framework classes"));
}
```

**Key note**: No `allowEmptyShould(true)` is needed (data package is non-empty). No FreezeList
entry is permitted (FR-011 of spec 003). No FR-010 carve-out is needed — markup has been removed
from `torg-codex-data` in Phase A, so `de.paladinsinn.torg.codex.data.*` is now persistence-only
and the rules pass cleanly without any exemption.

Verification: `./mvnw test -pl torg-codex -Dtest="DataPersistenceBoundaryArchitectureTest"` GREEN

**B2 — Fix two stale tests in `AdapterConventionArchitectureTest`**

Replace the two failing test methods with corrected assertions (research Decision 5):

| Old method name | New method name | Old package assertion | Correct package assertion |
|---|---|---|---|
| `driveThruRpgOutboundAdapterLivesUnderDataAdapterOutHttp` | `driveThruRpgOutboundAdapterLivesUnderDriveThruAdapterOutHttp` | `de.paladinsinn.torg.codex.data.adapter.out.http` | `de.paladinsinn.drivethru.adapter.out.http` |
| `domainEventBridgeOutboundAdapterLivesUnderDataAdapterOutEvent` | `domainEventBridgeOutboundAdapterLivesUnderDriveThruAdapterOutEvent` | `de.paladinsinn.torg.codex.data.adapter.out.event` | `de.paladinsinn.drivethru.adapter.out.event` |

Verification: `./mvnw test -pl torg-codex -Dtest="AdapterConventionArchitectureTest"` GREEN
(all 5 test methods pass; 0 failures)

**B3 — Full build verification**

```bash
./mvnw clean verify
```

Expected:
- All modules compile cleanly
- All unit tests pass (markup unit tests now in `torg-codex-domain`; architecture tests in `torg-codex`)
- All integration tests (`*IT`) pass under Failsafe with Testcontainers PostgreSQL
- Zero `DataPersistenceBoundaryArchitectureTest` failures
- Zero `AdapterConventionArchitectureTest` failures
- `CharacterizationReplayTest` / `CensoringDifferentialTest` pass byte-for-byte identical (zero REST behavior change)
- `CensoringSingleMechanismArchitectureTest` all 4 tests GREEN (censoring chain unchanged)

---

## Backward Compatibility Invariants

| Invariant | Maintained by |
|---|---|
| All 17 catalog REST endpoint responses byte-identical | Import-path-only changes to mappers/controllers; no logic change |
| `Censor.apply(String)` behavior unchanged | Logic not touched; only package and annotation changes |
| `TorgMarkupService.render(String, Set<String>)` behavior unchanged | Logic not touched |
| Markup pipeline order preserved | Order in `TorgMarkupService.render()` not changed |
| Markup public methods and factories remain source-compatible after import updates | `render`, `apply`, `of`, and `unauthenticated` signatures remain unchanged; the old `data.markup` package is intentionally not re-exported because data must be persistence-only |
| No JPA entity field or schema change | No entity/entity-superclass field changes; `TorgEntity.censor` `@Transient` field preserved |
| No Liquibase changeset touched | Zero changeset files modified or added |
| `de.paladinsinn.security.*` and `de.paladinsinn.drivethru.*` FQCNs unchanged | These packages/classes are not touched by this feature |

---

## Verification Commands Summary

```bash
# Phase A intermediate check — after A2 (markup in domain, annotations removed)
./mvnw test -pl torg-codex -Dtest="DomainPurityArchitectureTest"

# Phase A intermediate check — after A6 (tests moved to domain)
./mvnw test -pl torg-codex-domain

# Phase A gate — before starting Phase B
./mvnw test -pl torg-codex -Dtest="DomainPurityArchitectureTest,ApplicationPurityArchitectureTest,ModuleBoundaryArchitectureTest,CensoringSingleMechanismArchitectureTest"

# Phase B — new data boundary test
./mvnw test -pl torg-codex -Dtest="DataPersistenceBoundaryArchitectureTest"

# Phase B — corrected stale tests
./mvnw test -pl torg-codex -Dtest="AdapterConventionArchitectureTest"

# Full build (requires Docker for Testcontainers)
./mvnw clean verify

# POM boundary check (SC-005 of spec 003)
./mvnw dependency:tree -pl torg-codex-data | grep -i security
# expected: no output

# Deliberate regression test (SC-003 of spec 003) — local branch only
# 1. Add import de.paladinsinn.security.DriveThruUserDetails; to any torg-codex-data class
# 2. ./mvnw test -pl torg-codex -Dtest="DataPersistenceBoundaryArchitectureTest" — expect FAILURE
# 3. Revert the change and re-run — expect SUCCESS
```
