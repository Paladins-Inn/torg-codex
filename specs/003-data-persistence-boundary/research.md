# Research: Enforce Data-Persistence-Only Module Boundary

**Branch**: `003-data-persistence-boundary` | **Date**: 2026-08-16

This document records the Phase 0 research findings for feature
`003-data-persistence-boundary`. All unknowns identified from the spec and plan Technical
Context are resolved here. No NEEDS CLARIFICATION items remain.

---

## Decision 1 — ADR-016 physical code move status

**Question**: Has the physical relocation of `de.paladinsinn.security.*` and
`de.paladinsinn.drivethru.*` from `torg-codex-data` to `torg-codex-application` already been
completed, or is it still pending?

**Decision**: Fully completed. Both packages reside exclusively in `torg-codex-application`.

**Rationale**: Direct source scan of
`torg-codex-data/src/main/java` contains zero files under `de/paladinsinn/security` or
`de/paladinsinn/drivethru`. All 26 classes (10 in `de.paladinsinn.security`, 16 in
`de.paladinsinn.drivethru`) are present in `torg-codex-application/src/main/java`. ADR-016
(`docs/modules/arc42/pages/09_architecture_decisions/016_application-integration-boundaries.adoc`)
is marked `Accepted` as of 2026-08-16.

**Alternatives considered**: None — state-of-the-codebase determination, not a design choice.

---

## Decision 2 — torg-codex-data production class boundary compliance

**Question**: Do any `torg-codex-data` production classes currently import
`de.paladinsinn.security.*`, `de.paladinsinn.drivethru.*`, or
`org.springframework.security.*`?

**Decision**: Zero violations exist. The new `DataPersistenceBoundaryArchitectureTest` will
pass with zero violations from day one (SC-002 satisfied immediately).

**Rationale**: A full grep of
`torg-codex-data/src/main/java/**/*.java` for the import patterns
`de.paladinsinn.security`, `de.paladinsinn.drivethru`, and `springframework.security`
returned no matches. The `torg-codex-data` production source tree contains only:
- `de.kaiserpfalz.liquibase.*` (5 classes: Liquibase entity support)
- `de.paladinsinn.torg.codex.data.adapter.out.persistence.*` (17 JPA persistence adapters)
- `de.paladinsinn.torg.codex.data.mapper.*` (17 MapStruct mappers + `ValueObjectMapper`)
- `de.paladinsinn.torg.codex.data.markup.*` (9 markup pipeline classes — in scope for
  feature 002 only, carved out of data-purity rules by FR-010)
- `de.paladinsinn.torg.codex.data.model.*` (17 JPA entity classes + `TorgEntity`,
  `ClearanceLevel`, `ClearanceLevelConverter`, `DifficultyNumber`, `VehicleWeapon`)
- `de.paladinsinn.torg.codex.data.repository.*` (17 Spring Data JPA repositories)
- `de.paladinsinn.torg.codex.data.TorgDataConfiguration` and `EnableTorgData`
- `de.paladinsinn.torg.codex.data.TorgCodexDataApplication`

None of these classes reference forbidden packages.

**Alternatives considered**: None — compliance determination, not a design choice.

---

## Decision 3 — torg-codex-data POM dependency boundary compliance

**Question**: Does `torg-codex-data/pom.xml` currently declare any Spring Security,
`de.paladinsinn.security.*`, or `de.paladinsinn.drivethru.*` production-scoped dependencies?

**Decision**: No such dependencies exist. FR-004 and FR-005 are already satisfied. No POM
change is required by this feature.

**Rationale**: Inspection of `torg-codex-data/pom.xml` found the following production-scope
dependencies only:
- `torg-codex-domain` (internal reactor module)
- `torg-codex-application` (internal reactor module — needed for port interfaces e.g.
  `CatalogPersistencePort`)
- `spring-boot-starter-data-jpa` (JPA/Hibernate/Spring Data)
- `spring-boot-starter-liquibase`
- `postgresql` (runtime)
- `hibernate-validator` + `expressly` (runtime, bean validation EL)
- `micrometer-tracing-bridge-brave`, `datasource-micrometer-*` (observability)
- `commonmark` (Markdown rendering — markup pipeline, out of scope for this feature)
- `mapstruct`, `lombok` (code generation)

No `spring-boot-starter-security`, `spring-security-core`, `spring-security-web`, or any
`de.paladinsinn.*` integration artifact appears at compile/runtime scope. The
`torg-codex-data` dependency on `torg-codex-application` is at compile scope (needed for port
adapter implementations) — this is expected and does not introduce a security dependency
because `de.paladinsinn.security.*` classes in `torg-codex-application` are not imported by
`torg-codex-data` production code (verified in Decision 2).

**Alternatives considered**: None — compliance determination, not a design choice.

---

## Decision 4 — TorgDataConfiguration ComponentScan boundary compliance

**Question**: Does the `@ComponentScan` in `TorgDataConfiguration` inadvertently include
`de.paladinsinn.security` or `de.paladinsinn.drivethru` package roots?

**Decision**: No. `TorgDataConfiguration` scans only `de.paladinsinn.torg.codex.data`.
FR-006 is already satisfied. No configuration change is required.

**Rationale**:
```java
@ComponentScan({ "de.paladinsinn.torg.codex.data" })
@EntityScan({ "de.paladinsinn.torg.codex.data.model", "de.kaiserpfalz.liquibase" })
@EnableJpaRepositories({ "de.paladinsinn.torg.codex.data.repository", "de.kaiserpfalz.liquibase" })
public class TorgDataConfiguration {}
```
The `@ComponentScan` base package `de.paladinsinn.torg.codex.data` does NOT subsume
`de.paladinsinn.security` or `de.paladinsinn.drivethru` (different top-level package trees).
The `@EntityScan` and `@EnableJpaRepositories` roots are also limited to persistence packages.
No Spring bean from the Security or DriveThruRPG integration is wired by `@EnableTorgData`.

**Alternatives considered**: None — compliance determination, not a design choice.

---

## Decision 5 — Stale architecture test identification and correction strategy

**Question**: Which tests in `AdapterConventionArchitectureTest` are stale, and what are the
correct replacement assertions?

**Decision**: Two tests are stale and must be replaced with assertions against the
ADR-016-established package locations.

**Rationale**:

| Test method | Old (stale) assertion | Correct replacement assertion |
|---|---|---|
| `driveThruRpgOutboundAdapterLivesUnderDataAdapterOutHttp` | `DriveThruRpgProductAdapter` at `de.paladinsinn.torg.codex.data.adapter.out.http` | `DriveThruRpgProductAdapter` at `de.paladinsinn.drivethru.adapter.out.http` |
| `domainEventBridgeOutboundAdapterLivesUnderDataAdapterOutEvent` | `SpringDomainEventPublisherAdapter` at `de.paladinsinn.torg.codex.data.adapter.out.event` | `SpringDomainEventPublisherAdapter` at `de.paladinsinn.drivethru.adapter.out.event` |

Verified by inspecting package declarations directly:
- `DriveThruRpgProductAdapter.java`: `package de.paladinsinn.drivethru.adapter.out.http;`
- `SpringDomainEventPublisherAdapter.java`: `package de.paladinsinn.drivethru.adapter.out.event;`

Both classes reside in `torg-codex-application/src/main/java` and are already on the
`ArchitectureTestSupport.IMPORTED_CLASSES` classpath (which imports
`de.paladinsinn.drivethru`, `de.paladinsinn.security`, `de.paladinsinn.torg.codex`, and
`de.kaiserpfalz.liquibase`). The corrected assertions will pass immediately against the
current codebase without any production code change.

**Alternatives considered**:
- *Delete the tests without replacement*: Rejected. The tests guard a meaningful invariant
  (confirming these adapters exist and are correctly placed). Deleting them leaves a gap.
- *Suppress via FreezeList*: Rejected per FR-011. The stale assertion is a defect in the
  test, not in the code; it must be corrected at source.
- *Rename test methods*: After replacement the old names no longer describe the assertion
  accurately. The replacement methods use updated names that reflect the ADR-016 locations.

---

## Decision 6 — ArchUnit rule design for DataPersistenceBoundaryArchitectureTest

**Question**: What is the precise ArchUnit rule syntax for FR-001 and FR-002? Should a third
rule covering `org.springframework.security.*` be included?

**Decision**: Two `noClasses` rules, named exactly as specified in the spec acceptance
scenarios. A third rule covering `org.springframework.security.*` is added as a complementary
defensive guard (covers direct framework-level security imports, not just integration
sub-package imports).

**Rationale**:

*FR-001 rule (security integration):*
```java
noClasses()
    .that().resideInAnyPackage("de.paladinsinn.torg.codex.data..")
    .should().dependOnClassesThat().resideInAnyPackage("de.paladinsinn.security..")
    .as("torg-codex-data must not reference Security integration classes")
```

*FR-002 rule (DriveThruRPG integration):*
```java
noClasses()
    .that().resideInAnyPackage("de.paladinsinn.torg.codex.data..")
    .should().dependOnClassesThat().resideInAnyPackage("de.paladinsinn.drivethru..")
    .as("torg-codex-data must not reference DriveThruRPG integration classes")
```

*Complementary Spring Security framework rule:*
```java
noClasses()
    .that().resideInAnyPackage("de.paladinsinn.torg.codex.data..")
    .should().dependOnClassesThat().resideInAnyPackage("org.springframework.security..")
    .as("torg-codex-data must not import Spring Security framework classes")
```

The `ArchitectureTestSupport.IMPORTED_CLASSES` importer already scans all four required
package roots (`de.paladinsinn.torg.codex`, `de.paladinsinn.drivethru`,
`de.paladinsinn.security`, `de.kaiserpfalz.liquibase`). No import option change is needed.

The `allowEmptyShould(true)` option is NOT needed because the data package is non-empty
(ArchUnit will find classes to evaluate). This matches the pattern in
`DomainPurityArchitectureTest` (which also does not use `allowEmptyShould`).

**Alternatives considered**:
- *Use `importPackages` scoped to data only*: Rejected. `ArchitectureTestSupport.IMPORTED_CLASSES`
  is shared state; changing its scope would affect every other test in the class. All tests
  use this shared importer to guarantee consistent classpath visibility.
- *No Spring Security rule (only check internal packages)*: The two required rules (FR-001,
  FR-002) are sufficient. The Spring Security framework rule is additive defense; it is safe
  to include since the current codebase already passes it (Decision 2).

---

## Decision 7 — FreezeList interaction

**Question**: Do any of the new data-purity rules require FreezeList entries? Does adding
these rules risk triggering existing FreezeList entries?

**Decision**: No FreezeList entries are needed or appropriate for this feature's rules. The
existing FL-007 entry is not affected.

**Rationale**:
- Decision 2 confirmed zero violations in `torg-codex-data` — both new ArchUnit rules pass
  immediately on the current codebase.
- FR-011 explicitly prohibits using the FreezeList to suppress the new data-purity rules.
- The FL-007 entry (`AMQP publishers/listeners`) covers a different rule
  (`applicationDependsOnlyOnDomainAndJava` in `ModuleBoundaryArchitectureTest`) and a
  class/dependency that does not exist. It cannot interfere with the new rules.
- The `FreezeListEntry.matches(violation)` predicate only suppresses violations whose text
  contains the registered `violatingClassOrDependency` string. The new rules' violation
  messages would contain `de.paladinsinn.torg.codex.data.*` class names; FL-007's registered
  dependency is `AMQP publishers/listeners (no such class exists)`, which is a prose string
  that will never appear in a real ArchUnit violation report — so FL-007 cannot accidentally
  suppress any new violation.

**Alternatives considered**: None applicable.

---

## Decision 8 — Impact on other architecture tests

**Question**: Do the new test additions or the two stale test corrections affect any other
architecture test, the `FreezeListEnforcementArchitectureTest`, or the `FreezeListFormatTest`?

**Decision**: No other architecture test is affected. No FreezeList format change is needed.

**Rationale**:
- `DataPersistenceBoundaryArchitectureTest` is a new file; it does not modify shared
  infrastructure.
- The two corrected methods in `AdapterConventionArchitectureTest` are self-contained
  existence/package assertions using `ArchitectureTestSupport.IMPORTED_CLASSES`; they do not
  reference other test methods or shared state.
- The `FreezeListEnforcementArchitectureTest` asserts that the freeze list loads with at least
  1 entry and that FL-007 parses correctly. Neither new tests nor corrected tests alter
  `freeze-list.md`, so that test's assertions remain valid.
- The `FreezeListFormatTest` validates raw row format of `freeze-list.md`; no change to that
  file is made.
- `ModuleBoundaryArchitectureTest.applicationDependsOnlyOnDomainAndJava` asserts that
  `de.paladinsinn.torg.codex.application.*` does not depend on `de.paladinsinn.drivethru.*`
  or `de.paladinsinn.security.*`. This rule is about the `application` package namespace, not
  `data`; it is orthogonal to the new data-purity rules.

**Alternatives considered**: None applicable.

---

# Research Extension: Markup-to-Domain Consolidation (Feature 002 Integration)

*Added to this document per user authoritative instruction consolidating feature 002 into
feature 003. Decisions 9–16 cover all unknowns introduced by the expanded scope.*

---

## Decision 9 — Markup ownership: user authority resolution of constitution conflict

**Question**: Constitution v2.0.0 Principle I/VI assigns markup to `torg-codex-application`.
Feature 002 spec (FR-012/FR-013) requires an ADR and constitution amendment. The user
instruction places markup in `torg-codex-domain`. Which authority governs?

**Decision**: The user's explicit instruction is authoritative and supersedes the current
constitution text. Markup moves to `torg-codex-domain`. A new ADR and a constitution amendment
(v2.0.0 → v2.1.0 minimum) are required deliverables of the implementation phase.

**Rationale**: The user's directive is: "markup implementation moves into torg-codex-domain as
a framework-independent domain service." The constitution is a living governance document that
can be amended via ADR. Feature 002 spec FR-012/FR-013 already defines the amendment process.
The plan proceeds on domain ownership; the plan does NOT amend the constitution (that is an
implementation task), but names it explicitly as a non-optional deliverable (Task A8 in the plan).

**Alternatives considered**:
- *Follow constitution v2.0.0 literally (markup to application)*: Rejected per user instruction.
- *Split markup between domain (pure pipeline) and application (spring wiring) without constitution amendment*: The current plan already does this (domain for pure classes, application for Spring config), but the constitution must still be updated to reflect that the domain is now the authoritative home.

---

## Decision 10 — Framework-binding Spring configuration: package placement

**Question**: The framework-binding Spring `@Configuration` class must live in
`torg-codex-application` per spec 002 FR-003. But `ApplicationPurityArchitectureTest` forbids
Spring imports in `de.paladinsinn.torg.codex.application.*`. Where exactly in
`torg-codex-application` should the configuration class live?

**Decision**: The class `MarkupConfiguration` lives at
`de.paladinsinn.torg.codex.markup.spring.MarkupConfiguration` within the
`torg-codex-application` Maven artifact.

**Rationale**:
- `ApplicationPurityArchitectureTest.applicationContainsNoSpringOrJpaInfrastructureImports`
  only restricts `de.paladinsinn.torg.codex.application.*` (and its sub-packages). A class at
  `de.paladinsinn.torg.codex.markup.spring.*` is outside that checked package tree.
- `ModuleBoundaryArchitectureTest.applicationDependsOnlyOnDomainAndJava` also only checks
  `de.paladinsinn.torg.codex.application.*` — no conflict.
- This pattern mirrors how `de.paladinsinn.drivethru.*` and `de.paladinsinn.security.*` live
  in `torg-codex-application` outside the checked `application.*` sub-tree and use Spring
  freely. The markup Spring config follows the same convention.
- The class IS in `ArchitectureTestSupport.IMPORTED_CLASSES` scope (the importer scans
  `de.paladinsinn.torg.codex` and all sub-packages), enabling future architecture rules to
  cover it.
- The existing `@SpringBootApplication` on
  `de.paladinsinn.torg.codex.TorgCodexApplication` is rooted at
  `de.paladinsinn.torg.codex`, so normal component scanning discovers the configuration
  class across the application module's dependency artifact. No extra `@Import`,
  `@AutoConfiguration`, or scan-root change is required; adding one would risk duplicate
  registration. Task A4 must verify this with an application-context test.

**Alternatives considered**:
- *Place config in `torg-codex-data`*: Rejected. The spec 002 FR-003 requires the adapter in
  `torg-codex-application`. Data should have no knowledge of Spring bean wiring for domain classes.
- *Place config in `de.paladinsinn.torg.codex.application.*`*: Rejected. Would violate
  `ApplicationPurityArchitectureTest`.
- *Place config in `de.paladinsinn.markup.*`*: Feasible, but would place markup Spring wiring
  outside the `de.paladinsinn.torg.codex.*` namespace and outside ArchUnit scope — less coherent.
- *Auto-configure via Spring Boot autoconfiguration*: Rejected for this move because the
  existing application component scan already discovers the configuration and an
  additional auto-configuration path would be redundant.

---

## Decision 11 — Spring annotation removal from markup classes

**Question**: Which markup classes carry Spring annotations and must have them removed when
moving to `torg-codex-domain`?

**Decision**: Five classes carry `@Component`, one carries `@Service`. All six annotations must
be removed. `Censor`, `EntityType`, and `GameTokenRegistry` carry no Spring annotations and
require only a package declaration update.

**Rationale** (verified by source inspection):

| Class | Spring annotation(s) to remove |
|---|---|
| `TorgMarkupService` | `@Service` |
| `ConditionalBlockProcessor` | `@Component` |
| `EntityReferenceProcessor` | `@Component` |
| `GameTokenProcessor` | `@Component` |
| `MarkdownProcessor` | `@Component` |
| `RawHtmlProcessor` | `@Component` |
| `Censor` | none (already annotation-free) |
| `EntityType` | none (enum/value class) |
| `GameTokenRegistry` | none (plain class) |

After removal, `DomainPurityArchitectureTest.domainContainsNoSpringOrJpaImports` passes
because `org.springframework.stereotype.*` imports are gone and `commonmark` has no Spring
coupling. The import `import org.springframework.stereotype.Service;` (and `Component`) must
also be removed from the source files — not just the annotations themselves.

`GameTokenRegistry` is a static utility with a private constructor, so it is not instantiated
or registered as a bean. `GameTokenProcessor` calls its static lookup directly and therefore
has a no-argument constructor in the framework-binding configuration.

**Alternatives considered**: None — removing framework annotations is the definitional
requirement of the move to a framework-free domain module (spec 002 FR-002).

---

## Decision 12 — `commonmark` dependency: module ownership after markup move

**Question**: `org.commonmark:commonmark:0.24.0` is currently a production dependency of
`torg-codex-data`. When `MarkdownProcessor` moves to `torg-codex-domain`, where does the
dependency go?

**Decision**: Remove `commonmark` from `torg-codex-data/pom.xml`; add it to
`torg-codex-domain/pom.xml` as a production dependency at version `0.24.0`.

**Rationale**:
- `commonmark` is a pure-Java Markdown rendering library with no Spring Framework, JPA,
  Hibernate, or Jakarta CDI dependency. It introduces no framework coupling.
- `torg-codex-domain`'s `pom.xml` comment explicitly anticipates this pattern: "Import only
  for managed dependency *versions*... No Spring/JPA/Hibernate artifact is ever declared
  below." Adding `commonmark` is permitted — it is neither Spring nor JPA.
- `torg-codex-domain` currently has no external library dependency beyond `jakarta.validation-api`
  and Lombok; adding `commonmark` is a minimal footprint increase.
- Pre-existing discrepancy: ADR-011 specifies `flexmark-java` but the current codebase uses
  `commonmark`. This feature does NOT resolve the discrepancy (out of scope per spec 002
  Assumptions); the library travels with the class unchanged.

**Alternatives considered**:
- *Keep `commonmark` in `torg-codex-data`*: Rejected — `MarkdownProcessor` is moving to
  domain and would lose access to its compile-time dependency. `torg-codex-domain` does not
  depend on `torg-codex-data`.
- *Add `commonmark` to both modules*: Rejected — unnecessary duplication; only domain needs it.

---

## Decision 13 — `TorgEntity` impact: Censor import in a JPA entity base class

**Question**: `TorgEntity` is a `@MappedSuperclass` JPA entity in `torg-codex-data.model` that
imports `Censor` from `de.paladinsinn.torg.codex.data.markup.Censor`. When markup moves, what
happens to this import?

**Decision**: Update the import in `TorgEntity.java` from
`de.paladinsinn.torg.codex.data.markup.Censor` to
`de.paladinsinn.torg.codex.domain.markup.Censor`. No behavioral change.

**Rationale**:
- `torg-codex-data` already declares `torg-codex-domain` as a production dependency at compile
  scope. The import update is a valid cross-module reference using an existing dependency.
- `TorgEntity` uses `Censor` for its `@Transient` field and `withCensor(Censor)` / `render(String)`
  helper methods. The `Censor` API (`of(...)`, `unauthenticated(...)`, `apply(String)`) is
  unchanged by the relocation; only the package path changes.
- `TorgEntity` is a persistence-layer base class (`@MappedSuperclass`). After the import
  update, it references `domain.markup.Censor` — a domain type. This is acceptable because:
  `torg-codex-data` is explicitly allowed to depend on `torg-codex-domain` (hexagonal
  architecture outer-to-inner direction). The `DataPersistenceBoundaryArchitectureTest` rules
  will verify that `de.paladinsinn.torg.codex.data.*` does NOT reference
  `de.paladinsinn.security.*` or `de.paladinsinn.drivethru.*` — a reference to
  `de.paladinsinn.torg.codex.domain.markup.*` is NOT prohibited by those rules.

**Alternatives considered**:
- *Move `withCensor(Censor)` / `render(String)` out of TorgEntity*: Rejected as out of scope
  for this feature; would require JPA entity refactoring and schema/API risk.

---

## Decision 14 — Impact on `DomainPurityArchitectureTest` after markup move

**Question**: After markup classes move to `torg-codex-domain`, will
`DomainPurityArchitectureTest.domainContainsNoSpringOrJpaImports` pass or fail? The rule checks
all `de.paladinsinn.torg.codex.domain.*` classes.

**Decision**: The rule passes after markup move, **provided** Spring annotations and their
imports are removed (Decision 11). No FreezeList entry is needed.

**Rationale**:
- The rule: `noClasses().that().resideInAnyPackage("de.paladinsinn.torg.codex.domain..")
  .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..", "org.hibernate..")`
- After removing `@Service`/`@Component` and their `import org.springframework.stereotype.*`
  statements from the relocated classes, no class in `domain.markup` imports any Spring,
  JPA, or Hibernate type.
- `commonmark` types (`Parser`, `HtmlRenderer`, `Node`) are in `org.commonmark.*` — not
  `org.springframework.*`, `jakarta.persistence.*`, or `org.hibernate.*`. No violation.
- The 6 moved markup unit tests are excluded from the ArchUnit scan
  (`ImportOption.DO_NOT_INCLUDE_TESTS`).

**Alternatives considered**: None — Spring annotation removal is the prerequisite that makes
this test pass; the plan already requires it.

---

## Decision 15 — Impact on `ApplicationPurityArchitectureTest` after adding `MarkupConfiguration`

**Question**: Will `ApplicationPurityArchitectureTest` catch violations if `MarkupConfiguration`
is placed in `de.paladinsinn.torg.codex.markup.spring.*` within `torg-codex-application`?

**Decision**: No violations. The rule covers only `de.paladinsinn.torg.codex.application.*`;
`MarkupConfiguration` at `de.paladinsinn.torg.codex.markup.spring.*` is outside the checked
package tree.

**Rationale**:
- `applicationContainsNoSpringOrJpaInfrastructureImports` checks
  `de.paladinsinn.torg.codex.application..*`
- `applicationContainsNoTransactionalAnnotations` checks the same scope
- Neither rule applies to `de.paladinsinn.torg.codex.markup.spring.*`
- This is consistent with how `de.paladinsinn.drivethru.*` and `de.paladinsinn.security.*`
  (both in `torg-codex-application` artifact, both using Spring) are not checked by
  `ApplicationPurityArchitectureTest`

**Alternatives considered**: None — package placement outside `application.*` is the correct
pattern; no rule change needed.

---

## Decision 16 — FR-010 exemption becomes moot after markup move

**Question**: Spec 003 FR-010 states: "The markup package (`de.paladinsinn.torg.codex.data.markup.*`)
is explicitly excluded from the scope of data-purity rules introduced by this feature; the
architecture test MUST NOT rule against markup classes remaining in `torg-codex-data`."
After Phase A removes markup from `torg-codex-data`, how does this interact with
`DataPersistenceBoundaryArchitectureTest`?

**Decision**: FR-010 becomes a non-issue. No carve-out clause, no `allowEmptyShould`, no
additional `except()` qualifier is needed in the ArchUnit rules for `DataPersistenceBoundaryArchitectureTest`.

**Rationale**:
- FR-010 was a temporary accommodation for the pre-consolidation world where markup was still
  in `torg-codex-data` when the boundary rules were first written.
- After Phase A, `de.paladinsinn.torg.codex.data.markup` no longer exists. The three
  `DataPersistenceBoundaryArchitectureTest` rules check for Security/DriveThruRPG/Spring-Security
  references — none of which markup ever introduced (research Decision 2 confirmed zero
  violations pre-move). The markup removal makes the rules simpler and cleaner.
- Phase B adds the test AFTER Phase A completes, so the rules are evaluated against a
  markup-free `torg-codex-data` from day one.

**Alternatives considered**: None — the entire point of this consolidation is to reach the
clean end state where FR-010's exception is unnecessary.
