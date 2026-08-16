# Feature Specification: Relocate Markup Service to Domain

**Feature Branch**: `002-markup-to-domain`

**Created**: 2026-08-16

**Status**: Draft

**Input**: User description: "Move the markup implementation from torg-codex-data into torg-codex-domain because it is a domain service. Preserve public API behavior, censoring/rendering semantics, backward compatibility, persistence schema/data, and module dependency direction. Address that the current markup implementation uses Spring annotations and CommonMark, so the spec must define framework-independent domain ownership and any adapter/provider boundary needed without prescribing implementation unnecessarily."

## User Scenarios & Testing *(mandatory)*

<!--
  IMPORTANT: User stories are prioritized as user journeys ordered by importance.
  Each story is independently testable and delivers standalone value.
-->

### User Story 1 - Markup as a testable, framework-free domain service (Priority: P1)

A contributor who wants to work on or test markup processing logic — conditional product blocks, entity references, game tokens, and Markdown rendering — can do so entirely within `torg-codex-domain`, constructing the markup service and all its processors as plain Java objects in tests, without requiring a Spring application context, a database connection, or any framework infrastructure.

**Why this priority**: This is the primary objective. Today markup classes reside in the data module among JPA/Spring infrastructure, and carry `@Component`/`@Service` annotations that conflate pure domain logic with framework wiring concerns. Relocating them to the framework-free domain module gives the domain boundary meaning for this capability and removes the false coupling to persistence infrastructure.

**Independent Test**: Can be fully tested by instantiating `TorgMarkupService` and its processors as plain Java objects in a `torg-codex-domain` test — no Spring context, no mocks of Spring types — calling `render()` with raw markup samples for each processing stage, and asserting the expected HTML output. This is already the shape of the existing processor unit tests.

**Acceptance Scenarios**:

1. **Given** a unit test in `torg-codex-domain` with no Spring application context, **When** `TorgMarkupService` and its processors are instantiated directly and `render()` is called with raw text containing conditional blocks, entity references, game tokens, and Markdown, **Then** the output matches the expected rendered HTML, proving the pipeline works without any framework.
2. **Given** the `torg-codex-domain` module's production dependency list, **When** it is inspected, **Then** no Spring Framework artifact appears (no `spring-context`, `spring-beans`, `spring-core`, or any other `org.springframework.*` artifact) as a production dependency.
3. **Given** any class in the relocated markup package within `torg-codex-domain`, **When** its source imports are inspected, **Then** no `org.springframework.*` or `jakarta.inject.*` import is present.

---

### User Story 2 - Zero externally visible change for API consumers (Priority: P1)

Existing consumers of all 17 catalog REST endpoints continue to receive exactly the same rendered content after the relocation as before — both for anonymous requests and for authenticated requests carrying `ROLE_<codex-id>` product ownership authorities.

**Why this priority**: Shared priority with User Story 1. The relocation is an architectural cleanup, not a behavioral change. Any regression in rendered output would break the application silently and violate the constitution's non-negotiable backward-compatibility rule.

**Independent Test**: Can be fully tested by replaying a fixed set of captured request/response pairs against every catalog endpoint before and after the relocation and asserting byte-for-byte equivalence of the response body for every markup-rendered field (e.g., `worldLaws`, `description`, `text` fields across catalog entities).

**Acceptance Scenarios**:

1. **Given** a recorded API response for any catalog endpoint before the relocation, **When** the same request is made after the relocation, **Then** the response body is byte-for-byte identical for all markup-rendered fields.
2. **Given** an authenticated request carrying `ROLE_sourcebook-aysle`, **When** the same Aysle cosm detail request is made before and after the relocation, **Then** the rendered `worldLaws` field is identical in both responses.
3. **Given** the markup rendering order defined in the constitution (conditional product blocks → entity references → raw HTML → game tokens → CommonMark), **When** the relocated service processes any catalog content, **Then** the same pipeline order and output are observed as before the relocation.

---

### User Story 3 - Automated architecture enforcement of the new boundary (Priority: P2)

A future contributor who inadvertently adds a Spring import to a domain markup class sees a build failure immediately — before code review — rather than discovering the violation through a manual audit.

**Why this priority**: Without automated enforcement, the domain module's framework-free constraint becomes a convention that can silently erode. Automated architecture tests (already present for the hexagonal boundary) provide a durable safety net.

**Independent Test**: Can be fully tested by introducing a deliberate Spring annotation to a domain markup class on a local branch, confirming the architecture test fails, then removing it and confirming the build passes again.

**Acceptance Scenarios**:

1. **Given** an architecture test enforcing no Spring imports in `torg-codex-domain`, **When** any class in the relocated markup package is given a `@Component` annotation or any Spring import, **Then** the build fails with a detected architecture-rule violation naming the offending class.
2. **Given** all markup classes moved to domain with Spring annotations removed, **When** the full build (`./mvnw clean verify`) runs, **Then** all architecture tests pass with zero violations in the domain markup package.
3. **Given** the consuming modules (`torg-codex-data`, `torg-codex-application`, `torg-codex`) referencing markup classes after relocation, **When** the build runs, **Then** all references resolve correctly against the new package path in `torg-codex-domain`.

---

### Edge Cases

- What happens when the `TorgMarkupService` or any processor is requested via Spring dependency injection after Spring annotations are removed from the domain classes? A framework-binding configuration provided in `torg-codex-application` MUST recreate equivalent Spring bean definitions so that injection-based callers continue to function without source-level changes to those callers beyond import path updates.
- What happens to the `commonmark` library dependency when `MarkdownProcessor` moves to `torg-codex-domain`? The library travels with the class: it becomes a `torg-codex-domain` production dependency and is removed from `torg-codex-data`. Because `commonmark` is a pure Java library with no Spring or JPA coupling, this does not violate the domain's framework-free constraint.
- What happens if any `torg-codex-data` class (e.g., a JPA entity helper or a data-layer mapper) currently uses `Censor` or `TorgMarkupService` by their old package path? Only the import path changes from `de.paladinsinn.torg.codex.data.markup` to `de.paladinsinn.torg.codex.domain.markup`; no behavioral change is required. This is safe because `torg-codex-data` already depends on `torg-codex-domain`.
- What happens to the constitution and ADRs after this move? The constitution v2.0.0 assigns markup to `torg-codex-application`; this feature is a correction to that assignment. A new ADR and a constitution amendment are required as formal deliverables of this feature.
- What is the relationship between this feature and the `commonmark` vs. `flexmark-java` discrepancy (ADR-011 specifies `flexmark-java`; current code uses `commonmark`)? This pre-existing discrepancy is out of scope for this feature. The markup library is not changed by the relocation. The ADR amendment required by this feature covers module ownership and adapter pattern only; aligning the library implementation with ADR-011 is deferred to a separate future task.
- What happens if OWASP HTML Sanitizer is currently absent from the pipeline despite being specified in ADR-011 and the constitution? This pre-existing gap is out of scope for this feature. Its absence is neither introduced nor remedied by the relocation.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The complete markup processing package — `TorgMarkupService`, `Censor`, `ConditionalBlockProcessor`, `EntityReferenceProcessor`, `RawHtmlProcessor`, `GameTokenProcessor`, `GameTokenRegistry`, `EntityType`, and `MarkdownProcessor` — MUST be relocated from `torg-codex-data` to `torg-codex-domain`.
- **FR-002**: No class in the relocated markup package MUST carry any Spring Framework or Jakarta CDI stereotype annotation (`@Component`, `@Service`, `@Bean`, `@Named`, or equivalent) or import any `org.springframework.*` package in production source within `torg-codex-domain`.
- **FR-003**: A framework-binding configuration MUST be provided in `torg-codex-application` that exposes the domain markup classes as Spring-managed beans for the application runtime, enabling existing Spring-injection-based callers to continue functioning without modification to those callers' source code (import path updates excepted).
- **FR-004**: The public API surface of `TorgMarkupService` (method `render(String rawText, Set<String> ownedProducts)`) and `Censor` (static factories `of(TorgMarkupService, Set<String>)` and `unauthenticated(TorgMarkupService)`, and instance method `apply(String)`) MUST remain behaviorally identical before and after relocation.
- **FR-005**: The markup rendering pipeline order MUST be preserved exactly: (1) conditional product block resolution (`<IF:id>`, `<IF:!id>`) → (2) entity reference expansion → (3) raw HTML passthrough → (4) game token substitution → (5) CommonMark rendering.
- **FR-006**: No Liquibase changeset, JPA entity class, persistence repository interface, or database schema column MUST be added, modified, or removed as part of this feature.
- **FR-007**: No externally visible REST API response (status code, response headers, body field values) for any of the 17 catalog endpoints MUST differ before and after the relocation.
- **FR-008**: All existing unit tests for the markup classes MUST continue to pass after relocation. Tests MUST reside in `torg-codex-domain` and MUST NOT require a Spring application context to run.
- **FR-009**: The `commonmark` library dependency MUST be transferred from `torg-codex-data` to `torg-codex-domain` as a production dependency; `torg-codex-data`'s production dependency on `commonmark` MUST be removed.
- **FR-010**: All import references from `de.paladinsinn.torg.codex.data.markup.*` to `de.paladinsinn.torg.codex.domain.markup.*` MUST be updated across all consuming modules (`torg-codex-data`, `torg-codex-application`, `torg-codex`); no re-export or compatibility stub in the old package path is required.
- **FR-011**: Automated architecture tests MUST enforce that no class in `torg-codex-domain`'s markup package imports Spring Framework types, in line with the existing domain module boundary rules.
- **FR-012**: A new Architecture Decision Record MUST be authored, accepted, and committed alongside this feature that: (a) establishes markup processing as a domain service owned by `torg-codex-domain`, (b) supersedes the markup ownership assignment in constitution v2.0.0 Principle I and Principle VI (which assigned markup to `torg-codex-application`), and (c) specifies the required framework-binding adapter pattern for Spring wiring of domain markup classes.
- **FR-013**: The TORG-CODEX Constitution MUST be updated to reflect the corrected module ownership: the markup processing package belongs in `torg-codex-domain`; the framework-binding Spring configuration adapter for that package belongs in `torg-codex-application`.
- **FR-014**: After the relocation, `torg-codex-data` MUST contain no production class in a `markup` subpackage.

### Key Entities

- **Markup pipeline**: The ordered sequence of five processors (`ConditionalBlockProcessor`, `EntityReferenceProcessor`, `RawHtmlProcessor`, `GameTokenProcessor`, `MarkdownProcessor`) coordinated by `TorgMarkupService`, transforming raw markup-bearing text fields into rendered HTML.
- **Censor**: A value object pairing a resolved set of owned product identifiers with the markup pipeline, providing the `apply(String)` entry point used by catalog mappers to render each text field with correct product-gate filtering. Currently Spring-annotation-free; this status is preserved after relocation.
- **Framework-binding adapter**: A configuration artifact outside `torg-codex-domain` (in `torg-codex-application`) that creates and registers the domain markup service and its processors as Spring-managed beans, so that framework-dependent consumers can use them without domain classes having any knowledge of or dependency on the application framework.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of relocated markup classes in `torg-codex-domain` have zero Spring Framework or Jakarta CDI imports in production source, verified by automated architecture tests during `./mvnw clean verify`.
- **SC-002**: 100% of existing markup unit tests pass after relocation, executing within the `torg-codex-domain` test scope with no Spring application context, verified by `./mvnw test` on the domain module.
- **SC-003**: 100% of catalog REST API responses are byte-for-byte identical before and after the relocation for all markup-rendered fields, verified by the characterization test suite during `./mvnw clean verify`.
- **SC-004**: The full build (`./mvnw clean verify`) passes with zero architecture violations and zero test failures after the relocation.
- **SC-005**: Exactly one new ADR is committed and accepted, recording the markup-to-domain ownership decision and its adapter pattern, and the constitution is updated to the appropriate next version reflecting the corrected module ownership.
- **SC-006**: The `torg-codex-data` module contains no production class in any `markup` subpackage after the relocation, verifiable by inspecting the module's compiled output or source tree.

## Assumptions

- The `commonmark` library used by `MarkdownProcessor` has no Spring or JPA runtime coupling and is therefore a permissible production dependency for the framework-free `torg-codex-domain` module; no library change is required as part of this feature.
- The pre-existing discrepancy between ADR-011 (which specifies `flexmark-java`) and the current `MarkdownProcessor` implementation (which uses `commonmark`) is acknowledged but out of scope for this feature; aligning the library with ADR-011 is deferred to a separate task.
- All current callers of `Censor` and `TorgMarkupService` in `torg-codex-data` and `torg-codex` receive these via Spring dependency injection; the framework-binding configuration required by FR-003 will provide equivalent bean definitions without requiring source-level changes to those callers beyond updating their import paths.
- `torg-codex-data` already declares `torg-codex-domain` as a dependency; the package rename (FR-010) therefore introduces no new cross-module dependency for `torg-codex-data`.
- `torg-codex-application` already has Spring Framework dependencies and already depends on `torg-codex-domain`; hosting the framework-binding adapter (FR-003) there introduces no new inter-module dependency.
- Moving markup to `torg-codex-domain` is a correction of the constitution v2.0.0 module ownership assignment for markup; the required ADR (FR-012) formalizes this correction rather than creating an exception or deviation.
- The absence of an OWASP HTML Sanitizer step in the active pipeline (despite being specified in ADR-011 and the constitution) predates this feature and is not introduced or remedied by the relocation; it is tracked separately.
- The target package for relocated classes is `de.paladinsinn.torg.codex.domain.markup`; no intermediate compatibility layer or re-export from the old `de.paladinsinn.torg.codex.data.markup` path is required.
