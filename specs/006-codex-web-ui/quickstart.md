# Quickstart & Validation Guide: Torg Codex Web User Interface

**Feature**: `006-codex-web-ui` | **Spec**: [spec.md](./spec.md) | **Data Model**: [data-model.md](./data-model.md) | **Contracts**: [web-ui-routes.md](./contracts/web-ui-routes.md), [catalog-browse-and-usage-ports.md](./contracts/catalog-browse-and-usage-ports.md), [observability-and-logging.md](./contracts/observability-and-logging.md)

## Prerequisites

- **JDK 25** (`java -version` reports 25.x)
- **Docker** daemon running (required for Testcontainers PostgreSQL integration and differential tests)
- Repository root: `./mvnw` wrapper (no Node.js or external frontend tooling needed per `ADR-020` and `ADR-021`)

---

## 1. Automated Verification Commands

### 1.1 Domain & Application Unit Tests (Common Core Model, Slugs, Descriptors, Usage Observer)

```bash
./mvnw -pl torg-codex-domain,torg-codex-application test
```

**Expected outcomes**:
- All 17 `CatalogArea` descriptors validate summary columns, filter facets, and detail sections (`data-model.md` §1).
- `CatalogSlugs` normalizes names/diacritics deterministically and disambiguates intra-category slug collisions.
- `RestrictedCatalogQueryService`, `CatalogQueryService`, and `CatalogBrowseService` invoke `CatalogUsageObserver` accurately across `DRM_GRANTED`, `PUBLIC_REFERENCE`, `TEASER`, and `NOT_FOUND`, ignoring unknown filter values and free-text `q` (`catalog-browse-and-usage-ports.md` §1–§2).

### 1.2 Architecture & Boundary Guard Tests

```bash
./mvnw -pl torg-codex -am -Dtest='*ArchitectureTest*' -Dsurefire.failIfNoSpecifiedTests=false test
```

**Expected outcomes**:
- `CatalogDrmArchitectureTest` (`A1`–`A6`) passes with zero bypasses.
- `AdapterConventionArchitectureTest` and `WebUiSurfaceSeparationArchitectureTest` (`W1`–`W4`) verify `@RestController` vs. `@Controller` placement, `web.codex` vs. `web.admin` surface isolation, single-mechanism censoring (`CurrentUserCensorFactory`), and SLF4J-only logging (`ADR-022`).

### 1.3 Web UI, DRM Differential, htmx, Authentication, Metrics, and Logging Tests

```bash
./mvnw -pl torg-codex -am -Dtest='CodexWebUi*Test,CatalogUsageObservability*Test,StructuredLoggingTest,CatalogOpenApiContractTest' -Dsurefire.failIfNoSpecifiedTests=false test
```

**Expected outcomes**:
- **Generic 17-Category Rendering & Slugs**: Every category route (`/<category>`) renders its Bootstrap 5 table (`200 OK`, `Vary: HX-Request`), and every accessible entry opens at both `/<category>/<slug>` and `/<category>/<uuid>` (`web-ui-routes.md` §2.2–§2.3).
- **htmx Fragment Filtering**: Requests with `HX-Request: true` and `HX-Target: catalog-results` return only the `#catalog-results` table fragment with matching rows or the empty-state banner (`ADR-021`).
- **DRM Enforcement Across All 14 Product-Bound Areas**:
  - Anonymous requests list and count `core-rulebook` entries only; `sourcebook-aysle`-only entries are absent from HTML and DOM attributes.
  - Direct anonymous `GET /<category>/<aysle-entry>` returns `403 Forbidden` with the purchase teaser (`teaser.jte`) showing only entry name, category, and unlocking publication(s).
  - Product-less entries return `404 Not Found` (`notFound.jte`) and increment `torg.codex.catalog.entries.without.products{area}`.
  - Authenticated owner of `sourcebook-aysle` sees full detail (`200 OK`) and uncensored `<IF:sourcebook-aysle>` prose.
- **Dedicated DriveThruRPG API-Key Login & Logout**:
  - Global navbar contains zero `<input>` credential fields (`SC-006`).
  - `GET /login` and `GET /logout` render dedicated reader-themed pages; WireMock-stubbed `POST /login` establishes session `ROLE_<codex-id>` ownership on valid key and returns `401` with user-friendly message on invalid key; `POST /logout` clears session (`web-ui-routes.md` §2.4).
- **Metrics & Logging Policy (`observability-and-logging.md`)**:
  - Granular counters (`usage.category`, `usage.cosm`, `usage.grouping`, `usage.detail`) and all 8 roll-up counters (`outcome.*` and `family.*`) match exact expected totals on `/actuator/prometheus`, with untrusted missing slugs mapped to `entry="_not_found_"`.
  - Invalid logins, `403` teasers, and `404` lookups emit `INFO`/`DEBUG` ECS JSON records only — **zero** `WARN` or `ERROR` records and **zero** leaked API keys or `ROLE_<codex-id>` strings.
- **REST Compatibility**: `CatalogOpenApiContractTest` continues to pass unchanged.

### 1.4 Full Reactor Verification

```bash
./mvnw clean verify
```

---

## 2. Interactive Smoke Check

Start the application locally (with PostgreSQL / dev configuration):

```bash
./mvnw -pl torg-codex -am spring-boot:run
```

1. Open `http://localhost:8080/` — verify category cards, accessible free-tier counts, Missing Books section, and Sign In link (no inline key box in navbar).
2. Navigate through *Storm Knights → Perks* (`/perks`), *Gear & Vehicles → Gear* (`/items`), *Game Master → Cosms* (`/cosms`), and *Reference → Publications* (`/publications`) — toggle Cosm and Clearance checkboxes and type in the search box to observe instant htmx table updates and updated browser URLs (`?cosm=...`).
3. Click an entry row (e.g., `/cosms/aysle`) and follow an in-text `<type:slug>` cross-reference link.
4. Visit `http://localhost:8080/login`, sign in with a DriveThruRPG API key, verify unlocked counts update, then visit `http://localhost:8080/logout` and confirm sign-out.
5. Inspect `http://localhost:8080/actuator/prometheus` to verify `torg_codex_catalog_usage_*_total` counters.
