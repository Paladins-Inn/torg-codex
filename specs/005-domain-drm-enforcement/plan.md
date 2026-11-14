# Implementation Plan: Enforce Product-Ownership DRM at Domain Level

**Branch**: `005-domain-drm-enforcement` | **Date**: 2026-11-14 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/005-domain-drm-enforcement/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command; its definition describes the execution workflow.

## Summary

Anonymous callers can list and read the whole proprietary catalog. The only DRM today is the
in-text `<IF:…>` censoring of detail responses, and the catalog use cases (`CatalogQueryService`)
pass every entry through unfiltered. This applies to all 14 product-bound areas.

The fix moves the access decision into the core:

- **Access rule**: a pure domain policy (`CatalogAccessPolicy` + `OwnedProducts`). It always
  includes the free tier `core-rulebook`, and entries without products are denied (fail-closed).
- **Enforcement**: a new framework-free use case (`RestrictedCatalogQuery` /
  `RestrictedCatalogQueryService`) is the only way to read the 14 product-bound areas.
- **What callers get**:
  - lists contain only accessible entries;
  - single non-accessible entries return a purchase teaser (`403`);
  - product-less entries look like missing ones (`404`);
  - embedded entry lists omit inaccessible references.
- **Observability**: product-less entries are reported through a driven port. A single adapter
  logs a `WARN` and increments a Prometheus counter for each encounter. Spring Boot native
  structured logging emits ECS JSON; no Logstash-specific encoder is used (ADR-017).
- **Ownership**: still resolved only by the existing `ProductOwnershipResolver`, which now also
  implements the new `CallerProductsPort`.
- **Data fix**: a Liquibase data migration fixes the catalog gaps found during planning. The 10
  core cosms get `core-rulebook`, and perk groups get the products of their perks.

Details: [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/).

## Technical Context

**Language/Version**: Java 25 (Maven wrapper, 4-module reactor)

**Primary Dependencies**: Spring Boot (Web MVC, Security, Data JPA, Actuator), Micrometer +
Prometheus registry (already present in `torg-codex`), MapStruct, Lombok, Liquibase, ArchUnit,
JUnit 5 / AssertJ / Mockito, Testcontainers PostgreSQL. **No new dependency.**

**Storage**: PostgreSQL via Liquibase. There is no schema change. There is one data-only changeset
(`2026-11-14-001-migrate.yaml`, ADR-011 "migrate" phase), which is idempotent and a no-op on the
free-tier dataset.

**Testing**:
- `./mvnw test`: domain and application unit tests, adapter tests, ArchUnit, and the
  Testcontainers-backed `@SpringBootTest` tests in `torg-codex` (Surefire, module convention).
- `./mvnw clean verify`: the full build, including the `LiquibaseImportIT` Failsafe test in
  `torg-codex-data`.

**Target Platform**: Linux server, the existing Spring Boot web application (`torg-codex`).

**Project Type**: Web service (hexagonal 4-module Maven reactor).

**Performance Goals**: SC-006, at most a 10% increase in typical list and detail response time.
The plan adds one in-memory set intersection per entry, one `CallerProductsPort` call per request,
and at most one extra batched query per request for list areas with embedded references.

**Constraints**:
- `torg-codex-application` and `torg-codex-domain` must stay free of Spring, JPA, Hibernate and
  Micrometer.
- There must be a single ownership mechanism (Principle V).
- Response shapes of `200` responses must not change (FR-015).
- The markup rendering order must not change (FR-012).
- Applied changesets are never edited.

**Scale/Scope**:
- 14 product-bound areas, at most about 1,100 entries each (Threat).
- 14 controllers rewired.
- 3 persistence adapters override `findAllById` (default method on the port).
- 1 reference-query service changed.
- 1 Liquibase data changeset.
- ADR-019 for DRM, accepted ADR-017 for native structured logging, and arc42 concept updates.
- An OpenAPI source-of-truth contract for the catalog REST API, including existing DTO schemas.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Pre-design | Post-design | Notes |
|---|---|---|---|
| **I. Hexagonal Architecture & Clean Ports** | PASS | PASS by design | The rule lives in `torg-codex-domain` (`CatalogAccessPolicy`, `OwnedProducts`, `EntryAccess`, `ProductBoundEntry`), and the use case and ports live in `torg-codex-application`. Adapters are in `torg-codex`: the resolver implements `CallerProductsPort`, and the observability adapter implements `CatalogAccessObserver`. `torg-codex-data` only gains `findAllById` and a reference projection, so it stays persistence-only. Dependencies point inward. A1-A6 guard production-wide boundaries and actual generic query-bean bindings, with negative bypass fixtures rather than only the existing controller names. |
| **II. SCS & Async Integration** | N/A | N/A | No AMQP or inter-service surface. |
| **III. REST & OpenAPI** | GAP today | PASS by design; implementation pending | Create `contracts/catalog-api.openapi.yaml` as the catalog API source of truth, with all 17 collections' list/detail operations, existing DTO schemas, filters, media types, and the `403` teaser/`404` contract. T015 adds the specification and response-contract checks. The Markdown contract is explanatory only. No media-type bump (ADR-008). |
| **IV. Zero-Downtime DB Migrations** | PASS | PASS by design | No schema change. The new idempotent changeset inserts derived group products before removing assignments outside the exact derived union. Groups with no product-bearing perks deliberately end unassigned and fail closed. Existing changesets are untouched. |
| **V. Security, DRM & Data Protection** | **FAIL today → PASS by design** | PASS | Repository queries, controllers and mappers currently bypass DRM; this feature closes that. The single `ROLE_<codex-id>` mechanism is kept: `ProductOwnershipResolver` remains the only reader of `GrantedAuthority`, and `CensoringSingleMechanismArchitectureTest` is unchanged. The test-adequacy rule is met because `CatalogDrmDifferentialTest` asserts anonymous ≠ owner for list and detail in all 14 areas. Logs and metrics carry no caller data. |
| **VI. Markdown Pipeline** | PASS | PASS | Pipeline and order are untouched. `Censor` receives `OwnedProducts.codexIds()`, which always includes the free tier (FR-012 clarification). |
| **VII. Observability** | GAP today | PASS by design; implementation pending | Constitution v3.0.0 and accepted ADR-017 require Boot native ECS JSON logging. T011 configures production output and verifies emitted JSON, including SLF4J key-values and exclusion of caller data. Remove any Logstash-specific dependencies/configuration rather than adding them. WARN/counter behavior remains in one adapter. |
| **VIII. Test-First / incremental builds** | PASS | PASS by design | Every code task writes tests first, observes the expected failure, implements the slice, and finishes with compilation and relevant tests passing. Reference-port signature changes and all consumers are one atomic task (T023); configuration/controller rewiring is another (T016). |
| **Governance: ADR-001** | ACTION | PASS (planned) | Enforcing DRM in the domain/application core, and the `403` teaser contract, are architecture-level decisions. **ADR-019 "Domain-level product-ownership DRM"** must be written and accepted, and `role-mapping.adoc`, `abac.adoc`, `observability.adoc` and the ADR nav updated, before the implementation is merged. |

**Result**: The revised design addresses the gates against constitution v3.0.0; this is not a
claim of runtime compliance. The OpenAPI artifact, native JSON logging, and all-entry-point
architecture guards must be implemented and verified before merge. ADR-017 is accepted;
ADR-019 still requires review and acceptance. No constitutional requirement is waived.

## Project Structure

### Documentation (this feature)

```text
specs/005-domain-drm-enforcement/
├── plan.md              # This file
├── research.md          # Phase 0 output (decisions D1–D11)
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output (validation guide)
├── contracts/
│   ├── rest-access-contract.md     # list/detail/teaser HTTP behaviour
│   ├── catalog-api.openapi.yaml    # PLANNED: authoritative catalog API specification (T015)
│   ├── catalog-access-ports.md     # ports, guarantees, ArchUnit invariants
│   └── observability.md            # WARN event, counter, alert rule
├── checklists/
│   └── requirements.md  # spec quality gate (16/16)
└── tasks.md             # Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```text
torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/
├── access/                                   # NEW package
│   ├── CatalogAccessPolicy.java              # NEW pure rule: UNASSIGNED / GRANTED / TEASER
│   ├── AccessDecision.java                   # NEW enum
│   ├── EntryAccess.java                      # NEW sealed: Granted | Teaser | Absent
│   └── OwnedProducts.java                    # NEW value object, FREE_TIER = "core-rulebook"
├── model/
│   ├── ProductBoundEntry.java                # NEW interface (id, name, products, entityType)
│   ├── ReferencesEntries.java                # NEW interface (embedded refs + wither)
│   ├── Cosm.java … Vehicle.java (14)         # MODIFIED: implements ProductBoundEntry
│   └── SpellList / MiracleList / PowerList   # MODIFIED: + implements ReferencesEntries
└── markup/EntityType.java                    # reused as catalog-area id (unchanged)
torg-codex-domain/src/test/java/.../domain/access/
├── CatalogAccessPolicyTest.java              # NEW
└── OwnedProductsTest.java                    # NEW

torg-codex-application/src/main/java/de/paladinsinn/
├── torg/codex/application/
│   ├── port/in/RestrictedCatalogQuery.java           # NEW
│   ├── port/out/CallerProductsPort.java              # NEW
│   ├── port/out/CatalogAccessObserver.java           # NEW
│   ├── port/out/CatalogEntryReference.java           # NEW record (id, name, products)
│   ├── port/out/CatalogPersistencePort.java          # MODIFIED: + findAllById
│   ├── port/out/CatalogReferencePersistencePort.java # MODIFIED: findCosmBySlug -> CatalogEntryReference
│   ├── service/RestrictedCatalogQueryService.java    # NEW
│   ├── service/ReferencedEntryResolver.java          # NEW (batched embedded-ref filter)
│   ├── service/CatalogReferenceQueryService.java     # MODIFIED: fail-closed cosm refs + observer
│   └── service/CatalogQueryService.java              # unchanged (reference data only)
└── security/NotLoggedInUserDetails.java              # MODIFIED: FREE_PRODUCT_ID = OwnedProducts.FREE_TIER
torg-codex-application/src/test/java/.../application/service/
├── RestrictedCatalogQueryServiceTest.java    # NEW
└── CatalogReferenceQueryServiceTest.java     # NEW/extended

torg-codex-data/
├── src/main/java/.../data/adapter/out/persistence/
│   ├── JpaSpell/JpaMiracle/JpaPowerPersistenceAdapter.java  # MODIFIED: override findAllById (default method on port)
│   └── JpaCatalogReferenceAdapter.java       # MODIFIED: returns CatalogEntryReference incl. products
└── src/main/resources/db/changelog/
    ├── 2026-11-14-001-migrate.yaml           # NEW changeset (sqlFile) for the data correction (FR-018)
    ├── 2026-11-14-001-migrate.sql            # NEW idempotent SQL (executed by CatalogDataFixMigrationIT too)
    └── db.changelog-master.yaml              # MODIFIED: include the new changeset last

torg-codex/src/main/java/de/paladinsinn/torg/codex/
├── api/configuration/CatalogQueryConfiguration.java  # MODIFIED: 14 RestrictedCatalogQuery beans
├── api/controller/{14 product-bound}Controller.java  # MODIFIED: RestrictedCatalogQuery + 200/403/404
├── api/controller/CatalogAccessResponses.java        # NEW helper: EntryAccess -> ResponseEntity
├── api/dto/EntryTeaserDto.java, RequiredProductDto.java   # NEW
├── api/mapper/EntryTeaserMapper.java                 # NEW (MapStruct, uses TorgMappingSupport)
├── api/security/ProductOwnershipResolver.java        # MODIFIED: implements CallerProductsPort
├── api/security/CurrentUserCensorFactory.java        # MODIFIED: Censor from OwnedProducts
└── api/observability/CatalogAccessObservabilityAdapter.java  # NEW: WARN + Micrometer counter
torg-codex/src/test/java/de/paladinsinn/torg/codex/
├── api/security/ProductOwnershipResolverTest.java     # MODIFIED: current() incl. free tier
├── api/observability/CatalogAccessObservabilityAdapterTest.java  # NEW
├── api/observability/StructuredLoggingTest.java        # NEW: serialized production ECS JSON
├── api/controller/CatalogOpenApiContractTest.java      # NEW: responses match OpenAPI
├── architecture/CatalogDrmArchitectureTest.java       # NEW: A1-A6, negative fixtures and bean checks
├── characterization/CatalogDrmDifferentialTest.java   # NEW: 14-area anonymous vs owner (Testcontainers)
└── resources/
    ├── sql/catalog-drm-seed.sql                       # NEW deterministic seed for the differential test
    └── characterization/**                            # REGENERATED fixtures (reviewed diff)

docs/modules/arc42/pages/
├── 09_architecture_decisions/019_domain-level-drm.adoc   # NEW ADR
├── 09_architecture_decisions/017_spring-boot-structured-logging.adoc  # Accepted logging decision
├── 09_architecture_decisions/_nav.adoc, index.adoc       # MODIFIED
└── 08_concepts/{abac.adoc, observability.adoc, torg_data/role-mapping.adoc}  # MODIFIED
```

**Structure Decision**: This is the existing 4-module hexagonal reactor; no module is added. The
domain rule goes into a new `domain.access` package. The use case and ports go into the existing
`application.port`/`service` packages. Adapters stay in `torg-codex` (`api.*`) and
`torg-codex-data` (`data.adapter.out.persistence`), following the current conventions enforced by
`AdapterConventionArchitectureTest`.

## Implementation Order (for `/speckit.tasks`)

Each completed task leaves the reactor buildable (constitution workflow rule 4). Test-first
red/green steps happen inside a task, not as separate completed tasks with missing symbols.
`tasks.md` defines 33 atomic tasks and their exact prerequisites.

1. **Governance**: draft/register ADR-019; ADR-017 and the constitution logging amendment are
   already accepted.
2. **Data correction**: SQL, YAML include, hash manifest and PostgreSQL migration tests in one
   task, including stale grants and empty derived groups.
3. **Domain and ports**: introduce types before consumers, with tests and implementation in
   each task; add the single ownership adapter.
4. **US1**: observability adapter, Boot native JSON configuration and emitted-output tests,
   deterministic seeds, restricted service, teaser mapping, OpenAPI source of truth; then
   atomically rewire controllers/configuration and add differential tests. Refresh fixtures.
5. **US2**: verify owner/principal independence and change censor ownership with its tests.
6. **US3**: add embedded-reference support; change the reference persistence port, projection,
   adapter, application service, configuration and affected tests together. Guard production
   inbound adapters in every package, verify query-bean generic bindings, and exercise negative
   architecture fixtures.
7. **Polish**: align docs, review final fixtures, run full validation, obtain ADR-019 acceptance.

OpenAPI schemas must describe the existing full DTOs, not generic object placeholders; include
the public Article/Publication/Tag operations and shared reference DTOs. Contract checks cover
anonymous, owner, teaser, missing and product-less responses. See the REST contract for details.

All-entry-point guard scope and explicit persistence/composition exceptions are specified in
`contracts/catalog-access-ports.md`; a named-controller allowlist alone is insufficient.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

*No exception is requested. Pending deliverables are merge gates, not accepted violations.*
