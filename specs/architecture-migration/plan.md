# Implementation Plan: Hexagonal Architecture Migration

**Branch**: `architecture-migration` | **Date**: 2026-08-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/architecture-migration/spec.md`

## Summary

Restructure the Maven reactor into `torg-codex-domain`, `torg-codex-application`,
`torg-codex-data`, and `torg-codex` using ports-and-adapters while preserving every
observable REST, security/censorship, event, persistence, and database-schema behavior.
The migration is incremental and horizontal by layer for each catalog area: scaffold and
enforce boundaries; introduce domain-only ports and models; map at adapter edges with
MapStruct; rewire adapters; then remove all tracked temporary exceptions. No migration
code or Liquibase changes are part of this planning feature.

## Technical Context

**Language/Version**: Java 25 (`java.version` in root POM).

**Primary Dependencies**: Maven reactor; Spring Boot 4.0.6 (web, security, data JPA,
validation, cache); Spring Modulith; Spring Data JPA/Hibernate; MapStruct 1.6.3; Lombok;
Liquibase; PostgreSQL; Testcontainers; JUnit 5, AssertJ, Mockito; Spring Cloud Contract.
ArchUnit is added as the architecture-test dependency during implementation.

**Storage**: PostgreSQL through JPA entities and Spring Data repositories in
`torg-codex-data`; Liquibase resources under
`torg-codex-data/src/main/resources/db`. The schema and all applied changesets are
immutable for this migration.

**Testing**: JUnit 5 unit tests, Mockito and AssertJ; Testcontainers PostgreSQL
integration tests under Failsafe; WireMock for outbound HTTP; recorded REST
characterization fixtures plus OpenAPI/contract comparison; ArchUnit architecture tests
executed by `./mvnw clean verify`.

**Target Platform**: Linux-hosted Spring Boot web application and PostgreSQL, built with
the Maven Wrapper.

**Project Type**: Multi-module JVM web service with REST API and a reusable data module.

**Performance Goals**: No performance regression is accepted. Existing endpoint
latency, ordering, pagination/collection behavior, and downstream event delivery
semantics remain observationally equivalent; characterization tests are the release
gate because no numerical SLO is presently specified.

**Constraints**:
- Preserve paths, methods, request/response JSON, status codes, headers, IANA
  media-type version negotiation, RBAC/ABAC/UMA outcomes, DRM censorship, markup
  rendering, and externally observed Spring-event payload/delivery semantics.
- Preserve database table/column mapping, data, query ordering, and null behavior; do
  not add, modify, or remove Liquibase changesets.
- `torg-codex-domain` has no imports or dependencies from `org.springframework..`,
  `jakarta.persistence..`, `org.hibernate..`, or another reactor module. Lombok is
  permitted. Validation is invoked after construction, never by a constructor.
- `torg-codex-application` depends only on the domain module and Java/Lombok as needed;
  it has neither JPA/Spring infrastructure nor `@Transactional`.
- MapStruct owns domain-to-JPA and domain-to-DTO conversion. Controllers are thin
  inbound adapters; JPA, HTTP, AMQP, and Spring-event bridge implementations are
  outbound adapters.
- Transaction demarcation is on the Spring inbound-adapter/composition boundary only.
- Run `./mvnw clean verify` after every implementation task. Architecture tests cannot
  be disabled to obtain a passing build.

**Scale/Scope**: Four reactor modules; 17 current catalog resource families
(`Article`, `Cosm`, `Item`, `Miracle`, `MiracleList`, `Perk`, `PerkGroup`, `Power`,
`PowerList`, `Publication`, `Race`, `Shard`, `Spell`, `SpellList`, `Tag`, `Threat`,
`Vehicle`), their DTOs/mappers/repositories, DriveThruRPG integration, and existing
event/messaging seams. The public REST compatibility surface is enumerated in
[`contracts/rest-compatibility.md`](contracts/rest-compatibility.md).

## Constitution Check

### Pre-research gates

| Gate | Result | Plan treatment |
|---|---|---|
| Hexagonal boundaries and inward dependencies | PASS | Four modules and allowed dependency graph are explicit; domain/application contain no framework infrastructure. |
| Separate domain and persistence representations | PASS | Each JPA entity remains in data and maps to a distinct domain model through MapStruct. |
| REST/OpenAPI compatibility | PASS | Existing paths and representations are frozen by the REST compatibility contract and characterization fixtures; no endpoint is added or changed. |
| Security, DRM censorship, and markup safety | PASS | Authorization and censoring stay at inbound/composition boundaries and are replayed for all defined roles; rendering order is not moved into domain models. |
| Database evolution | PASS | This is explicitly a no-schema-change migration: no Liquibase artifact changes and existing JPA tables/columns are retained. |
| Event and integration architecture | PASS | Framework-independent event port is bridged by a Spring outbound adapter; existing listener-facing payload and delivery semantics are preserved. |
| Test-first and buildability | PASS | Unit, integration, WireMock, characterization, and ArchUnit gates are required at every task. |
| Documentation/ADR governance | PASS | ADR-005 supplies the approved hexagonal direction; finalized boundaries are synchronized to `docs/modules/arc42` during cleanup as required by FR-027. |

No constitution violation requires a complexity justification.

### Post-design re-check

**PASS.** `research.md`, `data-model.md`, the REST compatibility contract, and
`quickstart.md` preserve the pre-research gate decisions. There are no unresolved
technical clarifications and no accepted compatibility exceptions. Any future exception
must be separately justified, version-controlled, and remain a failing completion gate.

## Project Structure

### Documentation (this feature)

```text
specs/architecture-migration/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── rest-compatibility.md
└── checklists/
    └── architecture.md
```

### Source Code (repository root)

```text
pom.xml                                  # reactor parent; declares all four modules
torg-codex-domain/
└── src/main/java/de/paladinsinn/torg/codex/domain/
    ├── model/                           # framework-independent domain models/value objects
    └── event/                           # DomainEvent representation and publisher port
torg-codex-application/
└── src/main/java/de/paladinsinn/torg/codex/application/
    ├── port/in/                         # driving use-case interfaces
    ├── port/out/                        # driven persistence/HTTP/event interfaces
    └── service/                         # framework-free use-case implementations
torg-codex-data/
└── src/
    ├── main/java/de/paladinsinn/torg/codex/data/
    │   ├── adapter/out/                 # JPA, DriveThruRPG, AMQP, Spring-event adapters
    │   ├── mapper/                      # JPA entity <-> domain MapStruct mappers
    │   ├── model/                       # JPA entities only
    │   └── repository/                  # Spring Data repositories
    └── main/resources/db/               # unchanged Liquibase definitions
torg-codex/
└── src/main/java/de/paladinsinn/torg/codex/
    ├── api/
    │   ├── controller/                  # REST inbound adapters
    │   ├── dto/                         # unchanged public wire DTOs
    │   ├── mapper/                      # domain <-> DTO MapStruct mappers
    │   └── security/                    # HTTP/security/censor adapters
    └── configuration/                   # composition and transaction boundary
docs/modules/arc42/                      # finalized architecture documentation
```

**Structure Decision**: Use the target four-module Maven reactor. Domain and
application form the framework-independent core; existing `torg-codex-data` and
`torg-codex` become outer adapters and composition roots. The current transitional
`data.application` code is not a target boundary and is moved through the ordered
migration rather than normalized as a permanent design.

## Migration Design and Exit Criteria

1. **Module scaffolding and enforcement**: add the two modules, dependency management,
   baseline ArchUnit rules, and a version-controlled exception list. Exit only when the
   reactor builds, the list names every baseline exception with rationale/removal owner,
   and any new unlisted violation fails.
2. **Ports/use-case interfaces**: define domain-only driving and driven ports for a
   catalog area, including `DomainEventPublisher`. Exit only when interfaces expose no
   DTO/JPA/Spring type and framework-free application services are independently tested.
3. **Domain extraction**: introduce distinct domain models/value objects and explicit
   post-construction validation. Exit only when architecture tests reject prohibited
   domain imports and characterization baselines remain unchanged.
4. **Outbound adapters**: implement MapStruct JPA mappers and adapters for repositories,
   DriveThruRPG, AMQP, and Spring event publication. Exit only when persistence
   read/write equivalence, external HTTP stubs, and event observations pass.
5. **Inbound rewiring**: make controllers/security boundaries invoke driving ports and
   map domain models to existing DTOs; place transactions only at the adapter boundary.
   Exit only when all affected REST/security/censorship fixtures pass.
6. **Cleanup**: remove legacy coupling and every exception-list entry, complete
   architecture documentation, and run the full reactor verification. Exit only with
   zero suppressions, no schema change, and all compatibility tests green.

Each catalog area is migrated in that sequence and is split further whenever a step
cannot remain buildable. No phase can trade off a compatibility or schema invariant.

## Complexity Tracking

No constitution violations or justified complexity exceptions.
