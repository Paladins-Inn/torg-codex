# Feature Specification: Enforce Data-Persistence-Only Module Boundary

**Feature Branch**: `003-data-persistence-boundary`

**Created**: 2026-08-16

**Status**: Draft

**Input**: User description: "torg-codex-data shall contain persistence only; Security and DriveThruRPG integration shall be housed in torg-codex-application. Create/update exactly one feature specification for this request, not implementation code. Inspect the current repository, constitution v2.0.0, ADR-016 and related ADRs, existing specs including specs/001-unify-censoring-authorization, specs/002-markup-to-domain, current module contents/POMs/package ownership, and architecture documentation. The specification must define the source/package/dependency boundary, preserve fully qualified public API behavior where possible, preserve REST/security/censoring behavior, persistence schema/data and Liquibase ownership, and require tests/architecture checks for absence of Security/DriveThru code and dependencies in torg-codex-data. Keep scope distinct from the already specified markup-to-domain feature: do not include moving markup."

## User Scenarios & Testing *(mandatory)*

<!--
  IMPORTANT: User stories are prioritized as user journeys ordered by importance.
  Each story is independently testable and delivers standalone value.
-->

### User Story 1 - Build immediately catches any reintroduction of Security or DriveThruRPG code in torg-codex-data (Priority: P1)

A contributor who accidentally introduces a Security or DriveThruRPG import into a `torg-codex-data` production class — for example, by adding a Spring Security type to a JPA entity, repository, or persistence adapter — sees the build fail at the automated architecture test step before the change can be merged. The failure message names the offending class and the violated rule, so the contributor can locate and remove the prohibited dependency without a code-review round-trip.

**Why this priority**: ADR-016 (Accepted 2026-08-16) established that `de.paladinsinn.security.*` and `de.paladinsinn.drivethru.*` must live in `torg-codex-application`, not `torg-codex-data`. The physical code move has been completed. However, no automated architecture test currently enforces this boundary on `torg-codex-data`. Without enforcement, the boundary can silently erode in future contributions. Adding the architecture test makes the ADR durable and self-policing.

**Independent Test**: Can be fully tested by adding a deliberate `de.paladinsinn.security.*` import to any `torg-codex-data` production class on a local branch, running `./mvnw clean verify`, and confirming the build fails with the new data-purity architecture rule; removing the import and confirming the build passes again.

**Acceptance Scenarios**:

1. **Given** a build with the new `DataPersistenceBoundaryArchitectureTest` in place, **When** any production class in `de.paladinsinn.torg.codex.data.*` imports any type from `de.paladinsinn.security.*`, **Then** the architecture test fails, naming the offending class and the rule `"torg-codex-data must not reference Security integration classes"`.
2. **Given** a build with the new `DataPersistenceBoundaryArchitectureTest` in place, **When** any production class in `de.paladinsinn.torg.codex.data.*` imports any type from `de.paladinsinn.drivethru.*`, **Then** the architecture test fails, naming the offending class and the rule `"torg-codex-data must not reference DriveThruRPG integration classes"`.
3. **Given** the current codebase state (where `torg-codex-data` contains only JPA, Liquibase, persistence adapters, and the markup package pending spec 002), **When** the full build (`./mvnw clean verify`) runs, **Then** the new data-purity architecture test passes with zero violations.

---

### User Story 2 - Architecture tests correctly reflect the ADR-016-established boundary (Priority: P1)

A maintainer who reads the architecture test suite understands exactly where Security and DriveThruRPG integration live and which tests guard that placement. Two tests that were written before the ADR-016 code move (`driveThruRpgOutboundAdapterLivesUnderDataAdapterOutHttp`, `domainEventBridgeOutboundAdapterLivesUnderDataAdapterOutEvent`) currently assert that `DriveThruRpgProductAdapter` and `SpringDomainEventPublisherAdapter` live under `de.paladinsinn.torg.codex.data.adapter.out.*`, which is no longer true. These tests fail on the current codebase and must be replaced with accurate assertions that match the actual `de.paladinsinn.drivethru.adapter.out.*` locations.

**Why this priority**: A failing architecture test erodes trust in the entire test suite. When colleagues see multiple test failures they may assume tests are unreliable and disable or ignore them. Correcting the stale assertions is a prerequisite for the architecture test suite to be trusted and actionable.

**Independent Test**: Can be fully tested by running `./mvnw test -pl torg-codex` and confirming that no `AdapterConventionArchitectureTest` test fails; and by deliberate regression — moving `DriveThruRpgProductAdapter` back to the old package path — confirming the new tests detect the violation.

**Acceptance Scenarios**:

1. **Given** the current codebase (where `DriveThruRpgProductAdapter` lives at `de.paladinsinn.drivethru.adapter.out.http` in `torg-codex-application`), **When** the full build runs, **Then** all `AdapterConventionArchitectureTest` tests pass with zero failures.
2. **Given** the replaced architecture tests assert the correct package `de.paladinsinn.drivethru.adapter.out.http` for `DriveThruRpgProductAdapter`, **When** a hypothetical regression moves the class back to `de.paladinsinn.torg.codex.data.adapter.out.http`, **Then** the corrected test detects the violation.
3. **Given** the replaced architecture tests assert the correct package `de.paladinsinn.drivethru.adapter.out.event` for `SpringDomainEventPublisherAdapter`, **When** the full build runs against the current codebase, **Then** all such tests pass without freeze-list suppression.

---

### User Story 3 - Module POM dependency boundary is verifiable and clean (Priority: P2)

A security auditor or module reviewer inspecting the `torg-codex-data` Maven POM can confirm at a glance that the module does not depend on Spring Security, Spring Web, or any DriveThruRPG artifact as a direct production dependency. The POM expresses only persistence concerns: JPA/Hibernate, Liquibase, and the data platform dependencies.

**Why this priority**: A clean POM is the structural guarantee that even if an architect imports a security class via a transitive path, the compiler-level boundary remains intact. It is the foundation upon which the class-level architecture tests (User Story 1) make sense. Preventing direct production POM dependencies on Spring Security is a lower-risk gate with lasting value.

**Independent Test**: Can be fully tested by inspecting `torg-codex-data/pom.xml` for the absence of `spring-boot-starter-security`, `spring-security-core`, `spring-security-web`, and any `de.paladinsinn.*` artifact (security/drivethru) as a production-scoped dependency; and by running `./mvnw dependency:tree -pl torg-codex-data --include=org.springframework.security` and confirming no output.

**Acceptance Scenarios**:

1. **Given** the `torg-codex-data/pom.xml`, **When** it is inspected for Spring Security artifacts at `compile` or `runtime` scope, **Then** none are found: no `spring-boot-starter-security`, `spring-security-core`, `spring-security-web`, or equivalent.
2. **Given** the `torg-codex-data/pom.xml`, **When** it is inspected for direct `de.paladinsinn.security.*` or `de.paladinsinn.drivethru.*` artifacts, **Then** none are declared as direct production dependencies.
3. **Given** the production module configuration (excluding test scope), **When** the `torg-codex-data` POM declares its `@ComponentScan` in `TorgDataConfiguration`, **Then** the scan covers only persistence-relevant package roots and does not name `de.paladinsinn.security` or `de.paladinsinn.drivethru`.

---

### Edge Cases

- What if `torg-codex-application` exposes a Security or DriveThruRPG class as a return type of a port interface defined in the same module? `torg-codex-data` implements application ports (e.g., `CatalogPersistencePort`) and depends on `torg-codex-application`. The data-purity architecture test must check for direct use of `de.paladinsinn.security.*` or `de.paladinsinn.drivethru.*` types in torg-codex-data classes, regardless of whether that use is through a direct import or via a port method signature. If any application port leaks a security/drivethru type into its API, the port itself must be corrected to use a domain type instead.
- What happens to the markup package (`de.paladinsinn.torg.codex.data.markup.*`) that currently resides in `torg-codex-data`? The data-purity architecture tests introduced by this feature check only for Security and DriveThruRPG references; the markup package is explicitly out of scope and unaffected. Its relocation is governed by feature `002-markup-to-domain`.
- What if a future ADR moves additional integration concerns out of `torg-codex-data`? The architecture test introduced by this feature establishes the pattern; the specific rule can be extended to include additional prohibited package namespaces without structural changes to the test.
- What if Spring Security types appear as transitive compile dependencies of JPA or Liquibase? In that case, ArchUnit would detect the import, but the violation would be in a class that explicitly imports the security type. The POM check (User Story 3) prevents direct dependency; the class-level check (User Story 1) prevents intentional use. Accidental transitive exposure without an explicit import does not constitute a violation.
- What happens to the `DriveThruRpgProductAdapter` adapter's outbound HTTP port `DriveThruRpgProductPort`? This interface is defined in `torg-codex-application` and implemented in `torg-codex-application` (in `de.paladinsinn.drivethru.adapter.out.http`). It should NOT be implemented in `torg-codex-data`. If any implementation of `DriveThruRpgProductPort` remains in `torg-codex-data`, it must be removed or relocated; the architecture tests will surface this.
- What about the `@EnableTorgData` annotation and `TorgDataConfiguration`? The annotation activates `@ComponentScan("de.paladinsinn.torg.codex.data")`, which must not inadvertently wire security or drivethru beans. Since those packages have already been moved out of `torg-codex-data`, no wiring conflict exists currently. The architecture test guards against future reintroduction.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: An automated architecture test (named `DataPersistenceBoundaryArchitectureTest` or equivalent) MUST be added to the `torg-codex` test module's architecture test suite, asserting that no production class in `de.paladinsinn.torg.codex.data.*` imports or depends on any class in `de.paladinsinn.security.*`.
- **FR-002**: The same `DataPersistenceBoundaryArchitectureTest` MUST assert that no production class in `de.paladinsinn.torg.codex.data.*` imports or depends on any class in `de.paladinsinn.drivethru.*`.
- **FR-003**: The two stale tests in `AdapterConventionArchitectureTest` — `driveThruRpgOutboundAdapterLivesUnderDataAdapterOutHttp` and `domainEventBridgeOutboundAdapterLivesUnderDataAdapterOutEvent` — MUST be replaced with updated assertions confirming that `DriveThruRpgProductAdapter` resides at `de.paladinsinn.drivethru.adapter.out.http` and `SpringDomainEventPublisherAdapter` resides at `de.paladinsinn.drivethru.adapter.out.event`, matching the ADR-016-established locations in `torg-codex-application`.
- **FR-004**: The `torg-codex-data/pom.xml` MUST NOT declare `spring-boot-starter-security`, `spring-security-core`, `spring-security-web`, or any other `org.springframework.security.*` artifact as a direct dependency at `compile` or `runtime` scope.
- **FR-005**: The `torg-codex-data/pom.xml` MUST NOT declare any `de.paladinsinn.security.*` or `de.paladinsinn.drivethru.*` artifact as a direct dependency at any scope (these are internal sub-modules, not published artifacts, but the constraint formalizes the boundary).
- **FR-006**: The `TorgDataConfiguration` `@ComponentScan` MUST cover only `de.paladinsinn.torg.codex.data` and no package root belonging to Security or DriveThruRPG integration.
- **FR-007**: The fully qualified class names and public API surfaces of all classes in `de.paladinsinn.security.*` and `de.paladinsinn.drivethru.*` MUST remain unchanged; this feature introduces enforcement only and must not trigger any source-level rename or behavioral change.
- **FR-008**: No Liquibase changeset, JPA entity class, persistence repository interface, or database schema column MUST be added, modified, or removed as part of this feature.
- **FR-009**: No externally visible REST API response (status code, response headers, body field values) for any endpoint MUST differ before and after this feature is implemented.
- **FR-010**: The markup package (`de.paladinsinn.torg.codex.data.markup.*`) is explicitly excluded from the scope of data-purity rules introduced by this feature; the architecture test MUST NOT rule against markup classes remaining in `torg-codex-data` during the lifecycle of this feature.
- **FR-011**: All new and updated architecture tests MUST pass without FreezeList suppression entries for the rules they enforce; violations must be resolved at source, not hidden.
- **FR-012**: The full build (`./mvnw clean verify`) MUST pass after this feature is implemented.

### Key Entities

- **torg-codex-data persistence boundary**: The set of package roots permissible in `torg-codex-data` production code: `de.paladinsinn.torg.codex.data.*` (JPA entities, repositories, persistence adapters, mappers) and `de.kaiserpfalz.liquibase.*` (Liquibase support). All other package namespaces, specifically `de.paladinsinn.security.*`, `de.paladinsinn.drivethru.*`, and `org.springframework.security.*`, are prohibited at the production class level.
- **torg-codex-application integration boundary**: The package roots that own Security and DriveThruRPG integration: `de.paladinsinn.security.*` (Spring Security configuration, API-key filter, user details), `de.paladinsinn.drivethru.*` (DriveThruRPG client, adapters, configuration), and `de.paladinsinn.torg.codex.application.*` (ports, services). These are explicitly authorized in `torg-codex-application` and must not appear in `torg-codex-data`.
- **Architecture test FreezeList**: The mechanism (`FreezeListLoader`, `FreezeListEntry`) that allows pre-existing, acknowledged ArchUnit violations to be suppressed during a migration phase. For rules introduced by this feature, the FreezeList MUST NOT be used; all violations must be resolved at source.
- **Stale architecture test**: The two existing tests in `AdapterConventionArchitectureTest` that assert adapter locations still reflecting the pre-ADR-016 module layout. These must be corrected as part of this feature to restore confidence in the architecture test suite.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The full build (`./mvnw clean verify`) passes with zero failures after implementing this feature, including all architecture tests and integration tests.
- **SC-002**: Exactly zero FreezeList entries cover the new data-purity architecture rules; all rules pass cleanly on the current codebase with no suppression.
- **SC-003**: A deliberate regression test (adding any `de.paladinsinn.security.*` or `de.paladinsinn.drivethru.*` import to a `torg-codex-data` production class) causes the `DataPersistenceBoundaryArchitectureTest` to fail within `./mvnw test -pl torg-codex`, confirming the rule is genuinely enforced.
- **SC-004**: Zero `AdapterConventionArchitectureTest` test failures remain on the current codebase after the stale tests are replaced with accurate assertions for the ADR-016 locations.
- **SC-005**: The `torg-codex-data` module's effective `compile`-scope dependency tree contains zero `org.springframework.security.*` artifacts, verifiable by `./mvnw dependency:tree -pl torg-codex-data`.
- **SC-006**: Zero changes to externally visible REST API behavior, database schema, or Liquibase migrations; characterization test replays for all 17 catalog endpoints remain byte-for-byte identical.

## Assumptions

- The physical code move of `de.paladinsinn.security.*` and `de.paladinsinn.drivethru.*` from `torg-codex-data` to `torg-codex-application` has already been completed as part of the ADR-016 implementation; this feature adds automated enforcement of the resulting boundary, not the code move itself.
- The markup package (`de.paladinsinn.torg.codex.data.markup.*`) is a known tenant in `torg-codex-data` during this feature's lifecycle and is explicitly carved out of the data-purity architecture rules; its relocation is addressed by the separate feature `002-markup-to-domain`.
- The ArchUnit test infrastructure in the `torg-codex` (web application) Maven module is the correct and established host for cross-cutting architecture rules, because it is the only module whose classpath includes compiled classes from all four reactor modules simultaneously.
- The two stale architecture tests (`driveThruRpgOutboundAdapterLivesUnderDataAdapterOutHttp`, `domainEventBridgeOutboundAdapterLivesUnderDataAdapterOutEvent`) currently fail against the ADR-016-aligned codebase because they assert adapter locations that predate the code move; correcting them is in scope for this feature since they guard the very boundary being specified.
- The `torg-codex-data` module's `pom.xml` already has no `spring-security` production dependency; FR-004 formalizes a constraint that is currently satisfied and must remain satisfied going forward.
- No new inter-module Maven dependency needs to be introduced to implement this feature; the `torg-codex` test module already imports all production classes from all four modules and already hosts the ArchUnit test harness.
- Fully qualified class names and public APIs in `de.paladinsinn.security.*` and `de.paladinsinn.drivethru.*` were preserved during the ADR-016 code move; this feature inherits that invariant and does not require any class rename or API renegotiation.
- The FreezeList mechanism is not a valid escape hatch for rules introduced by this feature; all violations detected by the new data-purity rules must be fixed at source.
