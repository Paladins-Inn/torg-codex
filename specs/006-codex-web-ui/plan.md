# Implementation Plan: Torg Codex Web User Interface

**Branch**: `006-codex-web-ui` | **Date**: 2026-11-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/006-codex-web-ui/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command; its definition describes the execution workflow.

## Summary

Deliver a self-contained browser UI inside the `torg-codex` module replacing `https://torg-codex.com`, built with **JTE**, **Bootstrap 5**, **Bootstrap Icons**, and **htmx** (`ADR-020`, `ADR-021`):

- **Common Core Entry & Generic 17-Category Pipeline (`FR-016`)**: Introduce `CatalogArea` (all 17 areas: 14 product-bound + Publications, Articles, Tags), `CatalogEntry` (common core interface implemented by all 17 domain records), deterministic human-readable slug + UUID resolution (`CatalogSlugs`), and `CatalogCategoryDescriptor` metadata so **one** generic reader controller and **one** set of JTE page/fragment templates render tabular listings, multi-facet htmx filters, full detail pages, and `403` purchase teasers across all 17 categories.
- **Strict Domain-Level DRM & Markup Censoring (`FR-009`–`FR-013`, `ADR-019`)**: All 14 product-bound areas read exclusively through `RestrictedCatalogQuery`; public reference areas read through `CatalogQuery<Article|Publication|Tag>`; rich-text prose renders through `CurrentUserCensorFactory` (`<IF:…>` censoring + sanitized CommonMark) into a single `$unsafe` JTE boundary.
- **Dedicated Non-Admin Login & Logout Pages (`FR-014`, `SC-006`)**: Replace the legacy inline navbar key box and Pico-CSS `login.jte` with dedicated reader-themed `/login` and `/logout` pages using DriveThruRPG API-key authentication exclusively for non-admin sessions (fixing the `TorgCodexSecurityConfig.formLogin` `POST /login` filter conflict).
- **Reader vs. Reserved Admin Surface Isolation (`FR-019`, `ADR-020`)**: Isolate reader controllers (`web.codex..`), JTE layouts (`jte/codex/`), and stylesheet (`/themes/codex/codex.css`) from the reserved `/admin/**` namespace (`web.admin..`, `jte/admin/`, `/themes/admin/admin.css`), enforced by ArchUnit.
- **Channel-Agnostic Usage Counters & Operational Logging (`FR-017`, `FR-018`, `ADR-022`)**: Instrument core query services via driven port `CatalogUsageObserver` and Micrometer adapter `CatalogUsageObservabilityAdapter` (4 granular counter families + 4 outcome roll-up counters + 4 metric-family roll-up counters, strictly cardinality-bounded with zero caller PII), and enforce `ADR-022` operational log-level discipline (`INFO` default in production, domain rejections at `INFO`, never `WARN`/`ERROR`).

Details: [research.md](./research.md), [data-model.md](./data-model.md), [quickstart.md](./quickstart.md), [contracts/](./contracts/).

## Technical Context

**Language/Version**: Java 25 (Maven wrapper `./mvnw`, 4-module reactor)

**Primary Dependencies**:
- *Existing*: Spring Boot 4.1.1 (Web MVC, Security, Data JPA, Actuator), JTE (`gg.jte:jte-spring-boot-starter-4:3.2.3`, `jte-maven-plugin:3.2.3`), Micrometer + Prometheus, MapStruct 1.6.3, Lombok, Liquibase, ArchUnit 1.4.1, JUnit 5 / AssertJ / Mockito, WireMock, Testcontainers PostgreSQL.
- *New WebJars in `torg-codex/pom.xml`* (self-hosted, zero Node/npm build): `org.webjars:webjars-locator-lite` (Boot-managed `1.1.4`), `org.webjars.npm:bootstrap:5.3.8`, `org.webjars.npm:bootstrap-icons:1.13.1`, `org.webjars.npm:htmx.org:2.0.8`.

**Storage**: PostgreSQL via Liquibase — **no database schema or data migration required** (`ddl-auto: validate` unchanged; canonical slugs on the 15 UUID-only tables are derived deterministically from `name` in `torg-codex-domain`, while `Cosm.slug` and `Publication.codexId` use existing columns).

**Testing**:
- `./mvnw -pl torg-codex-domain,torg-codex-application test`: unit tests for `CatalogSlugs`, `CatalogCategoryRegistry` descriptors, `CatalogBrowseService`, `CodexOverviewService`, and `CatalogUsageObserver` instrumentation.
- `./mvnw -pl torg-codex -am test`: ArchUnit guards (`A1`–`A6` + `W1`–`W4`), `CatalogUsageObservabilityAdapterTest`, WireMock-backed `CodexAuthControllerTest`, Testcontainers-backed `CodexWebUiDifferentialTest` (anonymous vs. `sourcebook-aysle` owner across all 17 categories, htmx fragments, teasers, unassigned entries), `StructuredLoggingTest`, and `CatalogOpenApiContractTest`.
- `./mvnw clean verify`: full reactor build including Failsafe `LiquibaseImportIT`.

**Target Platform**: Linux server / container running the Spring Boot `torg-codex` self-contained web service.

**Project Type**: Self-Contained System (SCS) web application + REST service (hexagonal 4-module Maven reactor).

**Performance Goals**:
- Interactive htmx category filtering/searching updates `#catalog-results` in `< 1s` under single-instance load (`SC-004`).
- Precompiled JTE templates (`gg.jte.usePrecompiledTemplates: true`) for near-zero template rendering overhead in production (`D2`).

**Constraints**:
- `torg-codex-domain` and `torg-codex-application` MUST remain 100% free of Spring, JPA, Hibernate, JTE, and Micrometer (`ArchUnit` purity + `A5`).
- All 14 product-bound areas MUST be read solely via `RestrictedCatalogQuery` (`ArchUnit A1`–`A6`); single `ROLE_<codex-id>` ownership mechanism via `CallerProductsPort` / `CurrentUserCensorFactory` (`Principle V`).
- Existing `/api/*` REST response shapes and `catalog-api.openapi.yaml` MUST remain unchanged (`FR-015` of 005).
- Unowned entries/fields MUST NEVER appear in HTML rows, counts, or `data-*` attributes; untrusted missing identifiers MUST NEVER create unbounded metric labels (`entry="_not_found_"` sentinel).
- Domain/user errors (`401` invalid key, `403` teaser, `404` not-found) MUST log at `INFO`/`DEBUG`, never `WARN`/`ERROR` (`ADR-022`, `FR-018`).

**Scale/Scope**:
- All 17 catalog areas (~2,000 total entries in the full proprietary dataset; max ~1,100 in `Threat`) rendered via 1 generic reader controller + 1 generic set of JTE templates.
- 1 landing overview controller, 1 auth controller (`/login`, `/logout`), 1 reader error advice, 1 reserved `/admin` layout/theme boundary.
- 12 Prometheus counter series families (4 granular + 4 outcome totals + 4 family totals).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Pre-design | Post-design | Notes |
|---|---|---|---|
| **I. Hexagonal Architecture & Clean Ports** | PASS | PASS | `CatalogArea`, `CatalogEntry`, `CatalogSlugs`, and `CatalogCategoryDescriptor` live in framework-free `torg-codex-domain`. `CatalogBrowseQuery`, `CodexOverviewQuery`, and `CatalogUsageObserver` live in framework-free `torg-codex-application`. Inbound web controllers/view-models live in `torg-codex` (`web.codex..` / `web.admin..`), and Micrometer lives in `api.observability.CatalogUsageObservabilityAdapter`. `torg-codex-data` is untouched (persistence-only). ArchUnit `A1`–`A6` and new `W1`–`W4` enforce all boundaries. |
| **II. Self-Contained Systems & Async Integration** | PASS | PASS | Reader UI ships inside the `torg-codex` vertical slice calling driving ports in-process (no synchronous self-HTTP calls over `/api/*`), using server-rendered JTE + htmx (`ADR-021`). |
| **III. Standardized REST APIs & Strict OpenAPI** | PASS | PASS | Public `/api/*` REST controllers and `specs/005-domain-drm-enforcement/contracts/catalog-api.openapi.yaml` remain untouched; `CatalogOpenApiContractTest` stays green. Browser HTML routes live outside `/api/*` and are documented in `contracts/web-ui-routes.md`. |
| **IV. Zero-Downtime DB Migrations** | PASS | PASS | Zero database schema or data changes (`D4` derives canonical slugs deterministically in domain code). Existing Liquibase changelogs and manifests remain untouched. |
| **V. Multi-Tier Security, DRM & Data Protection** | PASS | PASS | Non-admin reader login uses DriveThruRPG API key on dedicated `/login` & `/logout` pages (`D7`), populating `ROLE_<codex-id>` authorities resolved exclusively by `ProductOwnershipResolver` (`CallerProductsPort`) and `CurrentUserCensorFactory`. All 14 product-bound areas flow through `RestrictedCatalogQuery`. Differential UI tests assert anonymous ≠ `sourcebook-aysle` owner across tables, teasers, and `<IF:…>` prose. Self-hosted WebJars prevent third-party CDN IP leaks. |
| **VI. Deterministic Markdown Pipeline & Template Safety** | PASS | PASS | Markup pipeline and rendering order are untouched. JTE `Html` content-type auto-escapes all `${...}` values; `$unsafe` is isolated to `codex/fragments/richText.jte` accepting typed `SanitizedHtml` produced solely via `Censor`. |
| **VII. Production Observability & Quality Standards** | GAP (`trace` in `application.yaml`) → PASS | PASS | Removes default `debug`/`trace` overrides from production `application.yaml`, enforces `ADR-017` ECS JSON + `ADR-022` operational severity rules (`INFO` for domain rejections), and publishes all 4 granular + 8 roll-up usage counters to `/actuator/prometheus` via Micrometer (`D9`, `D10`). |
| **VIII. Test-First & Integration Verification** | PASS | PASS | TDD across domain/application unit tests, ArchUnit guards, WireMock-stubbed DriveThruRPG `/login` tests (annotation + file mappings), and Testcontainers PostgreSQL UI differential tests; every task leaves `./mvnw clean verify` green. |
| **Governance: ADR-001** | PASS | PASS | `ADR-020` (Bootstrap 5 + Bootstrap Icons + reader/admin theme separation), `ADR-021` (htmx + JTE), and `ADR-022` (Operations-First Logging Policy) are authored, accepted, and linked in `docs/modules/arc42`. |

**Gate Result**: **PASS** (both pre-design and post-design). Zero constitutional violations or exceptions required.

## Project Structure

### Documentation (this feature)

```text
specs/006-codex-web-ui/
├── plan.md              # This file (/speckit.plan output)
├── research.md          # Phase 0 output (decisions D1–D10)
├── data-model.md        # Phase 1 output (17-area common core & view models)
├── quickstart.md        # Phase 1 output (runnable validation guide)
├── contracts/
│   ├── web-ui-routes.md                  # HTML/htmx routes, headers, auth, & surface boundaries
│   ├── catalog-browse-and-usage-ports.md # Driving/driven ports & ArchUnit invariants W1–W4
│   └── observability-and-logging.md      # 12 Prometheus counter series & ADR-022 log rules
├── checklists/
│   └── requirements.md  # Spec quality checklist (16/16 passing)
└── tasks.md             # Phase 2 output (/speckit.tasks — NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/
├── catalog/                                     # NEW package: generic 17-area core metadata
│   ├── CatalogArea.java                         # NEW 17-value enum (14 EntityType + Publication/Article/Tag)
│   ├── NavigationSection.java                   # NEW enum (Storm Knights, Gear & Vehicles, GM, Reference)
│   ├── GroupingDimension.java                   # NEW 9-value enum (PerkGroup, Lists, Tag, Types, Clearance, Race)
│   ├── DetailAccessOutcome.java                 # NEW 4-value enum (DRM_GRANTED, PUBLIC_REFERENCE, TEASER, NOT_FOUND)
│   ├── CatalogSlugs.java                        # NEW deterministic slug normalizer & collision resolver
│   ├── CatalogCategoryDescriptor.java           # NEW generic column/facet/detail-section descriptor
│   ├── ColumnDescriptor.java                    # NEW summary column metadata + CellKind
│   ├── FilterFacetDescriptor.java               # NEW structured filter facet metadata
│   ├── DetailSectionDescriptor.java             # NEW ordered attribute/prose/embedded-list descriptor
│   └── CatalogCategoryRegistry.java             # NEW immutable registry of all 17 descriptors
└── model/
    ├── CatalogEntry.java                        # NEW common core interface for all 17 areas
    ├── ProductBoundEntry.java                   # MODIFIED: extends CatalogEntry
    └── Article / Publication / Tag / Cosm / …   # MODIFIED: implement CatalogEntry / cosmSlugs / groupings

torg-codex-application/src/main/java/de/paladinsinn/torg/codex/application/
├── port/
│   ├── in/
│   │   ├── RestrictedCatalogQuery.java          # MODIFIED: + findByIdentifier(String)
│   │   ├── CatalogQuery.java                    # MODIFIED: + findByIdentifier(String)
│   │   ├── CatalogBrowseQuery.java              # NEW generic 17-area browse & detail driving port
│   │   ├── CatalogFilterCriteria.java           # NEW immutable filter/search/sort criteria
│   │   ├── CatalogBrowseResult.java             # NEW filtered table model + facet options
│   │   ├── CatalogDetailResult.java             # NEW sealed: GrantedProductEntry | PublicReferenceEntry | RestrictedTeaser | Absent
│   │   ├── CodexOverviewQuery.java              # NEW landing-page overview driving port
│   │   └── CodexHomeOverview.java               # NEW landing-page overview value object
│   └── out/
│       └── CatalogUsageObserver.java            # NEW framework-free usage metrics observer port
└── service/
    ├── RestrictedCatalogQueryService.java       # MODIFIED: + findByIdentifier + CatalogUsageObserver calls
    ├── CatalogQueryService.java                 # MODIFIED: + CatalogArea + findByIdentifier + CatalogUsageObserver calls
    ├── CatalogBrowseService.java                # NEW generic 17-area filter/sort/detail use case
    └── CodexOverviewService.java                # NEW landing-page category counts & missing books use case

torg-codex/
├── pom.xml                                      # MODIFIED: + webjars-locator-lite, bootstrap, bootstrap-icons, htmx.org
├── src/main/java/de/paladinsinn/torg/codex/
│   ├── api/
│   │   ├── configuration/CatalogQueryConfiguration.java   # MODIFIED: wire CatalogUsageObserver, CatalogBrowseQuery, CodexOverviewQuery
│   │   ├── controller/LoginController.java                # REMOVED (replaced by web.codex.CodexAuthController)
│   │   └── observability/CatalogUsageObservabilityAdapter.java # NEW Micrometer 4 granular + 8 roll-up counters
│   ├── security/TorgCodexSecurityConfig.java              # MODIFIED: remove conflicting formLogin; reserve /admin/**
│   └── web/
│       ├── codex/                                         # NEW Reader UI inbound adapter package
│       │   ├── CodexHomeController.java                   # GET /
│       │   ├── CodexCatalogController.java                # GET /{category}, GET /{category}/{identifier}, /perk-groups alias
│       │   ├── CodexAuthController.java                   # GET/POST /login (DriveThruRPG API key), GET /logout
│       │   ├── CodexExceptionAdvice.java                  # Reader UI 404 / 500 error rendering + ADR-022 logging
│       │   ├── mapper/CodexViewMapper.java                # Domain + Descriptor + Censor -> Reader ViewModels
│       │   └── model/                                     # NavContextViewModel, TableViewModel, DetailViewModel, TeaserViewModel, SanitizedHtml
│       └── admin/                                         # NEW reserved Admin UI surface boundary (package-info.java)
├── src/main/jte/
│   ├── codex/                                             # NEW Reader UI JTE templates (replaces root login.jte)
│   │   ├── layout/ (page.jte, navbar.jte, footer.jte)
│   │   ├── pages/ (home.jte, category.jte, detail.jte, teaser.jte, notFound.jte, login.jte, logout.jte)
│   │   └── fragments/ (categoryResults.jte, filterPanel.jte, richText.jte, badges.jte)
│   └── admin/
│       └── layout/page.jte                                # NEW separate Admin UI shell placeholder
├── src/main/resources/
│   ├── application.yaml                                   # MODIFIED: precompiled JTE default + ADR-022 INFO logging level
│   ├── application-dev.yaml                               # NEW optional local dev overrides (JTE dev mode, debug logging)
│   └── static/themes/
│       ├── codex/codex.css                                # NEW Reader theme (Bootstrap 5 CSS vars, cosm badges, clearance)
│       └── admin/admin.css                                # NEW separate Admin theme stylesheet
└── src/test/java/de/paladinsinn/torg/codex/
    ├── architecture/
    │   ├── AdapterConventionArchitectureTest.java         # MODIFIED: enforce W1 (@RestController in api.., @Controller in web..)
    │   └── WebUiSurfaceSeparationArchitectureTest.java    # NEW: enforce W2, W3, W4
    ├── api/observability/CatalogUsageObservabilityAdapterTest.java # NEW
    └── web/codex/
        ├── CodexWebUiDifferentialTest.java                # NEW Testcontainers 17-area UI + DRM + htmx + metrics test
        └── CodexAuthControllerTest.java                   # NEW WireMock DriveThruRPG API-key login/logout + ADR-022 logging test
```

**Structure Decision**: Retain the existing 4-module hexagonal Maven reactor (`torg-codex-domain`, `torg-codex-application`, `torg-codex-data`, `torg-codex`) as an autonomous Self-Contained System. Place pure entry abstractions and category metadata in `torg-codex-domain`, generic browse/overview use cases and `CatalogUsageObserver` in `torg-codex-application`, leave `torg-codex-data` untouched, and house the reader UI (`web.codex` + `jte/codex` + `/themes/codex`) alongside the isolated reserved admin surface (`web.admin` + `jte/admin` + `/themes/admin`) in `torg-codex`.

## Complexity Tracking

> No Constitution Check violations exist; this table is intentionally empty.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| *(None)* | — | — |

