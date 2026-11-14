---
description: "Atomic implementation tasks for 005-domain-drm-enforcement"
---

# Tasks: Enforce Product-Ownership DRM at Domain Level

**Input**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`.

**Revision**: The accepted analysis remediations replace the former 53-task breakdown with
33 atomic tasks. IDs below are the new execution order; do not use the former dependency graph.
No implementation task is completed yet. ADR-017 and constitution v3.0.0 are approved
governance inputs, not evidence that native runtime logging is already configured.

## Execution Rules

- Each code task contains its own test-first cycle: write the tests, confirm the expected
  failure, implement the complete slice, then finish with compilation and targeted tests
  passing. Missing-symbol errors are intermediate work, not a completed test task.
- Keep the reactor buildable after every completed task. Change incompatible port signatures
  and every affected implementation/consumer/test atomically.
- `[P]` means independent work may run concurrently only after all listed prerequisites.
  Do not parallelize tasks touching the same file.
- Use JDK 25 and the root Maven wrapper. Build dependent modules with `-am`; for targeted
  Surefire selectors add `-Dsurefire.failIfNoSpecifiedTests=false` where upstream modules
  contain no selected tests. Testcontainers requires Docker.
- Domain/application core packages remain free of Spring, JPA, Hibernate and Micrometer.
  Follow Lombok/MapStruct conventions. Never edit applied Liquibase changesets.
- Checklist files are read-only gates; their checked items do not mean implementation is done.

## Paths and Areas

- `DOM` = `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain`
- `DOM-T` = `torg-codex-domain/src/test/java/de/paladinsinn/torg/codex/domain`
- `APP` = `torg-codex-application/src/main/java/de/paladinsinn/torg/codex/application`
- `APP-T` = `torg-codex-application/src/test/java/de/paladinsinn/torg/codex/application`
- `PERS` = `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/adapter/out/persistence`
- `WEB` = `torg-codex/src/main/java/de/paladinsinn/torg/codex`
- `WEB-T` = `torg-codex/src/test/java/de/paladinsinn/torg/codex`

The 14 product-bound records are Cosm, Item, Miracle, MiracleList, Perk, PerkGroup, Power,
PowerList, Race, Shard, Spell, SpellList, Threat and Vehicle. Their matching `EntityType`
values use `GROUP` for PerkGroup. Article, Publication and Tag remain public reference data.

## Phase 1: Setup / Governance

- [ ] T001 Draft ADR-019 in `docs/modules/arc42/pages/09_architecture_decisions/019_domain-level-drm.adoc`, using `_template.adoc` and status `Proposed`. Cover central domain/application enforcement, the effective-free-tier ownership port, the `200`/`403` teaser/`404` contract, fail-closed handling, warning/counter behavior, exact-set data correction, and rejected alternatives. Reference accepted ADR-017 for logging; do not duplicate or reverse that decision.
- [ ] T002 Register ADR-019 in `docs/modules/arc42/pages/09_architecture_decisions/_nav.adoc`, preserving all existing local edits and the ADR-017 entry. `docs/modules/arc42/pages/09_architecture_decisions/index.adoc` already includes this navigation; verify the link resolves instead of adding a duplicate listing. Depends on T001.

**Checkpoint**: ADR-019 is drafted and discoverable. Acceptance remains a merge gate.

## Phase 2: Foundational / Data and Domain

- [ ] T003 Implement and verify the data correction as one atomic task: `torg-codex-data/src/main/resources/db/changelog/2026-11-14-001-migrate.sql`, `2026-11-14-001-migrate.yaml`, the final include in `db.changelog-master.yaml`, `torg-codex-data/src/test/resources/liquibase-changelog-manifest.sha256`, and `torg-codex-data/src/test/java/de/paladinsinn/torg/codex/data/liquibase/CatalogDataFixMigrationIT.java`. Write the PostgreSQL Testcontainers test first; apply the new SQL twice with `ScriptUtils`. SQL first assigns `core-rulebook` to the ten explicit unassigned core cosm slugs, then inserts missing derived group products, then deletes every group assignment outside the exact derived union using correlated `NOT EXISTS`. Use `p.perk_group = lower(replace(g.name, ' ', '-'))` and `torg_perk_products`; removal includes `groups` and stale non-`groups` grants. Seed all ten core slugs, an already-assigned core cosm, an unrelated cosm, a group with two contributing products plus stale assignments, and a group with no product-bearing perks. Assert exact sets after both executions, preservation of already-assigned cosms, empty derived groups ending unassigned, no duplicates, and a no-op on absent affected rows. Run the new migration test, `LiquibaseImportIT`, and `LiquibaseChangelogGuardTest`; the YAML, master and hash manifest must remain consistent throughout the completed task. Depends on T002.
- [ ] T004 [P] Implement `OwnedProducts` and its tests in `DOM/access/OwnedProducts.java` and `DOM-T/access/OwnedProductsTest.java` in one test-first slice. Define `FREE_TIER = "core-rulebook"`, null-safe `of(Set<String>)`, `anonymous()`, immutable `codexIds()`, `ownsAnyOf(Set<String>)`, and Lombok value semantics. Verify empty/null input, free-tier union, matching/nonmatching/multiple products, unknown ids, and immutability. Depends on T002.
- [ ] T005 [P] Add `DOM/model/ProductBoundEntry.java` with `UUID id()`, `String name()`, `Set<String> products()`, `EntityType entityType()`, and implement it in all 14 records under `DOM/model/`. Verify the correct area mapping, including PerkGroup to `GROUP`, in `DOM-T/access/ProductBoundEntryTest.java`; do not bind Article/Publication/Tag. Depends on T002.
- [ ] T006 Implement `DOM/access/AccessDecision.java` and `CatalogAccessPolicy.java` with `DOM-T/access/CatalogAccessPolicyTest.java`. Evaluate null/empty products first as `UNASSIGNED`, any owned product as `GRANTED`, otherwise `TEASER`. Test anonymous/free-tier, owners, multi-product owns-any, stale authorities, and fail-closed ordering. Depends on T004 and T005.
- [ ] T007 [P] Implement sealed `EntryAccess<T extends ProductBoundEntry>` in `DOM/access/EntryAccess.java` and test it in `DOM-T/access/EntryAccessTest.java`. Add `Granted<T>(T entry)`, `Teaser<T>(UUID id, String name, EntityType type, SortedSet<String> requiredProducts)`, `Absent<T>()` and static factories. Teaser products are sorted and immutable; a teaser cannot retain the full entry or protected fields. Depends on T005.
- [ ] T008 Add the driving/driven interfaces `APP/port/in/RestrictedCatalogQuery.java`, `APP/port/out/CallerProductsPort.java`, and `APP/port/out/CatalogAccessObserver.java`. List methods return `List<T>`; `findById` returns `EntryAccess<T>`. Document guarantees G1-G6 from `contracts/catalog-access-ports.md`. Do not change any existing persistence signature or bean yet. Depends on T004, T006 and T007.
- [ ] T009 Implement `CallerProductsPort` on `WEB/api/security/ProductOwnershipResolver.java`, with `current() = OwnedProducts.of(resolve())`, and extend `WEB-T/api/security/ProductOwnershipResolverTest.java` atomically. Keep `resolve()` unchanged. Test absent authentication, only `ROLE_DRIVETHRU_USER`, an aysle owner without a core authority, and principal-independent authority resolution. Change `torg-codex-application/src/main/java/de/paladinsinn/security/NotLoggedInUserDetails.java` so `FREE_PRODUCT_ID` refers to `OwnedProducts.FREE_TIER`. Keep `CensoringSingleMechanismArchitectureTest` unchanged. Depends on T008.

**Checkpoint**: Domain policy, ports, single ownership source and migration are verified.
Do not activate entry-level enforcement until T003 has passed.

## Phase 3: User Story 1 - Anonymous Restricted Views (P1)

**Independent test**: For every area, anonymous list/filter responses include core entries and
exclude non-free and unassigned entries; non-owned details are `403` teasers and unassigned
details match missing-entry `404` responses.

- [ ] T010 [P] [US1] Implement `WEB/api/observability/CatalogAccessObservabilityAdapter.java` and `WEB-T/api/observability/CatalogAccessObservabilityAdapterTest.java` test-first. Use `@Component`, `@Slf4j`, and `CatalogAccessObserver`; one call logs one `WARN` with fluent key-values `catalogArea`, `entryId`, `entryName` and increments `torg.codex.catalog.entries.without.products{area}` by one. Cache counters per bounded area, never per entry/caller. Test a second encounter, field values, and exclusion of caller/authority/API-key data with `SimpleMeterRegistry` and a Logback capturing appender. Depends on T009.
- [ ] T011 [US1] Configure Spring Boot native ECS JSON logging in `torg-codex/src/main/resources/application.yaml` and the actual production profile resources, with emitted-output tests in `WEB-T/api/observability/StructuredLoggingTest.java`. Resolve the actual application configuration filenames before editing; preserve unrelated settings. Set `logging.structured.format.console=ecs`; if file logging is enabled also set its native format to `ecs`. Inspect all module `pom.xml` files, resolved dependencies, and custom logging configuration; remove any Logstash-specific encoder dependency/configuration found, adding no encoder library. The inspected POMs currently declare none, so do not claim a dependency was removed unless one is found. Parse actual production-configured output as JSON and verify ECS severity/message and all three DRM fields, with sentinel caller credentials/ownership absent. An in-memory `ListAppender` alone is not proof. Verify file output too when enabled. Depends on T010.
- [ ] T012 [P] [US1] Create deterministic seeds and cleanup in `torg-codex/src/test/resources/sql/catalog-drm-seed.sql` and `catalog-drm-cleanup.sql`. Inspect `torg-codex-data/src/main/resources/db/changelog/torg-data-entity.yaml` for constraints. Seed fixed `DRM-` entries in every area: core, aysle-only, a third unowned product, and no product. Include cosm-filter coverage, spell/miracle/power lists referencing core/aysle/unassigned children, and known/unknown publication ids for teaser mapping. Include an Article with a core-gated passage for US2. Keep scripts independent of proprietary CSVs and isolated from other test data. Depends on T009.
- [ ] T013 [P] [US1] Implement `APP/service/RestrictedCatalogQueryService.java` and `APP-T/service/RestrictedCatalogQueryServiceTest.java` in one test-first task. Constructor: area, `CatalogPersistencePort<T>`, `CallerProductsPort`, `CatalogAccessObserver`, `CatalogAccessPolicy`. Resolve ownership exactly once per invocation; apply policy to every candidate; lists keep only granted entries; details return Granted/Teaser/Absent. Report and drop every unassigned candidate, with missing and unassigned both Absent. Test list/cosm/detail, sorted teaser products, repeated requests without cross-request deduplication, and observer/ownership call counts. No Spring or `@Transactional`. Embedded filtering is added in US3 before final delivery. Depends on T009.
- [ ] T014 [P] [US1] Add `WEB/api/dto/EntryTeaserDto.java`, `RequiredProductDto.java`, MapStruct `WEB/api/mapper/EntryTeaserMapper.java`, and `WEB/api/controller/CatalogAccessResponses.java`, with tests in `WEB-T/api/mapper/TorgMappingSupportTest.java` and `WEB-T/api/controller/CatalogAccessResponsesTest.java`. Extend `WEB/api/mapper/TorgMappingSupport.java` to map sorted granting products, preserving unknown codex ids with `publication=null`. Teaser fields are exactly id/name/type/requiredProducts. The helper's generic bound must match `EntryAccess<T extends ProductBoundEntry>`; map Granted to existing `200` detail, Teaser to `403`, and Absent to the existing empty `404`. Verify mapping, null publication, product ordering, and no extra protected fields. Depends on T009.
- [ ] T015 [US1] Create `specs/005-domain-drm-enforcement/contracts/catalog-api.openapi.yaml` as the authoritative REST API contract, with structural/specification checks in `WEB-T/api/controller/CatalogOpenApiContractTest.java`. Describe all 17 collections' actual summary/detail/reference DTOs, cosm-filter support, UUID parameters, existing media types, `200`, closed `403` teaser bodies, existing `404` and invalid-UUID responses. Preserve existing required/null behavior; do not use unconstrained object placeholders for existing DTOs. Validate OpenAPI syntax and references with an actual validator, not a file-exists assertion. Establish schema assertions without requiring unwired `403` runtime behavior; T016 adds live response coverage atomically with wiring. Markdown remains explanatory. Depends on T014.
- [ ] T016 [US1] Atomically replace all 14 product-bound query beans in `WEB/api/configuration/CatalogQueryConfiguration.java`, rewire the 14 controllers in `WEB/api/controller/`, and add live differential/contract assertions in `WEB-T/characterization/CatalogDrmDifferentialTest.java` and `WEB-T/api/controller/CatalogOpenApiContractTest.java`. Inject ownership through `CallerProductsPort`, not the concrete resolver; preserve the three public `CatalogQuery` beans and the existing 15 controllers' censor-factory dependency. Map details through `CatalogAccessResponses`; retain list/filter signatures and DTO shapes. Use Testcontainers/MockMvc and isolated T012 seed/cleanup. For every area assert anonymous core access, omission of both non-free products and unassigned entries, exact `403` teaser schema, missing/unassigned response equivalence, and counter increments. Cover cosm filters where supported and validate actual responses against OpenAPI, including public reference-data responses. Keep `CensoringSingleMechanismArchitectureTest` unchanged. Do not split bean/controller replacement into separately completed tasks. Depends on T003, T011, T012, T013 and T015.
- [ ] T017 [US1] Regenerate and review fixtures in `torg-codex/src/test/resources/characterization/` with the quickstart command. Existing capture logic in `WEB-T/characterization/CharacterizationFixtureCaptureTest.java` samples repository entries; inspect any changed selected ids, not only bodies. Accept changes required by ownership filtering for anonymous callers and owners (including owner entries from unowned third products) and the data correction; do not bless unrelated differences or change DTO shapes. Run `CharacterizationReplayTest` and `CensoringDifferentialTest`. Any later core-gated rendering change follows SC-007. Depends on T016.

**Checkpoint**: Top-level anonymous list/detail DRM and native logging work. Embedded/cosm
reference protection and future-entry-point guards remain mandatory US3 work; this increment
is not the complete all-requests access contract.

## Phase 4: User Story 2 - Owner and Free-Tier Consistency (P1)

**Independent test**: An aysle owner without an explicit core authority sees both core and aysle
entries, but not third-product/unassigned entries. Effective-free-tier rendering also applies
to Articles without changing their public availability.

- [ ] T018 [P] [US2] Extend `WEB-T/characterization/CatalogDrmDifferentialTest.java` for every area: aysle owner without `ROLE_core-rulebook`, owner detail `200` with actual protected fields, omission of third-product and unassigned entries, and unassigned detail `404`. A caller with only `ROLE_DRIVETHRU_USER` sees anonymous list ids. Compare distinct principal types with identical authorities (`user(...)` and `TestingAuthenticationToken`) for identical results; exercise API-key/OIDC authority-equivalent behavior without adding a second ownership source. Depends on T017.
- [ ] T019 [US2] Change `WEB/api/security/CurrentUserCensorFactory.java` to use `ownershipResolver.current().codexIds()` with tests in `WEB-T/api/security/CurrentUserCensorFactoryTest.java`. Test core-gated output for an owner lacking an explicit core authority, and preserve the markup order. Extend `WEB-T/characterization/CatalogDrmDifferentialTest.java` for the seeded Article and an accessible catalog detail: the free-tier exception in SC-007 applies to both. Keep public Article availability and unchanged-effective-ownership rendering identical. Keep existing single-mechanism architecture and differential tests green. Depends on T018.

**Checkpoint**: Owners retain entitled access, all callers get the free tier, and entry access
and in-text censoring use the same effective ownership.

## Phase 5: User Story 3 - References and All-Entry-Point Guards (P2)

**Independent test**: Returned list records omit inaccessible child ids; unassigned cosms use
the unknown-reference fallback. Architecture tests reject newly introduced bypasses even
outside `api..`; a central-filter mutation makes behavior tests fail.

- [ ] T020 [P] [US3] Add `DOM/model/ReferencesEntries.java` and implement it in `SpellList.java`, `MiracleList.java`, `PowerList.java`, with tests in `DOM-T/access/ReferencesEntriesTest.java`. Define referenced area, UUID list, and a typed copy/wither preserving all other record fields and reference ordering. Use self typing without unchecked casts. Depends on T019.
- [ ] T021 [P] [US3] Add the default batch method to `APP/port/out/CatalogPersistencePort.java` and mapped `repository.findAllById` overrides in `PERS/JpaSpellPersistenceAdapter.java`, `JpaMiraclePersistenceAdapter.java`, `JpaPowerPersistenceAdapter.java`. The existing port has unconstrained `T` and no id accessor: implement the generic compatibility default through distinct requested ids and existing `findById`, omitting absent results, not by calling `T.id()` or casting reference data. Only the three real batch overrides are wired to embedded-reference resolvers. Add tests in `APP-T/port/out/CatalogPersistencePortTest.java` and matching `torg-codex-data/src/test/java/de/paladinsinn/torg/codex/data/adapter/out/persistence/` tests. Verify requested ids only, missing ids, empty input, and one real batch repository call per override; preserve all other adapters. Depends on T019.
- [ ] T022 [US3] Implement `APP/service/ReferencedEntryResolver.java`, extend `APP/service/RestrictedCatalogQueryService.java`, and wire the three list-area resolvers in `WEB/api/configuration/CatalogQueryConfiguration.java` atomically with tests in `APP-T/service/RestrictedCatalogQueryServiceTest.java`. Use a typed reconstruction collaborator if necessary to preserve self types; no unchecked casts or runtime area-based reconstruction switch. Collect the union of child ids across granted entries, resolve once, filter by the same owned products, report unassigned children, and preserve ordering. Test multiple lists, core/non-owned/missing/unassigned children, no references in teaser/absent results, and one batch call per invocation. Resolve caller ownership only once. Depends on T020 and T021.
- [ ] T023 [P] [US3] Perform the cosm reference signature migration atomically: add `APP/port/out/CatalogEntryReference.java`, change `CatalogReferencePersistencePort.findCosmBySlug`, update `PERS/JpaCatalogReferenceAdapter.java`, `APP/service/CatalogReferenceQueryService.java`, the reference-query bean in `WEB/api/configuration/CatalogQueryConfiguration.java`, and all affected tests/fakes/callers. Use MapStruct for the entity-to-projection conversion in the persistence adapter. Tests in `APP-T/service/CatalogReferenceQueryServiceTest.java` must show unassigned cosm to empty plus observer report, assigned cosm to id/name regardless of ownership, and unchanged publication lookup. Preserve the existing unknown-slug mapper fallback. The old delegating Optional return and new projection return must not coexist in a completed task. Depends on T019; do not run concurrently with T022 because both edit configuration.
- [ ] T024 [US3] Extend `WEB-T/characterization/CatalogDrmDifferentialTest.java` and `torg-codex/src/test/resources/sql/catalog-drm-seed.sql`/cleanup as needed. Verify core spell/miracle/power lists omit aysle children for anonymous callers and include them for owners; all callers omit missing and unassigned children. Verify an unassigned cosm produces `{id: null, name: <slug>}` and increments the cosm counter. Keep response schema checks aligned with OpenAPI. Depends on T022 and T023.
- [ ] T025 [US3] Implement production-wide A1-A6 in `WEB-T/architecture/CatalogDrmArchitectureTest.java`, with deliberately violating and valid fixtures under `torg-codex/src/test/java/de/paladinsinn/torg/codex/drmfixtures/`, following `contracts/catalog-access-ports.md`. Import production classes from all four modules, not just `api..`; inspect direct/indirect persistence, repository/entity/concrete-adapter dependencies and unrestricted query-service construction. Use exact composition/application-consumer exceptions, not package-wide exemptions. Detect raw/unresolved and product-bound `CatalogQuery` types in fields/constructors/signatures/calls. Check actual resolved Spring bean bindings: only Article/Publication/Tag may use unrestricted queries; all 14 product-bound areas use restricted ones. Negative fixtures must include a new controller, page/export and consumer outside `api..`, an indirect helper bypass, and `CatalogQueryService<Spell>` wiring; assert each is rejected. Positive fixtures prove a new restricted adapter and all three public query bindings remain valid. Keep violating fixtures out of normal production imports and preserve existing A4 unchanged. Depends on T024.
- [ ] T026 [US3] Run the central-filter regression proof from `specs/005-domain-drm-enforcement/quickstart.md`: temporarily bypass `APP/service/RestrictedCatalogQueryService.findAll`, confirm both unit and live differential tests fail, restore only that mutation, and confirm they pass. Exercise T025 negative fixtures as additional bypass proof and record results in the PR description. Depends on T025.

**Checkpoint**: All three stories and reference paths are verified; future adapters are guarded
by production-wide architecture constraints and actual query-bean checks.

## Phase 6: Polish / Documentation and Merge Gates

- [ ] T027 [P] Update `docs/modules/arc42/pages/08_concepts/torg_data/role-mapping.adoc` for central entry DRM, effective-free-tier ownership, restricted ports, and the OpenAPI-documented `403` teaser contract. Correct the stale markup ownership table to `torg-codex-domain`. Depends on T026.
- [ ] T028 [P] Update `docs/modules/arc42/pages/08_concepts/abac.adoc` to describe the actual product-ownership policy and reference ADR-019, without inventing role bypasses. Depends on T026.
- [ ] T029 [P] Update `docs/modules/arc42/pages/08_concepts/observability.adoc` with the DRM WARN fields, bounded counter and alert rule from `specs/005-domain-drm-enforcement/contracts/observability.md`. Preserve the approved Boot native logging guidance and ADR-017; remove any remaining instructions to add a Logstash encoder. Depends on T026.
- [ ] T030 [P] Update `.github/copilot-instructions.md` to document restricted driving ports, the single ownership source, `OwnedProducts.FREE_TIER`, all-entry-point guards, OpenAPI source of truth and Boot native structured logging without Logstash encoder dependencies. Depends on T026.
- [ ] T031 Regenerate final fixtures in `torg-codex/src/test/resources/characterization/`, using the command in `specs/005-domain-drm-enforcement/quickstart.md`, and review the diff against ownership, embedded references, data correction and SC-007. Article rendering may change only for the approved effective-free-tier exception; public availability and DTO shapes remain unchanged. Depends on T027-T030.
- [ ] T032 Run validation steps 1-5 in `specs/005-domain-drm-enforcement/quickstart.md`, including authoritative OpenAPI validation, native JSON-output verification, all-entry-point architecture fixtures, and migration idempotency/exact-set assertions, then `./mvnw clean verify`. Report actual results and any unresolved acceptance criteria rather than treating compilation as full validation. Optional full-data smoke checks must not require committing proprietary CSVs. Depends on T031.
- [ ] T033 After review, obtain acceptance of ADR-019 and set its status to `Accepted` in `docs/modules/arc42/pages/09_architecture_decisions/019_domain-level-drm.adoc`. Do not infer this acceptance from the separate approved ADR-017 logging decision. Depends on T032.

## Dependencies and Parallel Work

Phase 1 precedes Phase 2, which blocks story activation. The migration and independent domain
types may proceed in parallel; the migration must finish before T016 activates enforcement.
US2 depends on US1 wiring/tests; US3 depends on US2. These are explicit dependencies, not
claims that the stories are independently implementable.

```text
T001 -> T002
T002 -> T003
T002 -> T004,T005
T004,T005 -> T006
T005 -> T007
T004,T006,T007 -> T008 -> T009
T009 -> T010 -> T011
T009 -> T012,T013,T014
T014 -> T015
T003,T011,T012,T013,T015 -> T016 -> T017 -> T018 -> T019
T019 -> T020,T021,T023
T020,T021 -> T022
T022,T023 -> T024 -> T025 -> T026
T026 -> T027,T028,T029,T030
T027,T028,T029,T030 -> T031 -> T032 -> T033
```

Parallel examples after prerequisites:
- T003, T004 and T005, with T007 after T005.
- T010, T012, T013 and T014 after T009; T011 follows T010.
- T020 and T021 after T019. T023 can run with these, but not with T022.
- T027-T030 touch separate documentation files.

## Delivery Strategy

US1 is an intermediate top-level DRM milestone, not permission to stop before US3's reference
and bypass protections. Deliver all three stories, the OpenAPI contract, native JSON logging,
reviewed fixtures, full validation and accepted ADR-019 before calling the feature complete.

Total: **33 tasks**, all initially unchecked. Task counts: governance 2, foundation 7,
US1 8, US2 2, US3 7, polish 7.
