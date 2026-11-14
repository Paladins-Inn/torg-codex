# Research: Enforce Product-Ownership DRM at Domain Level

**Feature**: `005-domain-drm-enforcement` | **Date**: 2026-11-14 | **Spec**: [spec.md](./spec.md)

## Findings (current state)

| # | Finding | Evidence |
|---|---|---|
| F1 | Catalog use cases do not restrict by product. `CatalogQueryService` passes `findAll` / `findById` / `findByCosm` straight through to the persistence port. | `torg-codex-application/.../service/CatalogQueryService.java` |
| F2 | DRM today is only the in-text `<IF:…>` censoring of detail responses. List endpoints never use the `Censor`. | `torg-codex/.../api/controller/CosmController.java` (`list()` vs `getById()`) |
| F3 | All 17 `CatalogQuery<T>` beans are built in one place. The 14 product-bound areas are the same as the 14 `EntityType` values. | `torg-codex/.../api/configuration/CatalogQueryConfiguration.java`, `torg-codex-domain/.../markup/EntityType.java` |
| F4 | All 14 product-bound domain records already expose `UUID id()`, `String name()` and `Set<String> products()`. | `torg-codex-domain/.../domain/model/*.java` |
| F5 | Embedded references expose UUIDs of other entries: `SpellList.spells`, `MiracleList.miracles`, `PowerList.powers`. | `*ListDetailDto` records |
| F6 | Cosm cross-references are resolved to `CosmRefDto(id, name)` through `CatalogReferenceQuery.findCosmBySlug`. Publication references are reference data. | `torg-codex/.../api/mapper/TorgMappingSupport.java` |
| F7 | In-text entity references (`<spell:fireball>`) become slug-only links. Nothing is looked up, so no protected data is exposed. | `torg-codex-domain/.../markup/EntityReferenceProcessor.java` |
| F8 | No inbound paths other than the REST controllers exist. There are no JTE catalog pages (only `login.jte`) and no message consumers. | `grep @RestController/@RabbitListener/@EventListener` |
| F9 | `torg-codex-application` MUST stay free of Spring, JPA and Hibernate (ArchUnit). Only `CurrentUserCensorFactory` may depend on `ProductOwnershipResolver`. | `ApplicationPurityArchitectureTest`, `CensoringSingleMechanismArchitectureTest` |
| F10 | Anonymous callers get `ROLE_core-rulebook`. API-key callers only get `ROLE_<codexId>` for products they own, so the free tier is not guaranteed for them. | `NotLoggedInUserDetails`, `DriveThruUserDetails` |
| F11 | **Data gaps** (local full dataset): 10 of 19 cosms have no product (Aysle, Core Earth, Cyberpapacy, Living Land, Nile Empire, Orrorsh, Pan-Pacifica, Tharkold, Universal, Unknown). All 26 perk groups belong only to the product `groups`, which does not exist. All other areas are fully assigned. | Analysis of `torg-codex-data/src/main/resources/db/load/*.csv` |
| F12 | No rendered-markup or catalog cache exists. The only caches are DriveThruRPG HTTP caches, which are unrelated. | `@Cacheable` usages |
| F13 | Catalog sizes are small: at most about 1,100 entries per area (Threat 1101, Item 979, Perk 539). | CSV row counts |

## Decisions

### D1 — Enforce in a restricted application use case backed by a pure domain policy

- **Decision**: A new driving port `RestrictedCatalogQuery<T extends ProductBoundEntry>` with the
  implementation `RestrictedCatalogQueryService<T>` in `torg-codex-application`. The access rule is
  the pure domain function `CatalogAccessPolicy` in `torg-codex-domain`. All 14 product-bound
  `CatalogQuery<T>` beans are replaced by `RestrictedCatalogQuery<T>` beans. After that, no
  unrestricted query bean exists for product-bound types. Production-wide dependency guards and
  generic query-binding checks prohibit bypasses in present and future adapters, including ones
  outside `api..` (FR-010). `CatalogQuery<T>` remains only for reference data (Article, Publication,
  Tag). Guard scope, explicit exceptions and negative fixtures are defined in the port contract.
- **Rationale**: This keeps the rule in the domain/application core as the user asked ("drm has to
  work … on domain level"). It is framework-free (F9), can be unit-tested without Spring, and every
  present and future adapter inherits it.
- **Alternatives rejected**:
  - *Filtering in controllers*: this is how the defect arose; every new entry point would need to
    remember it.
  - *Spring Security `@PostFilter` / AOP*: this needs Spring in the application module (violates
    F9) and hides the rule.
  - *Hibernate filters or `WHERE product IN (…)` queries*: these put the rule in persistence, would
    have to be repeated in 14 adapters, and cannot see product-less entries, which FR-016/FR-017
    need.

### D2 — Effective owned products as a domain value; the existing resolver implements the port

- **Decision**: A domain value object `OwnedProducts` always contains the free tier
  (`OwnedProducts.FREE_TIER = "core-rulebook"`). The application driven port `CallerProductsPort`
  (`OwnedProducts current()`) is implemented by the existing `ProductOwnershipResolver`, which
  gains `implements CallerProductsPort`. `CurrentUserCensorFactory` builds its `Censor` from the
  same `OwnedProducts`, and `NotLoggedInUserDetails.FREE_PRODUCT_ID` refers to the domain
  constant.
- **Rationale**: Exactly one component still reads `GrantedAuthority` (constitution Principle V,
  FR-011), and the `CensoringSingleMechanismArchitectureTest` rules stay unchanged. Free-tier
  handling (FR-002) is defined once, so entry access and in-text censoring use the same effective
  set (FR-012). This also closes F10 for API-key users who do not own the core rulebook.
- **Alternatives rejected**:
  - *A new adapter class depending on the resolver*: this would require relaxing the
    single-mechanism ArchUnit rule.
  - *Adding the free tier in the security filter*: this would leave the domain rule depending on
    configuration in an outer layer.

### D3 — Single-entry result type and HTTP mapping

- **Decision**: `findById` returns the sealed domain type `EntryAccess<T>`, which has three cases:
  `Granted(T)`, `Teaser(id, name, requiredProducts)` and `Absent`. The REST mapping is:
  - `Granted` → `200` with the unchanged detail DTO.
  - `Teaser` → `403 Forbidden` with an `EntryTeaserDto` body.
  - `Absent` (does not exist, or has no product) → `404`, the same as today.
- **Rationale**: `403` tells the caller "exists, you are not entitled" (FR-007, clearly
  distinguishable). The detail contract and the not-found behaviour stay unchanged (FR-015), and
  product-less entries look the same as missing ones (FR-008).
- **Alternatives rejected**:
  - *`200` with a teaser body*: this breaks the typed detail contract, and clients could mistake it
    for content.
  - *`402 Payment Required`*: the semantics are non-standard.
  - *`404` for everything*: this was rejected by the clarification (teaser chosen).

### D4 — In-memory filtering of listings

- **Decision**: Load through the existing persistence port, then filter with `CatalogAccessPolicy`
  in the application service. This also applies to the cosm filter.
- **Rationale**: Product-less entries must be observed for each request (FR-016), and the rule
  lives in one pure function. The catalogs are small (F13), so an O(n) set intersection costs
  little compared with database access and JSON serialization, which stays within SC-006's ≤10%
  limit.
- **Alternatives rejected**: Filtering in the database (see D1).

### D5 — Embedded entry references are filtered in the use case

- **Decision**: `SpellList`, `MiracleList` and `PowerList` implement the domain interface
  `ReferencesEntries<SELF>` (`List<UUID> referencedEntries()`,
  `SELF withReferencedEntries(List<UUID>)`). Their restricted services get a
  `ReferencedEntryResolver`, which wraps the persistence port of the referenced area. For each
  request it:
  1. collects the union of referenced ids from all returned entries;
  2. performs **one** batched lookup (`CatalogPersistencePort.findAllById(Collection<UUID>)`, a
     new default port method, overridden with `JpaRepository.findAllById` in the three adapters of
     the referenced areas). Because the existing port's `T` has no id accessor, its compatibility
     default delegates distinct requested ids to `findById`; only the real batch overrides
     are wired to these resolvers;
  3. applies `CatalogAccessPolicy`, which reports product-less referenced entries through the
     observer;
  4. removes every id that is not `Granted`.
- **Rationale**: FR-009 requires embedded lists to omit inaccessible entries. The batched lookup
  keeps it to one extra query per request.
- **Alternatives rejected**:
  - *Filtering only in the detail mapper*: this puts the rule in the adapter, and `findAll` would
    still return unrestricted domain objects.
  - *Per-id lookup*: this causes N+1 queries.

### D6 — Cross-reference resolution

- **Decision**: `CatalogReferencePersistencePort.findCosmBySlug` returns `CatalogEntryReference`
  (`id`, `name`, `products`). `CatalogReferenceQueryService` applies `CatalogAccessPolicy`:
  - Product-less cosm → `Optional.empty()` plus an observer report. The mapper's existing fallback
    `CosmRefDto(null, slug)` then applies, exactly like an unknown slug.
  - Otherwise → `CatalogReference(id, name)`. This exposes only teaser-level information, which
    FR-009 allows: the link leads at most to the teaser.

  Publication references stay unrestricted (reference data, FR-013). In-text entity references
  need no change (F7).
- **Rationale**: FR-008 (product-less entries are invisible everywhere) is applied consistently,
  and FR-016 covers "reference resolution".

### D7 — Observability through one driven port

- **Decision**: The application driven port is
  `CatalogAccessObserver.entryWithoutProducts(EntityType area, UUID id, String name)`. It is
  implemented in `torg-codex` by `CatalogAccessObservabilityAdapter`, which, for each call:
  - logs `WARN` through `@Slf4j`, with the SLF4J 2 fluent key-values `catalogArea`, `entryId` and
    `entryName`, emitted through Spring Boot native ECS JSON logging. Accepted ADR-017 and
    constitution v3.0.0 replace the LogstashEncoder requirement. Configure native structured
    production output and remove any Logstash-specific encoder dependencies/configuration if
    found; the inspected POMs currently declare none. No encoder dependency is added;
  - increments the Micrometer counter `torg.codex.catalog.entries.without.products` with the tag
    `area` (`EntityType.tag()`). Prometheus exposes it as
    `torg_codex_catalog_entries_without_products_total{area="…"}`.

  Neither the log nor the metric carries caller data.
- **Rationale**: Micrometer must stay out of the application module (F9). One adapter guarantees
  one warning and one count per encounter (SC-008). The counter is the alerting signal
  (Principle VII).
- **Alternatives rejected**:
  - *Logging in the application service and counting in an adapter*: this risks the two drifting
    apart.
  - *A gauge of product-less entries*: this would need periodic scans and does not match the
    per-encounter semantics.
  - *An external Logstash encoder*: the user explicitly selected Boot native structured
    logging. Verify actual serialized JSON fields, not just captured log events.

### D8 — Catalog area identifier

- **Decision**: Reuse the domain enum `EntityType` (14 values = the 14 product-bound areas, F3)
  as the area identifier in the policy, the observer and the teaser.
- **Rationale**: No duplicate enum. Its `tag()` is already a stable identifier for each area.

### D9 — Data correction (FR-018)

- **Decision**: A new Liquibase changeset file `2026-11-14-001-migrate.yaml`, included last in
  `db.changelog-master.yaml`. It runs `2026-11-14-001-migrate.sql` through `sqlFile`, which
  contains idempotent SQL:
  1. Insert `(cosm_id, 'core-rulebook')` into `torg_cosm_products` for cosms with slug in
     `aysle, core-earth, cyberpapacy, living-land, nile-empire, orrorsh, pan-pacifica, tharkold,
     universal, unknown` that have no product row.
  2. Insert the distinct products of each perk group's perks (`torg_perk.perk_group = lower(replace(torg_perk_group.name, ' ', '-'))`
     joined to `torg_perk_products`) into `torg_perk_group_products` where they are missing.
  3. Delete each group-product assignment absent from the same derived union, using correlated
     `NOT EXISTS`. This removes `groups` and any other stale product assignment.
- **Rationale**:
  - This is a pure data migration (ADR-011 "migrate" phase, no schema change).
  - It is a natural no-op on the public free-tier dataset, where the catalog tables are empty.
  - It never overwrites cosms that already have products.
  - It removes stale grants only after the derived rows exist. A group with an empty derived
    union intentionally ends product-less and fails closed; no stale grant is retained.
  - Applied changesets are never edited.
- **Verified locally**: every one of the 26 current perk groups has at least one perk with
  products. Tests must also cover data outside that snapshot, including an empty derived set.

### D10 — Caching

- **Decision**: Nothing to change, because there is no render or catalog cache (F12). If one is
  introduced later, the effective `OwnedProducts` must be part of its key. This is recorded in the
  data model.

### D11 — Test strategy (Principle VIII, FR-014)

- **Domain unit tests**: `CatalogAccessPolicyTest` (granted, teaser, absent, multi-product, free
  tier, unknown products), `OwnedProductsTest`.
- **Application unit tests** (fake ports, no Spring): `RestrictedCatalogQueryServiceTest` covers
  list, cosm filter, findById for the three outcomes, observer calls per encounter, and embedded
  reference filtering with a single batched lookup. `CatalogReferenceQueryServiceTest` covers
  product-less cosms.
- **Adapter tests**: `CatalogAccessObservabilityAdapterTest` (`SimpleMeterRegistry` and a
  log-capture appender assert one `WARN` and +1 count, with no caller data).
  `StructuredLoggingTest` also verifies parsed production ECS JSON and structured field retention.
  `ProductOwnershipResolverTest` is extended for `current()` including the free tier.
- **Integration (Testcontainers, MockMvc; Surefire `*Test`, following the `torg-codex` module convention of `CensoringDifferentialTest`)**: `CatalogDrmDifferentialTest`. It seeds its own minimal
  entries through `@Sql`, so it is independent of the proprietary data, for every product-bound
  area: one `core-rulebook` entry, one `sourcebook-aysle`-only entry, one product-less entry, and
  list entries that reference them. It asserts the differences between anonymous and
  `ROLE_sourcebook-aysle` callers for list, cosm filter and detail (200 vs 403 teaser), the 404
  for product-less entries, and the metric increase.
- **Data correction (Failsafe, `torg-codex-data`)**: `CatalogDataFixMigrationIT` seeds a product-less core cosm, an already-assigned core cosm, groups with `groups` and stale non-`groups` grants, an empty-derived-set group, and perks from two products. It executes `2026-11-14-001-migrate.sql` twice and asserts exact derived sets, removal of stale grants, idempotency, and preservation of already-assigned cosms.
- **Architecture (ArchUnit)**:
  1. Import all production modules and enforce persistence boundaries for inbound adapters
     everywhere, including helpers and concrete-adapter dependencies. Exceptions are explicit
     catalog application services, persistence internals, and the exact composition class.
  2. All product-bound inbound reads use restricted driving ports; unrestricted/raw query types
     and product-bound `CatalogQueryService` constructions are forbidden. Check actual bean
     bindings, and demonstrate violations with controller/page/consumer and helper fixtures
     outside the current controller list.
  3. Every domain record named after an `EntityType` implements `ProductBoundEntry`.
- **Regression proof**: replacing `RestrictedCatalogQueryService` filtering with a pass-through
  makes `RestrictedCatalogQueryServiceTest` and `CatalogDrmDifferentialTest` fail (SC-005).
- **Characterization fixtures**: regenerate with `-Dcharacterization.capture=true`.
  Entry filtering applies to owners too; effective-free-tier rendering can change authenticated
  Article and catalog details. Review changes against ownership, sample selection and reference
  rules, rather than restricting allowed changes to anonymous filenames.
- **OpenAPI**: T015 creates `contracts/catalog-api.openapi.yaml` as the authoritative catalog
  REST specification, with concrete existing DTO schemas for all 17 collections and the new
  teaser response. Response-contract tests validate real anonymous/owner/public responses.
- **Incremental builds**: each code task contains its test-first red/green cycle and finishes
  buildable. The reference-port return-type change and all consumers are one task; the
  controller/configuration replacement is also atomic. Missing-type compilation failures
  are not accepted as completed test tasks.

## Open items

The six approved analysis remediations are incorporated in the design/task breakdown.
The following other analysis findings have not been approved or resolved in this revision:
- I4: distinguish historical counter totals from zero new encounters after data correction.
- G2: define the latency baseline/statistic and add measurable SC-006 performance verification.
- A1: decide the warning/counting boundary for repeated references to one unassigned entry
  inside a single HTTP request.

Do not treat these as verified acceptance criteria merely because the implementation tasks
compile. They require a separate follow-up decision/refinement.
