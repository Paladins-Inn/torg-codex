# Research: Hexagonal Architecture Migration

## Decision 1: Target module graph

**Decision**: Create `torg-codex-domain` and `torg-codex-application` beside the
existing `torg-codex-data` and `torg-codex` modules. Permit only
`torg-codex`/`torg-codex-data` → `torg-codex-application` → `torg-codex-domain`
dependencies, plus direct outer-module → domain dependencies where an adapter needs a
domain type. The domain module has no dependency on any other reactor module.

**Rationale**: The root reactor currently declares only `torg-codex` and
`torg-codex-data`, while the constitution mandates inward-only dependencies and a
framework-free core. This graph makes the boundary mechanically testable.

**Alternatives considered**:
- Keep a logical domain package inside `torg-codex-data`: rejected because that module
  already imports Spring/JPA and cannot prove the no-framework rule.
- Add a separate infrastructure module: rejected by the feature assumption that
  `torg-codex-data` remains the infrastructure owner.

## Decision 2: Domain, application, and adapter ownership

**Decision**: Place business concepts and framework-independent value objects/events in
domain; place use-case implementations and in/out port interfaces in application; place
JPA entities/repositories, DriveThruRPG/AMQP clients, and the Spring event bridge in
data; place REST DTO mapping, controllers, HTTP security, censorship integration, and
Spring composition/transaction boundaries in the web module.

**Rationale**: Repository inspection shows current catalog entities under
`data.model`, repositories under `data.repository`, controllers under `api.controller`,
and a transitional `data.application` implementation. The target assigns each concern
to the constitutional layer without changing its external behavior.

**Alternatives considered**:
- Let controllers access repositories directly: rejected because it leaves adapters
  coupled to persistence and violates the required driving-port boundary.
- Put ports in domain: rejected because use cases belong to application and many ports
  express application coordination rather than domain behavior.

## Decision 3: Mapping and persistence identity

**Decision**: Retain existing JPA entities, table mappings, repository queries, UUIDs,
ordering, and null semantics in `torg-codex-data`; create a separate domain counterpart
for each catalog model; map both directions using MapStruct mappers located in the data
adapter. DTO mapping is MapStruct in the web adapter.

**Rationale**: Existing models (`Article`, `Cosm`, `Item`, `Miracle`, `MiracleList`,
`Perk`, `PerkGroup`, `Power`, `PowerList`, `Publication`, `Race`, `Shard`, `Spell`,
`SpellList`, `Tag`, `Threat`, and `Vehicle`) are annotated JPA types and repositories
are typed to those classes. Keeping them as persistence representations preserves the
schema while meeting the distinct-model requirement.

**Alternatives considered**:
- Annotate domain models as `@Entity`: rejected by FR-005 and the no-JPA domain rule.
- Manually map entities/DTOs: rejected by the constitution and FR-006, which require
  MapStruct.

## Decision 4: Validation and transaction boundaries

**Decision**: Domain constructors/builders only assign state. Application services,
domain factories, or dedicated validators explicitly validate constructed domain
objects. `@Transactional` is applied only to a Spring inbound adapter or composition
boundary; application services and domain classes carry none.

**Rationale**: This directly resolves the constitution and FR-004/FR-013. It also
keeps framework annotations and transaction management outside the reusable core.

**Alternatives considered**:
- Bean validation in constructors: rejected because construction must not evaluate
  validation.
- Transactional application services: rejected by the fixed boundary decision.

## Decision 5: Events and external integrations

**Decision**: Define a framework-independent `DomainEvent` representation and
`DomainEventPublisher` driven port. Implement that port in data using a dedicated Spring
adapter that delegates to `ApplicationEventPublisher`; expose DriveThruRPG HTTP and AMQP
capabilities as driven ports with data-side adapters.

**Rationale**: The constitution requires Spring in-process events while FR-012 requires
the framework dependency to be behind a port. The adapter preserves the payload shape
and delivery behavior observed by existing listeners.

**Alternatives considered**:
- Publish Spring events directly from domain/application: rejected because it imports
  Spring into the core.
- Replace existing event delivery with a new broker protocol: rejected because the
  feature preserves observed delivery semantics and adds no externally visible feature.

## Decision 6: Architecture enforcement and temporary state

**Decision**: Use ArchUnit tests in the standard Maven test lifecycle and a
version-controlled, named freeze/suppression list. Each item records the exact
dependency/placement violation, rationale, introduction/baseline, and planned removal
phase. The list is empty at completion; a new violation absent from it fails the build.

**Rationale**: This is the explicit FR-020–FR-024 mechanism and makes incremental,
buildable migration possible without silently accepting architectural erosion.

**Alternatives considered**:
- Disable rules until the end: rejected because it cannot detect new violations.
- Keep a broad package-level suppression: rejected because it cannot distinguish
  existing debt from newly introduced debt.

## Decision 7: REST, censorship, and database compatibility verification

**Decision**: Freeze the current API surface in
[`contracts/rest-compatibility.md`](contracts/rest-compatibility.md), capture
request/response fixtures for every listed endpoint and defined role, and compare status,
headers, body, ordering, and censored fields after every affected task. Use
Testcontainers for persisted-data read/write equivalence. Make no Liquibase change.

**Rationale**: Current controllers expose 17 GET-only `/api` resource families, with
optional `cosm` filtering on the applicable catalog families. Current models use
`TorgEntity` censor injection, and the current composition root applies
`CensoringCatalogQuery`; therefore security/censorship is part of the observable
contract, not an internal implementation detail.

**Alternatives considered**:
- Compare only OpenAPI documents: rejected because it misses authorization, censorship,
  header, ordering, and runtime rendering behavior.
- Perform a database migration to make domain types persistable: rejected because the
  migration forbids schema or Liquibase changes.

## Resolved Clarifications

| Clarification | Resolution |
|---|---|
| Exact forbidden domain dependencies | `org.springframework..`, `jakarta.persistence..`, `org.hibernate..`, and every other reactor module; Lombok remains allowed. |
| JPA-to-domain mapper ownership | `torg-codex-data` outbound persistence adapters, implemented with MapStruct. |
| DTO mapper ownership | `torg-codex` REST inbound adapter, implemented with MapStruct. |
| DriveThruRPG, AMQP, and Spring events | Driven-port implementations in `torg-codex-data`; only their port types reach the core. |
| REST scope | All currently exposed `/api` catalog paths in the compatibility contract; `/login` browser handlers remain behaviorally untouched but are not catalog REST resources. |
| Completion criterion for temporary exceptions | The list has zero entries; otherwise migration remains incomplete. |
