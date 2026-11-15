---

description: "Actionable, dependency-ordered tasks for 006-codex-web-ui"
---

# Tasks: Torg Codex Web User Interface

**Input**: Design documents from `/specs/006-codex-web-ui/` (`spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`)

**Prerequisites**: `plan.md` (required), `spec.md` (required for user stories), `research.md`, `data-model.md`, `contracts/`, `quickstart.md`, `.specify/memory/constitution.md`

**Tests**: Included across all phases as explicitly required by `FR-020` in `spec.md` and Constitution Principle VIII.

**Organization**: Tasks are grouped by user story to enable independent implementation, verification, and incremental delivery while keeping the 4-module Maven reactor buildable after every completed task.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks in the same wave)
- **[Story]**: Which user story this task belongs to (`[US1]`, `[US2]`, `[US3]`, `[US4]`)
- Exact repository-relative file paths are included in every task description

## Execution Rules

- Use JDK 25 and `./mvnw` from the repository root. Build dependent modules with `-am`; for targeted Surefire runs add `-Dsurefire.failIfNoSpecifiedTests=false`.
- Keep the reactor buildable after every completed task (Constitution Workflow Gate 4).
- `torg-codex-domain` and `torg-codex-application` MUST remain free of Spring, JPA, Hibernate, JTE, and Micrometer (`ArchUnit A5`, `DomainPurityArchitectureTest`, `ApplicationPurityArchitectureTest`).
- Never bypass `RestrictedCatalogQuery` for the 14 product-bound areas (`ArchUnit A1`–`A6`) or `CurrentUserCensorFactory` for prose rendering (`Principle V` & `VI`).
- Never alter applied Liquibase changesets (`Principle IV`); this feature requires zero database schema migrations.

---

## Phase 1: Setup (Shared Infrastructure & Assets)

**Purpose**: Self-hosted frontend dependencies (`ADR-020`, `ADR-021`), production JTE/logging configuration (`ADR-022`), and separated reader vs. reserved admin theme/layout roots (`FR-019`).

- [X] T001 Add self-hosted WebJars dependencies (`org.webjars:webjars-locator-lite`, `org.webjars.npm:bootstrap:5.3.8`, `org.webjars.npm:bootstrap-icons:1.13.1`, `org.webjars.npm:htmx.org:2.0.8`) in `torg-codex/pom.xml`
- [X] T002 Configure production precompiled JTE (`gg.jte.usePrecompiledTemplates: true`) and `ADR-022` `INFO` default logging levels in `torg-codex/src/main/resources/application.yaml` and move local `development-mode` / `debug` overrides to `torg-codex/src/main/resources/application-dev.yaml`
- [X] T003 [P] Create the reader theme stylesheet (Bootstrap 5 `--bs-*` tokens, per-cosm badge colors, clearance symbol typography, responsive table styles) in `torg-codex/src/main/resources/static/themes/codex/codex.css`
- [X] T004 [P] Create the isolated reserved `/admin` surface boundary with package documentation in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/admin/package-info.java`, separate admin layout shell in `torg-codex/src/main/jte/admin/layout/page.jte`, and separate admin theme stylesheet in `torg-codex/src/main/resources/static/themes/admin/admin.css`

---

## Phase 2: Foundational (Common Core Entry Model, Ports, Usage Metrics & Base Shell)

**Purpose**: Core 17-area domain abstractions, deterministic slug resolution, channel-agnostic usage metrics, generic browse/overview ports, base reader layout shell, and ArchUnit surface guards that MUST be in place before user story phases begin.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T005 [P] Implement `CatalogArea` (17 areas + `/perk-groups` alias, excluding reserved prefixes), `NavigationSection`, `GroupingDimension`, and `DetailAccessOutcome` enums in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/catalog/` with unit tests in `torg-codex-domain/src/test/java/de/paladinsinn/torg/codex/domain/catalog/CatalogAreaTest.java`
- [X] T006 [P] Implement deterministic `CatalogSlugs` normalizer (NFKD diacritic stripping, kebab-case normalization, UUID syntax check, deterministic intra-area collision suffixing) in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/catalog/CatalogSlugs.java` with unit tests in `torg-codex-domain/src/test/java/de/paladinsinn/torg/codex/domain/catalog/CatalogSlugsTest.java`
- [X] T007 Create common core `CatalogEntry` interface in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/CatalogEntry.java`, extend `ProductBoundEntry.java`, and implement `CatalogEntry` (`slug()`, `catalogArea()`, `clearance()`, `cosmSlugs()`, `productCodexIds()`) across all 17 domain records in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/` with unit tests in `torg-codex-domain/src/test/java/de/paladinsinn/torg/codex/domain/model/CatalogEntryContractTest.java`
- [X] T008 Implement generic category metadata descriptors (`ColumnDescriptor.java`, `FilterFacetDescriptor.java`, `DetailSectionDescriptor.java`, `CatalogCategoryDescriptor.java`) and all 17 area registrations in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/catalog/CatalogCategoryRegistry.java` with unit tests in `torg-codex-domain/src/test/java/de/paladinsinn/torg/codex/domain/catalog/CatalogCategoryRegistryTest.java`
- [X] T009 Define `CatalogUsageObserver` (with `NO_OP` constant) in `torg-codex-application/src/main/java/de/paladinsinn/torg/codex/application/port/out/CatalogUsageObserver.java` and extend `RestrictedCatalogQuery.java`, `CatalogQuery.java`, `RestrictedCatalogQueryService.java`, and `CatalogQueryService.java` in `torg-codex-application/src/main/java/de/paladinsinn/torg/codex/application/` with `findByIdentifier(String)` and usage notifications (`U1`–`U3`), verified by unit tests in `torg-codex-application/src/test/java/de/paladinsinn/torg/codex/application/service/CatalogUsageInstrumentationTest.java`
- [X] T010 [P] Implement Micrometer adapter `CatalogUsageObservabilityAdapter` (4 granular counters + 4 outcome roll-up counters + 4 metric-family roll-up counters, `"_not_found_"` sentinel, zero caller tags) in `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/observability/CatalogUsageObservabilityAdapter.java` with unit tests in `torg-codex/src/test/java/de/paladinsinn/torg/codex/api/observability/CatalogUsageObservabilityAdapterTest.java`
- [X] T011 Define generic UI driving ports and value types (`CatalogBrowseQuery.java`, `CatalogFilterCriteria.java`, `CatalogBrowseResult.java`, `CatalogDetailResult.java`, `CodexOverviewQuery.java`, `CodexHomeOverview.java`) in `torg-codex-application/src/main/java/de/paladinsinn/torg/codex/application/port/in/` and implement `CatalogBrowseService.java` and `CodexOverviewService.java` in `torg-codex-application/src/main/java/de/paladinsinn/torg/codex/application/service/` with unit tests in `torg-codex-application/src/test/java/de/paladinsinn/torg/codex/application/service/CatalogBrowseServiceTest.java` and `CodexOverviewServiceTest.java`
- [X] T012 Wire `CatalogUsageObserver`, the 14 `RestrictedCatalogQuery` beans, the 3 concrete `CatalogQuery<Article|Publication|Tag>` beans, `CatalogBrowseQuery`, and `CodexOverviewQuery` in `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/configuration/CatalogQueryConfiguration.java` while keeping `CatalogDrmArchitectureTest` (`A1`–`A6`) green
- [X] T013 [P] Create shared reader layout view models (`NavContextViewModel.java`, `SanitizedHtml.java`, `CosmBadgeViewModel.java`) in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/model/` and shared JTE shell/fragments (`layout/page.jte`, `layout/navbar.jte`, `layout/footer.jte`, `fragments/richText.jte`, `fragments/badges.jte`) in `torg-codex/src/main/jte/codex/` (isolating `$unsafe` strictly to `richText.jte` and omitting any navbar credential `<input>`)
- [X] T014 Relocate `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/controller/LoginController.java` to `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/CodexAuthController.java`, update `W1` in `torg-codex/src/test/java/de/paladinsinn/torg/codex/architecture/AdapterConventionArchitectureTest.java`, and create `W2`–`W4` surface/censoring/SLF4J guards in `torg-codex/src/test/java/de/paladinsinn/torg/codex/architecture/WebUiSurfaceSeparationArchitectureTest.java`

**Checkpoint**: Foundation ready — generic 17-area descriptors, slug lookup, 12 usage counters, reader shell, and `W1`–`W4` ArchUnit guards are active and green.

---

## Phase 3: User Story 1 - Browse Catalog Categories in Tabular Views (Priority: P1) 🎯 MVP

**Goal**: Deliver the landing overview page (`/`), persistent global 17-category navigation, generic metadata-driven tabular category views (`/{category}` + `/perk-groups` redirect), and interactive htmx search/facet filtering/sorting with `Vary: HX-Request` cache safety.

**Independent Test**: Open `/`, navigate to each of the 17 categories via global navigation, verify tabular columns match category metadata for accessible entries, and verify htmx filter/search requests (`HX-Request: true`, `HX-Target: catalog-results`) return filtered table fragments, update sorting/empty states, and increment category/cosm/grouping counters.

### Tests for User Story 1 ⚠️

> **NOTE: Write tests FIRST and confirm they fail before implementing the controllers/templates.**

- [X] T015 [P] [US1] Write MockMvc tests for `GET /` landing overview, all 17 `GET /{category}` tabular views, `/perk-groups` → `/groups` redirect, `HX-Request` partial table updates, `Vary: HX-Request` headers, sorting/empty states, and category/cosm/grouping counter increments in `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/codex/CodexCategoryBrowseControllerTest.java`

### Implementation for User Story 1

- [X] T016 [P] [US1] Create landing and tabular view models (`HomeViewModel.java`, `TableViewModel.java`, `TableRowViewModel.java`, `TableCellViewModel.java`, `FilterPanelViewModel.java`) in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/model/`
- [X] T017 [US1] Implement `NavContextViewModel`, `HomeViewModel`, and generic `TableViewModel` mapping in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/mapper/CodexViewMapper.java`
- [X] T018 [P] [US1] Create landing overview template (17-category accessible count cards, ownership progress bar, owned and missing publication cards with DriveThruRPG links) in `torg-codex/src/main/jte/codex/pages/home.jte`
- [X] T019 [P] [US1] Create generic category page template in `torg-codex/src/main/jte/codex/pages/category.jte` and htmx filter/table fragments in `torg-codex/src/main/jte/codex/fragments/filterPanel.jte` and `torg-codex/src/main/jte/codex/fragments/categoryResults.jte`
- [X] T020 [US1] Implement `GET /` landing endpoint in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/CodexHomeController.java`
- [X] T021 [US1] Implement `GET /{category}` (plus `/perk-groups` → `/groups` `302` redirect, `Vary: HX-Request`, `Cache-Control: private, no-store`, and full-page vs. `categoryResults` fragment rendering) in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/CodexCatalogController.java`

**Checkpoint**: User Story 1 (MVP) is complete and independently verifiable via `CodexCategoryBrowseControllerTest`.

---

## Phase 4: User Story 2 - Inspect Single Elements in a Detailed View (Priority: P1)

**Goal**: Deliver the generic single-element detailed view (`/{category}/{identifier}`) resolving both canonical human-readable slugs and UUIDs across all 17 categories, rendering censored & sanitized prose, `<type:slug>` cross-references, embedded entry lists, and source publication links.

**Independent Test**: Open accessible entries across categories by both canonical slug (`/perks/ace-pilot`, `/groups/...`, `/cosms/...`, `/articles/...`) and UUID; verify full attributes/stat blocks, `Censor`-processed `<IF:…>` prose, navigable cross-references, publication attribution, and `DRM_GRANTED` / `PUBLIC_REFERENCE` counter increments.

### Tests for User Story 2 ⚠️

- [X] T022 [P] [US2] Write MockMvc tests for `GET /{category}/{identifier}` covering canonical slug and UUID equivalence, metadata-driven attribute/stat/weaponry sections, `<IF:…>` prose censoring, `<type:slug>` cross-reference links, embedded list links, publication links, and `DRM_GRANTED` / `PUBLIC_REFERENCE` usage counters in `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/codex/CodexEntryDetailControllerTest.java`

### Implementation for User Story 2

- [X] T023 [P] [US2] Create detail view models (`DetailViewModel.java`, `AttributeGroupViewModel.java`, `RenderedProseSection.java`, `LinkedEntryViewModel.java`, `PublicationLinkViewModel.java`) in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/model/`
- [X] T024 [US2] Extend `CodexViewMapper` with `toDetailViewModel` (rendering prose exclusively via `CurrentUserCensorFactory.create()` into `SanitizedHtml` and mapping embedded references and source publications) in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/mapper/CodexViewMapper.java`
- [X] T025 [P] [US2] Create generic single-element detail page template (header badges, dynamic attribute/stat groups, `richText.jte` prose blocks, embedded entry links, publication attribution footer) in `torg-codex/src/main/jte/codex/pages/detail.jte`
- [X] T026 [US2] Extend `CodexCatalogController` with `GET /{category}/{identifier}` (and `/perk-groups/{identifier}` → `/groups/{identifier}` redirect) in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/CodexCatalogController.java`

**Checkpoint**: User Stories 1 and 2 work end-to-end across tabular browsing, cross-reference navigation, and single-element inspection.

---

## Phase 5: User Story 3 - Enforce Product-Ownership DRM Across Every Display (Priority: P1)

**Goal**: Render `403` purchase teasers (`teaser.jte`) for unowned entries and `404` styled not-found pages (`notFound.jte`) for missing or unassigned entries across all categories, proving strict DRM differentiation (anonymous vs. product owner) and `ADR-022` log compliance across every UI display.

**Independent Test**: Execute the Testcontainers differential suite across all 14 product-bound areas and 3 reference areas comparing anonymous vs. `sourcebook-aysle` owner: verify non-owned entries are absent from tables/counts/DOM attributes, unowned detail requests return `403` teasers without protected fields, product-less entries return `404` not-found and increment `entries.without.products`, embedded lists omit unowned children, and `TEASER`/`NOT_FOUND` counters + `INFO`-only logging match contracts.

### Tests for User Story 3 ⚠️

- [X] T027 [P] [US3] Create Testcontainers-backed 17-area UI DRM differential, teaser, unassigned fail-closed, embedded-list, roll-up counter, and `ADR-022` log-level test suite in `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/codex/CodexWebUiDifferentialTest.java`

### Implementation for User Story 3

- [X] T028 [P] [US3] Create purchase teaser and not-found view models (`TeaserViewModel.java`, `RequiredProductViewModel.java`, `NotFoundViewModel.java`) in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/model/`
- [X] T029 [P] [US3] Create purchase teaser template (entry name, category label, unlocking publication cards/purchase links only) in `torg-codex/src/main/jte/codex/pages/teaser.jte` and styled not-found template in `torg-codex/src/main/jte/codex/pages/notFound.jte`
- [X] T030 [US3] Extend `CodexViewMapper` (`toTeaserViewModel`), wire `403` teaser and `404` absent/unknown-category responses in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/CodexCatalogController.java`, and add reader-scoped error advice (`ADR-022` severity compliance, leaving `/api/*` untouched) in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/CodexExceptionAdvice.java`
- [X] T031 [US3] Verify `CodexWebUiDifferentialTest`, `CatalogDrmDifferentialTest`, `CatalogOpenApiContractTest`, and all `*ArchitectureTest*` suites pass via `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/codex/CodexWebUiDifferentialTest.java`

**Checkpoint**: All P1 stories (US1, US2, US3) are complete with full 17-area DRM differential proof.

---

## Phase 6: User Story 4 - Sign In and Sign Out via Dedicated Pages (Priority: P2)

**Goal**: Provide dedicated reader-themed `/login` and `/logout` pages exclusively for non-admin DriveThruRPG API-key authentication, resolving the `TorgCodexSecurityConfig.formLogin` filter conflict and ensuring zero inline credential forms in the global navigation bar.

**Independent Test**: Verify zero `<input>` fields in the global navbar, open `GET /login`, authenticate via WireMock-stubbed DriveThruRPG API key on `POST /login` to unlock sourcebook entries in session, verify invalid keys return `401` on `login.jte` with `INFO`-only logging (zero leaked keys), open `GET /logout`, and submit `POST /logout` to revert session to free-tier access.

### Tests for User Story 4 ⚠️

- [X] T032 [P] [US4] Create WireMock-backed authentication and session lifecycle test in `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/codex/CodexAuthControllerTest.java` with declarative WireMock stub mappings and payload fixtures in `torg-codex/src/test/resources/mappings/` and `torg-codex/src/test/resources/__files/`

### Implementation for User Story 4

- [X] T033 [P] [US4] Create dedicated reader-themed DriveThruRPG API-key login template in `torg-codex/src/main/jte/codex/pages/login.jte` and dedicated logout confirmation template in `torg-codex/src/main/jte/codex/pages/logout.jte`
- [X] T034 [US4] Update `torg-codex/src/main/java/de/paladinsinn/torg/codex/security/TorgCodexSecurityConfig.java` to remove conflicting `.formLogin(...)` interception of `POST /login`, configure `.logout(...)` redirecting to `/login?logout`, and document the reserved `/admin/**` OIDC security seam
- [X] T035 [US4] Implement `GET /login`, `POST /login` (`ApiKeyAuthenticationToken` session authentication, `401` invalid-key re-render, `ADR-022` `INFO` structured logging with zero key disclosure), and `GET /logout` in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/CodexAuthController.java`, deleting obsolete Pico-CSS `torg-codex/src/main/jte/login.jte`
- [X] T036 [US4] Extend production ECS JSON log assertions in `torg-codex/src/test/java/de/paladinsinn/torg/codex/api/observability/StructuredLoggingTest.java` to verify `INFO` severity and credential/ownership exclusion on login rejection, teaser, and not-found events

**Checkpoint**: All 4 user stories (US1–US4) are complete and independently verified.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Synchronize arc42 architecture/observability documentation and run full-reactor verification (`quickstart.md`).

- [X] T037 [P] Update building-block and observability documentation for `web.codex` / `web.admin` adapters and the 12 `torg.codex.catalog.usage.*` Prometheus counters in `docs/modules/arc42/pages/05_building_block_view/_level2.adoc` and `docs/modules/arc42/pages/08_concepts/observability.adoc`
- [X] T038 Execute all verification suites in `specs/006-codex-web-ui/quickstart.md` and run `./mvnw clean verify` against root `pom.xml`

---

## Follow-up: Reference DriveThruRPG Protocol Alignment

- [X] T039 Add database-independent WireMock coverage for the vBeta Application Key exchange, direct token/product responses, exact library query parameters, pagination, bounded HTTP 401 renewal and sanitized upstream failures in `torg-codex/src/test/java/de/paladinsinn/drivethru/DriveThruRPGServiceTest.java`.
- [X] T040 Align the DriveThruRPG client, service, token and security integration with the reference implementation, retain the existing public method signatures and Codex product mapping, and update the auth HTTP fixtures and `CodexAuthControllerTest.java`.
- [X] T041 Synchronize the authentication building-block documentation and route contract, and verify protocol, login/logout and ownership behavior.
- [X] T042 Add WireMock contract tests and JSON:API fixtures matching `dtrpg-api/openapi.yaml` for `/auth_key`, `/auth/refresh`, `/order_products`, `/products/{productId}`, and `/publishers/{publisherId}` in `torg-codex/src/test/java/de/paladinsinn/drivethru/DriveThruRPGServiceTest.java` and `torg-codex/src/test/resources/`.
- [X] T043 Update `de.paladinsinn.drivethru` models, JSON:API envelopes, client, properties, and `DriveThruRPGService` (including `refreshToken` cookie renewal with `auth_key` fallback, `links.next` pagination, `OrderProductAttributes` / `Product` / `Publisher` mapping, and `WARN`/`DEBUG` failure metadata) while preserving ADR-015 public signatures.
- [X] T044 Update authentication/DriveThruRPG documentation (`level2_drivethrurpg_security_userservice.adoc`, `web-ui-routes.md`) and verify all DriveThruRPG, auth, DRM, and architecture test suites.

## Follow-up: Perk Prerequisite Links

- [X] T045 Render perk prerequisite table cells through the caller-specific `Censor` and shared rich-text fragment, document full-page/htmx behavior, and verify entity links, product-gated content, and empty values with `CodexPrerequisitesRenderingTest`.

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1: Setup (`T001`–`T004`)**: No dependencies — can start immediately (`T003` and `T004` run in parallel with `T001`/`T002`).
- **Phase 2: Foundational (`T005`–`T014`)**: Depends on Phase 1 — **BLOCKS all user stories**:
  - `T005` and `T006` run in parallel → `T007` → `T008` and `T009` → `T010` `[P]` and `T011` → `T012`.
  - `T013` `[P]` runs once `T001`, `T003`, and `T008` are ready; `T014` completes the controller/surface ArchUnit boundary.
- **Phase 3: User Story 1 (`T015`–`T021`, P1 MVP)**: Depends on Phase 2 completion.
- **Phase 4: User Story 2 (`T022`–`T026`, P1)**: Depends on Phase 2 completion (integrates cleanly into `CodexViewMapper` and `CodexCatalogController` after US1, or can prepare `T022`/`T023`/`T025` in parallel while US1 finishes `T020`/`T021`).
- **Phase 5: User Story 3 (`T027`–`T031`, P1)**: Depends on US1 (`T021`) and US2 (`T026`) so differential DRM verification exercises landing counts, tabular views, embedded lists, `200` details, `403` teasers, and `404` fail-closed responses together.
- **Phase 6: User Story 4 (`T032`–`T036`, P2)**: Depends on Phase 2 (`T013`, `T014`) — independent of US2/US3 template bodies and can run in parallel with US2/US3 once Phase 2 is complete.
- **Phase 7: Polish (`T037`–`T038`)**: Depends on Phases 3–6 completion.

### User Story Dependency Graph

```text
Phase 1 (Setup: T001–T004)
   │
   ▼
Phase 2 (Foundational: T005–T014)
   ├──► Phase 3: US1 Tabular Category Browse & Landing [P1 MVP] (T015–T021)
   │       │
   │       ▼
   │    Phase 4: US2 Single-Element Detail & Slugs [P1] (T022–T026)
   │       │
   │       ▼
   │    Phase 5: US3 Full-Display DRM, Teasers & Not-Found [P1] (T027–T031)
   │
   └──► Phase 6: US4 Dedicated DriveThruRPG Login & Logout [P2] (T032–T036)
           │
           ▼
        Phase 7: Polish & Full Reactor Verify (T037–T038)
```

---

## Parallel Execution Examples

### Foundational Wave

```text
Task T005: "Implement CatalogArea, NavigationSection, GroupingDimension, and DetailAccessOutcome enums in torg-codex-domain/.../domain/catalog/"
Task T006: "Implement deterministic CatalogSlugs normalizer in torg-codex-domain/.../domain/catalog/CatalogSlugs.java"
```

### User Story 1 (MVP) Wave

```text
Task T015: "Write MockMvc tests for GET / and all 17 GET /{category} views in CodexCategoryBrowseControllerTest.java"
Task T016: "Create landing and tabular view models in torg-codex/.../web/codex/model/"
Task T018: "Create landing overview template in torg-codex/src/main/jte/codex/pages/home.jte"
Task T019: "Create category page and htmx fragments in torg-codex/src/main/jte/codex/"
```

### User Story 2 Wave

```text
Task T022: "Write MockMvc tests for GET /{category}/{identifier} in CodexEntryDetailControllerTest.java"
Task T023: "Create detail view models in torg-codex/.../web/codex/model/"
Task T025: "Create generic detail template in torg-codex/src/main/jte/codex/pages/detail.jte"
```

### User Story 3 & User Story 4 Concurrent Wave

```text
Task T027: "Create Testcontainers 17-area UI DRM differential suite in CodexWebUiDifferentialTest.java"
Task T028: "Create TeaserViewModel and NotFoundViewModel in torg-codex/.../web/codex/model/"
Task T029: "Create teaser.jte and notFound.jte in torg-codex/src/main/jte/codex/pages/"
Task T032: "Create WireMock authentication test in CodexAuthControllerTest.java"
Task T033: "Create login.jte and logout.jte in torg-codex/src/main/jte/codex/pages/"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete **Phase 1: Setup** (`T001`–`T004`).
2. Complete **Phase 2: Foundational** (`T005`–`T014`).
3. Complete **Phase 3: User Story 1** (`T015`–`T021`).
4. **STOP and VALIDATE**: Run `CodexCategoryBrowseControllerTest` and `*ArchitectureTest*` — all 17 categories are navigable and filterable via htmx with free-tier DRM and usage counters active.

### Incremental Delivery

1. **Foundation (`T001`–`T014`)** → Common 17-area entry model, deterministic slugs, 12 Prometheus counters, reader/admin surface split.
2. **+ US1 (`T015`–`T021`)** → Landing page + generic 17-category tabular views & htmx filtering (MVP).
3. **+ US2 (`T022`–`T026`)** → Generic element detail views, slug/UUID routing, censored prose & cross-references.
4. **+ US3 (`T027`–`T031`)** → `403` purchase teasers, `404` fail-closed handling, full 17-area differential DRM proof.
5. **+ US4 (`T032`–`T036`)** → Dedicated DriveThruRPG API-key `/login` and `/logout` pages + WireMock & `ADR-022` ECS log proof.
6. **+ Polish (`T037`–`T038`)** → Arc42 docs sync + `./mvnw clean verify` green across the entire reactor.
