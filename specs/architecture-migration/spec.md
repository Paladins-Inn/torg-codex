# Feature Specification: Hexagonal Architecture Migration

**Feature Branch**: `main` (no dedicated feature branch was created — no git branch-automation hook is configured in `.specify/extensions.yml`; work proceeds directly on the current branch)

**Created**: 2026-08-15

**Status**: Draft

**Input**: User description: "Migrate current torg-codex Maven reactor to constitution-compliant Hexagonal Architecture without changing externally visible behavior or persisted data. New Maven modules: torg-codex-domain and torg-codex-application. Domain has no Spring/JPA dependency; Lombok constructors permitted; Logging via Slf4j with lombo; getter and setter via lombok; tostring, hashCode and equals va lmbokk; Bean Validation outside constructors. JPA entities and pure domain models distinct, mapped with MapStruct. Application services expose use cases through ports. REST controllers inbound adapters; persistence/HTTP/Spring event bridges outbound adapters; inward dependency flow. Domain-event port bridges through Spring adapter to ApplicationEventPublisher. Transactions at inbound-adapter boundary. Migration horizontal-by-layer: ports/interfaces, then domain models, then adapters. Temporary violations only by ArchUnit freeze/suppression list, removed at final cleanup. Build after every task; public REST APIs/media types/security/censor/markup/schema/persisted data compatible."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Uninterrupted Service for API Consumers (Priority: P1)

Existing consumers of the public REST APIs (the web frontend, third-party integrations, and any other API clients) continue to receive identical responses, status codes, headers, security/censorship outcomes, and persisted data throughout and after the internal architecture restructuring — even though nothing in the feature is visible to them.

**Why this priority**: Any regression in externally visible behavior or persisted data would break production integrations and violate the constitution's non-negotiable backward-compatibility rule. This guarantee must hold continuously during the migration, not only at its end, so it is the foundation every other story depends on.

**Independent Test**: Can be fully tested by replaying a fixed suite of recorded requests against every existing endpoint before and after each migration phase and confirming equivalent responses (status, headers, body) and unchanged persisted data, independent of whether any restructuring work has been merged yet.

**Acceptance Scenarios**:

1. **Given** an existing REST endpoint and a previously recorded request/response pair, **When** the same request is issued after a migration task completes, **Then** the response status, headers (including the IANA-registered media-type version header), and body match the recorded baseline exactly, excluding fields explicitly known to be time-variant (e.g., timestamps).
2. **Given** a user holding a specific role (`Player`, `GM`, `Third Party Systems`, `Orga`, `Judge`, `Admin`) requesting a DRM-censored resource, **When** the same request is repeated after a migration task completes, **Then** the same fields are censored or omitted as before the change.
3. **Given** the database schema and persisted records before the migration starts, **When** any migration task completes, **Then** no Liquibase changeset has been added, altered, or removed, and every previously persisted record remains retrievable with identical values.

---

### User Story 2 - Safely Evolvable Business Logic (Priority: P2)

A developer extending or modifying a business use case (for example, adding a rule to how catalog entries are queried) can do so entirely within the domain and application layers, without needing to touch or understand REST controllers, JPA entities, or Spring configuration.

**Why this priority**: This is the central value the migration delivers — decoupling business rules from framework and infrastructure concerns — but it only matters once User Story 1's safety net is in place, since flexibility has no value if it breaks production behavior.

**Independent Test**: Can be fully tested by implementing one representative business-rule change using only classes in `torg-codex-domain` and/or `torg-codex-application`, and confirming it is exercised through an existing port without editing any adapter class.

**Acceptance Scenarios**:

1. **Given** a use case exposed through a driving (primary) port, **When** its underlying business rule changes, **Then** the change is implementable and independently testable entirely within `torg-codex-domain` and/or `torg-codex-application`, with no required edits to `torg-codex` or `torg-codex-data` adapter classes.
2. **Given** the `torg-codex-domain` module's source code, **When** its dependencies are inspected, **Then** no class in that module imports any Spring Framework or JPA/Hibernate package.
3. **Given** a domain object that requires validation, **When** the object is validated, **Then** the validation occurs outside the object's constructor (e.g., in an application service, domain factory method, or dedicated validator), never inside a Lombok-generated or hand-written constructor.

---

### User Story 3 - Continuous Automated Architecture Verification (Priority: P3)

A reviewer or team member relies on the automated build to catch any code change that violates the hexagonal module boundaries, without needing a manual architecture review on every pull request.

**Why this priority**: Automated enforcement is what makes the migration's guarantees durable over time. It is most valuable once the target module structure exists (building on Stories 1 and 2), formalizing — rather than replacing — the underlying separation of concerns.

**Independent Test**: Can be fully tested by introducing a deliberate boundary violation (e.g., a Spring import added to a domain class) on a local branch and confirming the build fails with an identified architecture-rule violation, then confirming the build passes again once the violation is removed.

**Acceptance Scenarios**:

1. **Given** a code change that introduces a new, previously unlisted dependency from `torg-codex-domain` on Spring or JPA, **When** the build runs, **Then** it fails, identifying the specific architecture-rule violation.
2. **Given** the migration's temporary architecture-rule exception (freeze/suppression) list still contains entries, **When** the build runs, **Then** each entry names the specific violation and its rationale, and the migration is not reported as complete.
3. **Given** all temporary exceptions have been resolved and removed from the list, **When** the build runs, **Then** it passes with zero suppressed architecture violations remaining.

---

### Edge Cases

- What happens when an existing JPA entity currently doubles as the de-facto domain model and is deeply referenced by controllers and services? It must be split into a distinct JPA entity and a distinct domain model connected by a MapStruct mapper; until the split is finished for a given entity, the temporary coupling is recorded as a named entry in the architecture-rule exception list, not left as an untracked violation.
- How does the system handle a hidden, previously unnoticed dependency from domain code on a Spring or JPA type discovered mid-extraction? The offending task is blocked from merging as a completed step unless the dependency is removed or, if unavoidable in the short term, explicitly added to the exception list with a rationale and later removal.
- What happens if a REST response subtly changes (an added field, a reordered array, a renamed header) while an adapter is being rewired to a new port? The characterization check for that endpoint fails, and the change is corrected before the migration task is considered complete.
- How are in-flight domain events (bridged from the domain event port to Spring's `ApplicationEventPublisher`) handled during the bridge's introduction? The event payload shape and delivery semantics observed by existing listeners must remain unchanged; only the internal publication mechanism moves behind the port.
- What happens if a migration task is too large to keep the repository buildable in one commit? It is split into smaller sub-tasks, each leaving the reactor buildable, using the exception list to bridge any unavoidable interim state between sub-tasks.
- What happens if a migration task accidentally introduces a new, unlisted architecture violation? The build fails immediately at that commit; the task cannot be merged until the violation is fixed or explicitly and separately added to the exception list.
- What happens to Spring `@Transactional` boundaries currently present on services being migrated? They are moved to the inbound-adapter boundary; `torg-codex-application` services and `torg-codex-domain` classes carry no transaction demarcation.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Maven reactor MUST include two new modules, `torg-codex-domain` and `torg-codex-application`, alongside the existing `torg-codex-data` and `torg-codex` modules.
- **FR-002**: The `torg-codex-domain` module MUST NOT declare a compile-time or runtime dependency on any Spring Framework artifact or any JPA/Jakarta Persistence or Hibernate artifact.
- **FR-003**: Domain classes MAY use Lombok-generated constructors (e.g., `@AllArgsConstructor`, `@NoArgsConstructor`, `@RequiredArgsConstructor`, `@Builder`) to reduce boilerplate.
- **FR-004**: Bean Validation annotations MUST NOT be evaluated as part of domain object construction; validation MUST instead be invoked explicitly outside constructors (in application services, domain factory methods, or dedicated validator components) after an object is constructed.
- **FR-005**: Persistence representations (JPA entities) and domain models MUST be implemented as distinct classes; no single class may simultaneously be a JPA `@Entity` and serve as the canonical domain model consumed by application/domain logic.
- **FR-006**: Conversions between JPA entities and domain models, and between domain models and API request/response DTOs, MUST be implemented using MapStruct-generated mappers.
- **FR-007**: The `torg-codex-application` module MUST expose each business use case through a driving (primary) port — an interface implemented by an application service — expressed only in terms of domain model types (no JPA entities, no web DTOs).
- **FR-008**: Outgoing operations required by application services (persistence, outbound HTTP calls, domain event publication) MUST be expressed as driven (secondary) ports — interfaces implemented by outbound adapters, not called directly against concrete infrastructure classes.
- **FR-009**: REST controllers MUST act solely as inbound adapters: they translate HTTP requests into calls on driving ports and MUST NOT contain business/domain decision logic.
- **FR-010**: Persistence implementations, outbound HTTP clients (including the DriveThruRPG client), AMQP publishers/listeners, and the bridge from the domain event port to Spring's `ApplicationEventPublisher` MUST be implemented as outbound adapters.

> **Deviation note (G1, recorded during `/speckit.analyze`)**: As of this migration, the repository has no existing AMQP infrastructure (no broker client, no publisher/listener, no configuration) anywhere in the codebase. This migration is a structural refactor only — it introduces no new business capabilities and therefore must not invent a new AMQP outbound adapter from scratch. **AMQP messaging is explicitly out of scope for this migration.** The "AMQP publishers/listeners" clause of FR-010 is a forward-looking placeholder for whenever AMQP capability is first introduced to the system (at which point it MUST be built as an outbound adapter, consistent with this requirement); it does not require any task in this feature's `tasks.md` to create AMQP code. See T005a in tasks.md for the corresponding traceability task.
- **FR-011**: Compile-time module dependencies MUST point inward only: `torg-codex` and `torg-codex-data` MAY depend on `torg-codex-application` and `torg-codex-domain`; `torg-codex-application` MAY depend on `torg-codex-domain`; `torg-codex-domain` MUST NOT depend on any other reactor module.
- **FR-012**: The domain module MUST define a framework-independent domain-event representation and a `DomainEventPublisher`-style port; a dedicated outbound adapter MUST bridge that port to Spring's `ApplicationEventPublisher`, and existing event payload shape and delivery semantics observed by current listeners MUST remain unchanged.
- **FR-013**: Transaction demarcation (e.g., `@Transactional`) MUST be applied only at the inbound-adapter boundary; `torg-codex-application` services and `torg-codex-domain` classes MUST carry no transaction demarcation.
- **FR-014**: The migration MUST proceed through the following ordered, independently verifiable phases: (1) module scaffolding, (2) port/use-case interface definition, (3) domain model extraction, (4) outbound adapter implementation, (5) inbound adapter rewiring, (6) cleanup and removal of temporary exceptions.
- **FR-015**: After every migration task, the full reactor build and verification (`./mvnw clean verify`) MUST succeed before the next task begins.
- **FR-016**: Public REST API contracts — paths, HTTP methods, status codes, request/response JSON schemas, headers, and IANA media-type versioning — MUST remain unchanged as observed by API consumers, unless a change is explicitly and separately documented as an accepted exception.
- **FR-017**: RBAC/ABAC/UMA authorization outcomes and Censor-filtered (DRM) field behavior MUST remain unchanged for identical requests and roles, verified before and after every migration phase.
- **FR-018**: No Liquibase changeset MUST be added, modified, or removed as part of this migration; the persisted database schema MUST remain identical.
- **FR-019**: Existing persisted data MUST remain readable and writable with equivalent results (same records, ordering, and null-handling) through the migrated persistence adapters.
- **FR-020**: Temporary deviations from the target module boundaries MUST be recorded in an automated, version-controlled architecture-rule exception (freeze/suppression) list rather than left as silent, undetected violations.
- **FR-021**: Every entry in the exception list MUST identify the specific violation and the reason it is temporary, and MUST be removed during the final cleanup phase; the migration is not considered complete while any entry remains.
- **FR-022**: Automated architecture tests MUST verify, at minimum: inward-only dependency direction between the four modules, absence of forbidden Spring/JPA dependencies in `torg-codex-domain`, and adherence to inbound/outbound adapter package conventions.
- **FR-023**: Architecture tests MUST run as part of the standard build so that any violation not present in the exception list fails `./mvnw clean verify`; architecture tests MUST NOT be disabled or skipped to make a migration task pass.
- **FR-024**: A newly introduced architecture violation that is not already present in the exception list MUST fail the build; only pre-existing, explicitly listed violations may pass.
- **FR-025**: Every REST endpoint currently exposed by the `torg-codex` module MUST be covered by characterization checks (e.g., recorded request/response fixtures) comparing behavior before and after each migration phase that touches that endpoint's call path.
- **FR-026**: If a migration task is found to alter externally visible behavior or persisted data unintentionally, the task MUST be blocked from completion or reverted, unless the change is separately and explicitly documented as an accepted, rationale-backed exception.
- **FR-027**: Architecture documentation (`docs/modules/arc42`) MUST be updated to reflect the finalized module structure and boundaries once the migration reaches its cleanup phase.

### Key Entities

- **Bounded Module**: One of the four Maven reactor modules (`torg-codex-domain`, `torg-codex-application`, `torg-codex-data`, `torg-codex`); has a name, an allowed set of inward dependencies, and a set of classes that belong to it.
- **Domain Model**: A framework-independent representation of a business concept (e.g., an item, spell, race, or publication) used by application services and adapters; distinct from its corresponding JPA entity.
- **Persistence Entity**: The JPA/Hibernate-mapped representation of a business concept, owned by `torg-codex-data`, mapped to/from its corresponding Domain Model via a MapStruct mapper.
- **Driving Port**: An interface exposed by `torg-codex-application` describing an inbound use case, implemented by an application service and invoked by inbound adapters.
- **Driven Port**: An interface describing an outbound capability needed by an application service (persistence, external HTTP call, domain-event publication), implemented by an outbound adapter.
- **Adapter**: A class translating between an external concern (HTTP request, database row, AMQP message, Spring event) and a port; classified as inbound (driving a use case) or outbound (implementing a driven port).
- **Domain Event**: A framework-independent notification of a business occurrence, published through the domain event port and bridged to Spring's `ApplicationEventPublisher` by an outbound adapter.
- **Architecture Exception List Entry**: A recorded, temporary, named deviation from target module boundaries, including the violation description, its rationale, and its removal status.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of a fixed regression suite of requests against currently published REST endpoints return identical status codes, response bodies, and headers before and after the migration.
- **SC-002**: Zero unplanned changes to persisted data content or database schema are introduced by the migration, confirmed by comparing schema and data snapshots taken before the migration starts and after it completes.
- **SC-003**: The full build, including architecture-conformance checks, succeeds after every migration task, achieving uninterrupted green-build continuity for the whole migration.
- **SC-004**: By migration completion, zero temporary architecture-rule exceptions remain in the exception list.
- **SC-005**: 100% of existing business use cases have their core decision logic locatable within `torg-codex-domain` and/or `torg-codex-application`, verified without needing to inspect Spring- or JPA-specific code.
- **SC-006**: Authorization and content-censorship outcomes for a fixed set of representative requests across all defined roles show zero regressions when compared before and after the migration.

## Assumptions

- Existing automated unit and integration tests can be adapted to the new module/package boundaries without changing their functional assertions; test relocation and rewiring are considered part of each migration phase's task, not a separate feature.
- ArchUnit (or an equivalent JVM architecture-testing library already compatible with the project's build) is the automated enforcement mechanism referenced by the constitution's "automated architecture tests" requirement.
- Partial completion (some architecture-exception-list entries still open) is an acceptable interim state across intermediate releases, provided every entry is documented with its rationale and the list is not silently expanded with new, unrelated violations; the migration is not considered functionally complete until the list is empty.
- `torg-codex-data` continues to own Liquibase migrations and remains the module hosting outbound persistence adapters after the migration; no additional infrastructure module is introduced by this effort.
- No new public API endpoints, DTOs, or business capabilities are introduced by this migration; its scope is limited to internal restructuring of existing behavior.
- The horizontal-by-layer approach (ports/interfaces, then domain models, then adapters) is applied per existing business area (e.g., Article, Cosm, Item, Miracle, Perk, Power, Publication, Race, Shard, Spell, Tag, Threat, Vehicle, and their list/group variants) rather than migrating all areas simultaneously, allowing the reactor to remain buildable between areas.
