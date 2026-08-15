---

description: "Task list template for feature implementation"
---

# Tasks: Hexagonal Architecture Migration

**Input**: Design documents from `/specs/architecture-migration/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/rest-compatibility.md, quickstart.md, checklists/architecture.md

**Tests**: Architecture-conformance tests (ArchUnit) and REST/persistence characterization tests are explicitly required by the spec (FR-022 through FR-026) and are therefore included as first-class tasks, not optional.

**Organization**: Tasks are grouped by user story (US1 = uninterrupted API/data compatibility, US2 = the horizontal-by-layer hexagonal migration itself, US3 = continuous automated architecture verification). Within Setup, Foundational, and the User Story 2 migration, tasks additionally follow the mandated horizontal-by-layer sequence: **ports/interfaces → domain models → outbound adapters → inbound adapters → transaction boundaries → events**, one catalog area (of the 17 existing families) at a time, so the reactor stays buildable after every task.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/…` — framework-independent domain models, value objects, domain events
- `torg-codex-application/src/main/java/de/paladinsinn/torg/codex/application/…` — driving/driven ports, framework-free use-case services
- `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/…` — JPA entities/repositories, MapStruct entity↔domain mappers, outbound adapters (persistence, DriveThruRPG, event bridge)
- `torg-codex/src/main/java/de/paladinsinn/torg/codex/…` — REST controllers, DTO mappers, security/censor adapters, composition/configuration, transaction boundary
- `specs/architecture-migration/freeze-list.md` — version-controlled architecture exception/suppression list

Catalog families (17, per data-model.md and contracts/rest-compatibility.md): Article, Cosm, Item, Miracle, MiracleList, Perk, PerkGroup, Power, PowerList, Publication, Race, Shard, Spell, SpellList, Tag, Threat, Vehicle.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Scaffold the two new reactor modules and the tooling the rest of the migration depends on, without changing any existing behavior.

- [X] T001 Create the `torg-codex-domain` Maven module (`torg-codex-domain/pom.xml`, `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/` package root) with no Spring, JPA/Jakarta Persistence, or Hibernate dependency declared, and add `<module>torg-codex-domain</module>` to the root `pom.xml`
- [X] T002 Create the `torg-codex-application` Maven module (`torg-codex-application/pom.xml`, `torg-codex-application/src/main/java/de/paladinsinn/torg/codex/application/` package root) depending only on `torg-codex-domain` (plus Lombok/JUnit as needed), and add `<module>torg-codex-application</module>` to the root `pom.xml`
- [X] T003 [P] Add `torg-codex-domain` and `torg-codex-application` to the root `pom.xml` `<dependencyManagement>`, and add (unused-for-now) dependencies on both to `torg-codex-data/pom.xml` and `torg-codex/pom.xml`
- [X] T004 [P] Add the ArchUnit test dependency (`com.tngtech.archunit:archunit-junit5`) to the root `pom.xml` `<dependencyManagement>` and to `torg-codex/pom.xml` (chosen as the aggregator module that can see all four modules on its test classpath)
- [X] T005 Create the version-controlled architecture exception/freeze list at `specs/architecture-migration/freeze-list.md` with columns: id, module, violating class/dependency, violated rule, rationale, baseline task, planned removal phase, status (open/removed)
- [X] T005a Record the **G1 deviation** (from `/speckit.analyze`) in `specs/architecture-migration/freeze-list.md` as a documentation-only entry: FR-010's AMQP publisher/listener clause is out of scope for this migration because no AMQP infrastructure exists anywhere in the repository today and this migration introduces no new business capabilities; cross-reference the deviation note added next to FR-010 in `spec.md`. Status = "accepted deviation, not a violation", removal phase = "N/A — revisit when AMQP is first introduced"
- [X] T006 Run `./mvnw clean verify` to confirm the four-module reactor (with the two new, still-empty modules) builds successfully

**Checkpoint**: Reactor has four modules and freeze-list tooling in place; no production behavior has changed yet.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish the automated architecture-enforcement baseline and the shared domain building blocks that every catalog area and every user story depends on.

**⚠️ CRITICAL**: No catalog-area migration work (Phase 4/US2) may begin until this phase is complete.

- [X] T007 Add a `ModuleBoundaryArchitectureTest` ArchUnit test in `torg-codex/src/test/java/de/paladinsinn/torg/codex/architecture/` asserting inward-only dependencies: `torg-codex` and `torg-codex-data` may depend on `torg-codex-application` and `torg-codex-domain`; `torg-codex-application` may depend only on `torg-codex-domain`; `torg-codex-domain` depends on no other reactor module
- [X] T008 [P] Add a `DomainPurityArchitectureTest` ArchUnit test asserting no class in `torg-codex-domain` imports `org.springframework..`, `jakarta.persistence..`, or `org.hibernate..`
- [X] T009 [P] Add an `ApplicationPurityArchitectureTest` ArchUnit test asserting no class in `torg-codex-application` carries `@Transactional` (or any Spring transaction annotation) and imports no JPA/Hibernate type
- [X] T010 [P] Add an `AdapterConventionArchitectureTest` ArchUnit test asserting REST controllers live under `api.controller` (inbound), and persistence/HTTP/event adapters live under `data.adapter.out` (outbound)
- [X] T011 Implement a freeze-list loader used by the ArchUnit tests from T007–T010 so listed violations are tolerated and any unlisted violation fails the build; record the current transitional `torg-codex-data/.../data/application/{port/in,port/out,service}` classes (`CatalogQuery`, `CatalogReferenceQuery`, `CatalogPersistencePort`, `CatalogReferencePersistencePort`, `CatalogQueryService`, `CatalogReferenceQueryService`) as baseline freeze-list entries in `specs/architecture-migration/freeze-list.md`, with removal phase = "Phase 4a: Ports relocation"
- [X] T012 Create the framework-independent `DomainEvent` representation in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/event/DomainEvent.java`
- [X] T013 Create the `DomainEventPublisher` driven port in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/event/DomainEventPublisher.java`
- [X] T014 [P] Create shared domain value objects used across multiple catalog areas in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/`: `ClearanceLevel` (enum), `PublicationReference`, `CosmReference`, `DifficultyNumber`, `VehicleWeapon`, with Lombok-generated constructors/getters/equals/hashCode/toString and no Bean Validation evaluated at construction
- [X] T015 Run `./mvnw clean verify` confirming the ArchUnit baseline (with freeze-list exceptions honored), the event port, and the shared value objects all compile and pass

**Checkpoint**: Foundation ready — architecture enforcement, freeze list, domain events, and shared value objects exist; catalog-area migration (Phase 4) and compatibility-harness work (Phase 3) can now proceed.

---

## Phase 3: User Story 1 - Uninterrupted Service for API Consumers (Priority: P1) 🎯 Safety Net

**Goal**: Establish the recorded baseline and automated replay/comparison harness for every currently published REST endpoint, security/censorship outcome, and persisted record, so every later migration task can be checked for regressions.

**Independent Test**: Run the fixture-capture and replay harness against the current (unmigrated) application and confirm 100% self-consistent pass results, independent of any US2/US3 work having started.

- [X] T016 [US1] Create the characterization fixture directory structure and recording/comparison tooling (WireMock-based DriveThruRPG stub, JSON fixture format for status/headers/body/ordering) under `torg-codex/src/test/resources/characterization/` per `contracts/rest-compatibility.md`
- [X] T017 [P] [US1] Capture baseline REST characterization fixtures (list, detail, not-found, invalid UUID, full role/censorship matrix) for **Articles** in `torg-codex/src/test/resources/characterization/articles/`
- [X] T018 [P] [US1] Capture baseline REST characterization fixtures for **Cosms** in `torg-codex/src/test/resources/characterization/cosms/`
- [X] T019 [P] [US1] Capture baseline REST characterization fixtures (incl. `cosm` filter variants) for **Items** in `torg-codex/src/test/resources/characterization/items/`
- [X] T020 [P] [US1] Capture baseline REST characterization fixtures for **Miracles** in `torg-codex/src/test/resources/characterization/miracles/`
- [X] T021 [P] [US1] Capture baseline REST characterization fixtures (incl. `cosm` filter variants) for **Miracle Lists** in `torg-codex/src/test/resources/characterization/miracle-lists/`
- [X] T022 [P] [US1] Capture baseline REST characterization fixtures (incl. `cosm` filter variants) for **Perks** in `torg-codex/src/test/resources/characterization/perks/`
- [X] T023 [P] [US1] Capture baseline REST characterization fixtures for **Perk Groups** in `torg-codex/src/test/resources/characterization/perk-groups/`
- [X] T024 [P] [US1] Capture baseline REST characterization fixtures for **Powers** in `torg-codex/src/test/resources/characterization/powers/`
- [X] T025 [P] [US1] Capture baseline REST characterization fixtures (incl. `cosm` filter variants) for **Power Lists** in `torg-codex/src/test/resources/characterization/power-lists/`
- [X] T026 [P] [US1] Capture baseline REST characterization fixtures for **Publications** in `torg-codex/src/test/resources/characterization/publications/`
- [X] T027 [P] [US1] Capture baseline REST characterization fixtures for **Races** in `torg-codex/src/test/resources/characterization/races/`
- [X] T028 [P] [US1] Capture baseline REST characterization fixtures (incl. `cosm` filter variants) for **Shards** in `torg-codex/src/test/resources/characterization/shards/`
- [X] T029 [P] [US1] Capture baseline REST characterization fixtures for **Spells** in `torg-codex/src/test/resources/characterization/spells/`
- [X] T030 [P] [US1] Capture baseline REST characterization fixtures (incl. `cosm` filter variants) for **Spell Lists** in `torg-codex/src/test/resources/characterization/spell-lists/`
- [X] T031 [P] [US1] Capture baseline REST characterization fixtures for **Tags** in `torg-codex/src/test/resources/characterization/tags/`
- [X] T032 [P] [US1] Capture baseline REST characterization fixtures (incl. `cosm` filter variants) for **Threats** in `torg-codex/src/test/resources/characterization/threats/`
- [X] T033 [P] [US1] Capture baseline REST characterization fixtures (incl. `cosm` filter variants) for **Vehicles** in `torg-codex/src/test/resources/characterization/vehicles/`
- [X] T034 [US1] Implement the automated fixture-replay/comparison test suite (`CharacterizationReplayTest`, comparing status, headers incl. IANA PEN 33132 media-type version, body, and collection/map ordering) under Testcontainers/Failsafe in `torg-codex/src/test/java/de/paladinsinn/torg/codex/characterization/`, covering all 17 catalog families from T017–T033
- [X] T035 [US1] Implement a Liquibase changelog diff-guard test verifying no changeset is added, modified, or removed relative to the pre-migration baseline, in `torg-codex-data/src/test/java/de/paladinsinn/torg/codex/data/LiquibaseChangelogGuardTest.java`
- [X] T036 [US1] Wire the characterization replay suite (T034) and the Liquibase diff guard (T035) into the Failsafe/Surefire phases exercised by `./mvnw clean verify`
- [X] T037 [US1] Run `./mvnw clean verify` and the full characterization replay against the current, unmigrated application; confirm 100% pass — this proves the US1 safety net works standalone before any Phase 4 (US2) work begins

**Checkpoint**: The compatibility safety net is in place and self-verified. Every subsequent migration task in Phase 4/5 must keep this suite green.

---

## Phase 4: User Story 2 - Safely Evolvable Business Logic (Priority: P2)

**Goal**: Perform the actual horizontal-by-layer hexagonal migration — ports/interfaces, then domain models, then outbound adapters, then inbound adapters, then transaction boundaries and event bridging — one catalog area at a time, keeping the reactor buildable and the Phase 3 characterization suite green throughout.

**Independent Test**: Implement one representative business-rule change using only `torg-codex-domain`/`torg-codex-application` classes for a migrated area, and confirm it is exercised through the existing driving port without editing any adapter class.

**Dependency**: Requires Phase 2 (Foundational) complete. Each area's Phase 4 tasks depend on that area's Phase 3 (US1) fixtures already being captured (T017–T033), so the fixture-replay suite (T034) can validate every step.

### Phase 4a: Ports/use-case interfaces relocation

- [X] T038 [US2] Move `CatalogQuery<T>` and `CatalogReferenceQuery<T>` (driving ports) from `torg-codex-data/src/main/java/.../data/application/port/in/` to `torg-codex-application/src/main/java/de/paladinsinn/torg/codex/application/port/in/`, keeping them generic over a domain type parameter only (no JPA/DTO import)
- [X] T039 [US2] Move `CatalogPersistencePort<T>` and `CatalogReferencePersistencePort<T>` (driven ports) from `torg-codex-data/src/main/java/.../data/application/port/out/` to `torg-codex-application/src/main/java/de/paladinsinn/torg/codex/application/port/out/`
- [X] T040 [US2] Move `CatalogQueryService<T>` and `CatalogReferenceQueryService<T>` (framework-free use-case implementations) from `torg-codex-data/src/main/java/.../data/application/service/` to `torg-codex-application/src/main/java/de/paladinsinn/torg/codex/application/service/`
- [X] T041 [US2] Delete the now-empty `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/application/` package and remove its corresponding entries from `specs/architecture-migration/freeze-list.md` (added in T011)
- [X] T042 [US2] Fix all import references in `torg-codex-data` and `torg-codex` to the new `torg-codex-application` port/service packages (no behavior change) and run `./mvnw clean verify` plus the Phase 3 characterization replay to confirm zero regression

**Checkpoint**: Ports and use-case services now live in the correct module for every catalog area.

### Phase 4b: Domain model extraction (per catalog area)

- [X] T043 [P] [US2] Create the framework-independent `Article` domain model in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/Article.java` per data-model.md, using Lombok constructors/getters/equals/hashCode/toString with no Bean Validation evaluated at construction
- [X] T044 [P] [US2] Create the framework-independent `Cosm` domain model (axioms map, `text`, `worldLaws`) in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/Cosm.java`
- [X] T045 [P] [US2] Create the framework-independent `Item` domain model (type, technology/magic axioms, price, bonus, ammo, range, features, additional features, `text`) in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/Item.java`
- [X] T046 [P] [US2] Create the framework-independent `Miracle` domain model (axiom, casting time, difficulty number, range, duration, required skills, `text`) in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/Miracle.java`
- [X] T047 [P] [US2] Create the framework-independent `MiracleList` domain model (`unlockingPerk`, miracle UUIDs, `text`, notes, disable condition) in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/MiracleList.java`
- [X] T048 [P] [US2] Create the framework-independent `Perk` domain model (contradiction flag, group, prerequisites, `text`) in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/Perk.java`
- [X] T049 [P] [US2] Create the framework-independent `PerkGroup` domain model (`text`, infos) in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/PerkGroup.java`
- [X] T050 [P] [US2] Create the framework-independent `Power` domain model (axiom, casting time, difficulty number, range, duration, required skills, `text`, enhancements, limitations) in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/Power.java`
- [X] T051 [P] [US2] Create the framework-independent `PowerList` domain model (`unlockingPerk`, power UUIDs, `text`, notes, disable condition) in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/PowerList.java`
- [X] T052 [P] [US2] Create the framework-independent `Publication` domain model (`id`, `codexId`, `name`, `primaryProductId`, optional `thirdParty`, product-ID set) in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/Publication.java`, preserving cover-URL derivation from `primaryProductId`
- [X] T053 [P] [US2] Create the framework-independent `Race` domain model (major flag, attribute-limit map, abilities, `text`, perk text) in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/Race.java`
- [X] T054 [P] [US2] Create the framework-independent `Shard` domain model (possibilities, tapping difficulty, purpose, `text`, powers, restrictions) in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/Shard.java`
- [X] T055 [P] [US2] Create the framework-independent `Spell` domain model (axiom, casting time, difficulty number, range, duration, required skills, `text`) in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/Spell.java`
- [X] T056 [P] [US2] Create the framework-independent `SpellList` domain model (`unlockingPerk`, spell UUIDs, `text`, notes, disable condition) in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/SpellList.java`
- [X] T057 [P] [US2] Create the framework-independent `Tag` domain model (optional `parentId` self-reference) in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/Tag.java`
- [X] T058 [P] [US2] Create the framework-independent `Threat` domain model (unique flag, subtitle, quote, `text`, attributes, skills, movement, toughness/shock/wounds, equipment, perks, possibilities, special abilities) in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/Threat.java`
- [X] T059 [P] [US2] Create the framework-independent `Vehicle` domain model (type, technology axiom, unique flag, speed values/modifier, size, passengers, maneuver rating, wounds, toughness, price, weaponry via `VehicleWeapon`, `text`) in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/Vehicle.java`
- [X] T060 [US2] Create post-construction validators/factory methods for every domain model that requires validation (Bean Validation invoked explicitly outside constructors) in `torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/validation/`
- [X] T061 [US2] Run `./mvnw clean verify`, confirm `torg-codex-domain` still compiles with zero Spring/JPA/reactor-module dependencies, and confirm the T008 `DomainPurityArchitectureTest` passes for all 17 new domain models and their validators

**Checkpoint**: Every catalog area has a distinct, framework-independent domain model.

### Phase 4c: Outbound adapters — MapStruct persistence mapping (per catalog area)

- [ ] T062 [P] [US2] Create `ArticleEntityMapper` (MapStruct, JPA entity ↔ domain) in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/mapper/` and `JpaArticlePersistenceAdapter` implementing `CatalogPersistencePort<Article>` (or reference variant) delegating to `ArticleRepository`, in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/adapter/out/persistence/`
- [ ] T063 [P] [US2] Create `CosmEntityMapper` and `JpaCosmPersistenceAdapter` implementing `CatalogPersistencePort<Cosm>` delegating to `CosmRepository`, in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/{mapper,adapter/out/persistence}/`
- [ ] T064 [P] [US2] Create `ItemEntityMapper` and `JpaItemPersistenceAdapter` implementing `CatalogPersistencePort<Item>` (incl. `findByCosm`) delegating to `ItemRepository`, in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/{mapper,adapter/out/persistence}/`
- [ ] T065 [P] [US2] Create `MiracleEntityMapper` and `JpaMiraclePersistenceAdapter` implementing `CatalogPersistencePort<Miracle>` delegating to `MiracleRepository`, in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/{mapper,adapter/out/persistence}/`
- [ ] T066 [P] [US2] Create `MiracleListEntityMapper` and `JpaMiracleListPersistenceAdapter` implementing `CatalogPersistencePort<MiracleList>` (incl. `findByCosm`) delegating to `MiracleListRepository`, in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/{mapper,adapter/out/persistence}/`
- [ ] T067 [P] [US2] Create `PerkEntityMapper` and `JpaPerkPersistenceAdapter` implementing `CatalogPersistencePort<Perk>` (incl. `findByCosm`) delegating to `PerkRepository`, in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/{mapper,adapter/out/persistence}/`
- [ ] T068 [P] [US2] Create `PerkGroupEntityMapper` and `JpaPerkGroupPersistenceAdapter` implementing `CatalogPersistencePort<PerkGroup>` delegating to `PerkGroupRepository`, in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/{mapper,adapter/out/persistence}/`
- [ ] T069 [P] [US2] Create `PowerEntityMapper` and `JpaPowerPersistenceAdapter` implementing `CatalogPersistencePort<Power>` delegating to `PowerRepository`, in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/{mapper,adapter/out/persistence}/`
- [ ] T070 [P] [US2] Create `PowerListEntityMapper` and `JpaPowerListPersistenceAdapter` implementing `CatalogPersistencePort<PowerList>` (incl. `findByCosm`) delegating to `PowerListRepository`, in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/{mapper,adapter/out/persistence}/`
- [ ] T071 [P] [US2] Create `PublicationEntityMapper` and `JpaPublicationPersistenceAdapter` implementing `CatalogPersistencePort<Publication>` delegating to `PublicationRepository`, in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/{mapper,adapter/out/persistence}/`
- [ ] T072 [P] [US2] Create `RaceEntityMapper` and `JpaRacePersistenceAdapter` implementing `CatalogPersistencePort<Race>` delegating to `RaceRepository`, in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/{mapper,adapter/out/persistence}/`
- [ ] T073 [P] [US2] Create `ShardEntityMapper` and `JpaShardPersistenceAdapter` implementing `CatalogPersistencePort<Shard>` (incl. `findByCosm`) delegating to `ShardRepository`, in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/{mapper,adapter/out/persistence}/`
- [ ] T074 [P] [US2] Create `SpellEntityMapper` and `JpaSpellPersistenceAdapter` implementing `CatalogPersistencePort<Spell>` delegating to `SpellRepository`, in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/{mapper,adapter/out/persistence}/`
- [ ] T075 [P] [US2] Create `SpellListEntityMapper` and `JpaSpellListPersistenceAdapter` implementing `CatalogPersistencePort<SpellList>` (incl. `findByCosm`) delegating to `SpellListRepository`, in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/{mapper,adapter/out/persistence}/`
- [ ] T076 [P] [US2] Create `TagEntityMapper` and `JpaTagPersistenceAdapter` implementing `CatalogPersistencePort<Tag>` delegating to `TagRepository`, in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/{mapper,adapter/out/persistence}/`
- [ ] T077 [P] [US2] Create `ThreatEntityMapper` and `JpaThreatPersistenceAdapter` implementing `CatalogPersistencePort<Threat>` (incl. `findByCosm`) delegating to `ThreatRepository`, in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/{mapper,adapter/out/persistence}/`
- [ ] T078 [P] [US2] Create `VehicleEntityMapper` and `JpaVehiclePersistenceAdapter` implementing `CatalogPersistencePort<Vehicle>` (incl. `findByCosm`) delegating to `VehicleRepository`, in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/{mapper,adapter/out/persistence}/`
- [ ] T079 [US2] Implement a `DriveThruRpgProductPort` driven port (domain-only request/result types) and a `DriveThruRpgProductAdapter` in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/adapter/out/http/`, replacing direct client usage, verified against the existing WireMock stubs
- [ ] T080 [US2] Implement the `SpringDomainEventPublisherAdapter` bridging the `DomainEventPublisher` port (T013) to Spring's `ApplicationEventPublisher`, in `torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/adapter/out/event/`, preserving any existing listener-observed payload/delivery semantics
- [ ] T080a [US2] **(G2 prerequisite)** Build the Testcontainers-backed persistence-equivalence test harness in `torg-codex-data/src/test/java/de/paladinsinn/torg/codex/data/equivalence/`: a `PersistenceEquivalenceHarness` that, per catalog area, (1) writes/reads a fixture row through the pre-migration raw-JPA-entity path, (2) writes/reads the same fixture through the post-migration domain-model + JPA-entity + MapStruct-mapper path against the same Testcontainers database, and (3) asserts field-by-field (or normalized-snapshot) equivalence between the two round-trips, failing with a diff on any mismatch
- [ ] T080b [US2] **(G2 prerequisite)** Implement `PersistenceEquivalenceTest` covering all 17 catalog areas using the T080a harness, in `torg-codex-data/src/test/java/de/paladinsinn/torg/codex/data/equivalence/`, and confirm it runs successfully via `./mvnw -pl torg-codex-data clean verify` before it is relied upon by T081
- [ ] T081 [US2] Run `./mvnw clean verify`, the Testcontainers persistence read/write equivalence tests (T080a/T080b), the WireMock DriveThruRPG tests, and the Phase 3 characterization replay for all 17 areas; confirm zero regression after adding the outbound adapters

**Checkpoint**: Every catalog area, plus DriveThruRPG and domain events, has an outbound adapter implementing the relocated driven ports via MapStruct, with no observable behavior change.

### Phase 4d: Inbound adapter rewiring — composition root, controllers, DTO mappers (per catalog area)

- [ ] T082 [US2] Update the `articleCatalogQuery` bean in `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/configuration/CatalogQueryConfiguration.java` to type `CatalogQuery`/`CatalogPersistencePort` against the `Article` domain model and wire `JpaArticlePersistenceAdapter` instead of the raw JPA entity
- [ ] T083 [US2] Update the `cosmCatalogQuery` bean in `CatalogQueryConfiguration.java` to type against the `Cosm` domain model and wire `JpaCosmPersistenceAdapter`
- [ ] T084 [US2] Update the `itemCatalogQuery` bean in `CatalogQueryConfiguration.java` to type against the `Item` domain model and wire `JpaItemPersistenceAdapter`
- [ ] T085 [US2] Update the `miracleCatalogQuery` bean in `CatalogQueryConfiguration.java` to type against the `Miracle` domain model and wire `JpaMiraclePersistenceAdapter`
- [ ] T086 [US2] Update the miracle-list catalog query bean in `CatalogQueryConfiguration.java` to type against the `MiracleList` domain model and wire `JpaMiracleListPersistenceAdapter`
- [ ] T087 [US2] Update the perk catalog query bean in `CatalogQueryConfiguration.java` to type against the `Perk` domain model and wire `JpaPerkPersistenceAdapter`
- [ ] T088 [US2] Update the perk-group catalog query bean in `CatalogQueryConfiguration.java` to type against the `PerkGroup` domain model and wire `JpaPerkGroupPersistenceAdapter`
- [ ] T089 [US2] Update the power catalog query bean in `CatalogQueryConfiguration.java` to type against the `Power` domain model and wire `JpaPowerPersistenceAdapter`
- [ ] T090 [US2] Update the power-list catalog query bean in `CatalogQueryConfiguration.java` to type against the `PowerList` domain model and wire `JpaPowerListPersistenceAdapter`
- [ ] T091 [US2] Update the publication catalog query bean in `CatalogQueryConfiguration.java` to type against the `Publication` domain model and wire `JpaPublicationPersistenceAdapter`
- [ ] T092 [US2] Update the race catalog query bean in `CatalogQueryConfiguration.java` to type against the `Race` domain model and wire `JpaRacePersistenceAdapter`
- [ ] T093 [US2] Update the shard catalog query bean in `CatalogQueryConfiguration.java` to type against the `Shard` domain model and wire `JpaShardPersistenceAdapter`
- [ ] T094 [US2] Update the spell catalog query bean in `CatalogQueryConfiguration.java` to type against the `Spell` domain model and wire `JpaSpellPersistenceAdapter`
- [ ] T095 [US2] Update the spell-list catalog query bean in `CatalogQueryConfiguration.java` to type against the `SpellList` domain model and wire `JpaSpellListPersistenceAdapter`
- [ ] T096 [US2] Update the tag catalog query bean in `CatalogQueryConfiguration.java` to type against the `Tag` domain model and wire `JpaTagPersistenceAdapter`
- [ ] T097 [US2] Update the threat catalog query bean in `CatalogQueryConfiguration.java` to type against the `Threat` domain model and wire `JpaThreatPersistenceAdapter`
- [ ] T098 [US2] Update the vehicle catalog query bean in `CatalogQueryConfiguration.java` to type against the `Vehicle` domain model and wire `JpaVehiclePersistenceAdapter`

> Note: T082–T098 all edit the same file (`CatalogQueryConfiguration.java`) and its reference-query counterpart, so they MUST be done sequentially, each followed by a build/characterization check for that area, not in parallel.

- [ ] T099 [P] [US2] Update `ArticleController` and `ArticleMapper` in `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/{controller,mapper}/` to consume `CatalogQuery<Article>` and map the domain model to the existing `ArticleSummaryDto`/`ArticleDetailDto` via MapStruct, preserving byte-for-byte JSON output
- [ ] T100 [P] [US2] Update `CosmController` and `CosmMapper` to consume `CatalogQuery<Cosm>` and map to `CosmSummaryDto`/`CosmDetailDto`
- [ ] T101 [P] [US2] Update `ItemController` and `ItemMapper` to consume `CatalogQuery<Item>` and map to `ItemSummaryDto`/`ItemDetailDto`, preserving the `cosm` query parameter behavior
- [ ] T102 [P] [US2] Update `MiracleController` and `MiracleMapper` to consume `CatalogQuery<Miracle>` and map to `MiracleSummaryDto`/`MiracleDetailDto`
- [ ] T103 [P] [US2] Update `MiracleListController` and `MiracleListMapper` to consume `CatalogQuery<MiracleList>` and map to `MiracleListSummaryDto`/`MiracleListDetailDto`, preserving the `cosm` query parameter behavior
- [ ] T104 [P] [US2] Update `PerkController` and `PerkMapper` to consume `CatalogQuery<Perk>` and map to `PerkSummaryDto`/`PerkDetailDto`, preserving the `cosm` query parameter behavior
- [ ] T105 [P] [US2] Update `PerkGroupController` and `PerkGroupMapper` to consume `CatalogQuery<PerkGroup>` and map to `PerkGroupSummaryDto`/`PerkGroupDetailDto`
- [ ] T106 [P] [US2] Update `PowerController` and `PowerMapper` to consume `CatalogQuery<Power>` and map to `PowerSummaryDto`/`PowerDetailDto`
- [ ] T107 [P] [US2] Update `PowerListController` and `PowerListMapper` to consume `CatalogQuery<PowerList>` and map to `PowerListSummaryDto`/`PowerListDetailDto`, preserving the `cosm` query parameter behavior
- [ ] T108 [P] [US2] Update `PublicationController` and `PublicationMapper` to consume `CatalogQuery<Publication>` and map to `PublicationSummaryDto`/`PublicationDetailDto`
- [ ] T109 [P] [US2] Update `RaceController` and `RaceMapper` to consume `CatalogQuery<Race>` and map to `RaceSummaryDto`/`RaceDetailDto`
- [ ] T110 [P] [US2] Update `ShardController` and `ShardMapper` to consume `CatalogQuery<Shard>` and map to `ShardSummaryDto`/`ShardDetailDto`, preserving the `cosm` query parameter behavior
- [ ] T111 [P] [US2] Update `SpellController` and `SpellMapper` to consume `CatalogQuery<Spell>` and map to `SpellSummaryDto`/`SpellDetailDto`
- [ ] T112 [P] [US2] Update `SpellListController` and `SpellListMapper` to consume `CatalogQuery<SpellList>` and map to `SpellListSummaryDto`/`SpellListDetailDto`, preserving the `cosm` query parameter behavior
- [ ] T113 [P] [US2] Update `TagController` and `TagMapper` to consume `CatalogQuery<Tag>` and map to `TagSummaryDto`/`TagDetailDto`
- [ ] T114 [P] [US2] Update `ThreatController` and `ThreatMapper` to consume `CatalogQuery<Threat>` and map to `ThreatSummaryDto`/`ThreatDetailDto`, preserving the `cosm` query parameter behavior
- [ ] T115 [P] [US2] Update `VehicleController` and `VehicleMapper` to consume `CatalogQuery<Vehicle>` and map to `VehicleSummaryDto`/`VehicleDetailDto`, preserving the `cosm` query parameter behavior
- [ ] T116 [US2] Run `./mvnw clean verify` and the full Phase 3 characterization replay for all 17 areas; confirm zero regression after inbound rewiring, including RBAC/ABAC/UMA outcomes and DRM-censored fields

**Checkpoint**: Every catalog area's controller and DTO mapper now depend only on the driving port and a domain model; no controller references a JPA entity.

### Phase 4e: Transaction boundary relocation

- [ ] T117 [US2] **(U1 split — see T117a–T117q below)** Relocate `@Transactional` (or equivalent Spring transaction demarcation) to the inbound-adapter/composition boundary for every catalog area's read operations, one catalog area at a time (not as one big-bang change), ensuring `torg-codex-application` services and `torg-codex-domain` classes carry none by the end
- [ ] T117a [US2] Relocate `@Transactional` to the inbound-adapter boundary for **Article** read operations (thin façade/controller-method demarcation in `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/`); run `./mvnw clean verify` plus the Article Phase 3 characterization fixtures before moving to the next area
- [ ] T117b [US2] Relocate `@Transactional` to the inbound-adapter boundary for **Cosm** read operations; run `./mvnw clean verify` plus the Cosm Phase 3 characterization fixtures before moving to the next area
- [ ] T117c [US2] Relocate `@Transactional` to the inbound-adapter boundary for **Item** read operations; run `./mvnw clean verify` plus the Item Phase 3 characterization fixtures before moving to the next area
- [ ] T117d [US2] Relocate `@Transactional` to the inbound-adapter boundary for **Miracle** read operations; run `./mvnw clean verify` plus the Miracle Phase 3 characterization fixtures before moving to the next area
- [ ] T117e [US2] Relocate `@Transactional` to the inbound-adapter boundary for **MiracleList** read operations; run `./mvnw clean verify` plus the Miracle List Phase 3 characterization fixtures before moving to the next area
- [ ] T117f [US2] Relocate `@Transactional` to the inbound-adapter boundary for **Perk** read operations; run `./mvnw clean verify` plus the Perk Phase 3 characterization fixtures before moving to the next area
- [ ] T117g [US2] Relocate `@Transactional` to the inbound-adapter boundary for **PerkGroup** read operations; run `./mvnw clean verify` plus the Perk Group Phase 3 characterization fixtures before moving to the next area
- [ ] T117h [US2] Relocate `@Transactional` to the inbound-adapter boundary for **Power** read operations; run `./mvnw clean verify` plus the Power Phase 3 characterization fixtures before moving to the next area
- [ ] T117i [US2] Relocate `@Transactional` to the inbound-adapter boundary for **PowerList** read operations; run `./mvnw clean verify` plus the Power List Phase 3 characterization fixtures before moving to the next area
- [ ] T117j [US2] Relocate `@Transactional` to the inbound-adapter boundary for **Publication** read operations; run `./mvnw clean verify` plus the Publication Phase 3 characterization fixtures before moving to the next area
- [ ] T117k [US2] Relocate `@Transactional` to the inbound-adapter boundary for **Race** read operations; run `./mvnw clean verify` plus the Race Phase 3 characterization fixtures before moving to the next area
- [ ] T117l [US2] Relocate `@Transactional` to the inbound-adapter boundary for **Shard** read operations; run `./mvnw clean verify` plus the Shard Phase 3 characterization fixtures before moving to the next area
- [ ] T117m [US2] Relocate `@Transactional` to the inbound-adapter boundary for **Spell** read operations; run `./mvnw clean verify` plus the Spell Phase 3 characterization fixtures before moving to the next area
- [ ] T117n [US2] Relocate `@Transactional` to the inbound-adapter boundary for **SpellList** read operations; run `./mvnw clean verify` plus the Spell List Phase 3 characterization fixtures before moving to the next area
- [ ] T117o [US2] Relocate `@Transactional` to the inbound-adapter boundary for **Tag** read operations; run `./mvnw clean verify` plus the Tag Phase 3 characterization fixtures before moving to the next area
- [ ] T117p [US2] Relocate `@Transactional` to the inbound-adapter boundary for **Threat** read operations; run `./mvnw clean verify` plus the Threat Phase 3 characterization fixtures before moving to the next area
- [ ] T117q [US2] Relocate `@Transactional` to the inbound-adapter boundary for **Vehicle** read operations; run `./mvnw clean verify` plus the Vehicle Phase 3 characterization fixtures before moving to the next area, confirming all 17 areas now carry zero `@Transactional` in `torg-codex-application`/`torg-codex-domain`
- [ ] T118 [P] [US2] Add a `TransactionBoundaryArchitectureTest` ArchUnit test asserting zero `@Transactional` usage anywhere in `torg-codex-application` and `torg-codex-domain`
- [ ] T119 [US2] Run `./mvnw clean verify` and the Phase 3 characterization replay, confirming transaction relocation preserves existing read/write behavior for all 17 areas

**Checkpoint**: User Story 2 complete — every catalog area is fully migrated through ports, domain models, outbound adapters, inbound adapters, transactions, and events, with zero characterization regressions.

---

## Phase 5: User Story 3 - Continuous Automated Architecture Verification (Priority: P3)

**Goal**: Harden and prove the automated enforcement mechanism so any newly introduced, unlisted boundary violation fails the build without manual review, formalizing the separation of concerns built in Phase 4.

**Independent Test**: Introduce a deliberate boundary violation (e.g., a Spring import in a domain class) on a local branch, confirm the build fails identifying the specific rule, then confirm it passes again once removed.

- [ ] T120 [US3] Add a `FreezeListEnforcementArchitectureTest` verifying the freeze-list loader (T011) tolerates only listed violations and fails the build for any unlisted one, in `torg-codex/src/test/java/de/paladinsinn/torg/codex/architecture/`
- [ ] T121 [US3] Perform the deliberate-violation drill: add a temporary `org.springframework` import to a `torg-codex-domain` class on a scratch commit, run `./mvnw clean verify`, confirm it fails and names the specific rule violated, then revert the change; record the drill outcome as a verification note in `specs/architecture-migration/quickstart.md`
- [ ] T122 [US3] Expand `AdapterConventionArchitectureTest` (T010) to cover the DriveThruRPG adapter (`data/adapter/out/http`), the domain-event bridge adapter (`data/adapter/out/event`), and all 17 REST controllers introduced/updated in Phase 4
- [ ] T123 [US3] Add a `FreezeListFormatTest` verifying every entry in `specs/architecture-migration/freeze-list.md` has id, module, violating class/dependency, violated rule, rationale, baseline task, removal phase, and status, failing the build if any required field is missing
- [ ] T124 [US3] Run `./mvnw clean verify`, confirming architecture tests execute in the standard Maven test lifecycle and cannot be skipped or disabled to pass a task

**Checkpoint**: Architecture enforcement is continuous, automated, and self-verified; the build fails on any regression without manual review.

---

## Phase 6: Polish & Cross-Cutting Concerns — Final Freeze-List Cleanup

**Purpose**: Remove every temporary exception, finalize documentation, and run the completion gate.

- [ ] T125 [P] Remove any remaining deprecated transitional classes/packages left over from the pre-migration `torg-codex-data/.../data/application` structure (if any remain beyond T041)
- [ ] T126 Resolve and remove every remaining entry in `specs/architecture-migration/freeze-list.md`, confirming the corresponding ArchUnit rule now passes without suppression for each one
- [ ] T127 Confirm `specs/architecture-migration/freeze-list.md` has zero open entries and update its status header to "Migration complete"
- [ ] T128 [P] Update `docs/modules/arc42/pages/04_solution_strategy.adoc` and `docs/modules/arc42/pages/02_architecture_constraints.adoc` (adding a building-block view page if none exists) to reflect the finalized four-module hexagonal structure, per FR-027
- [ ] T129 [P] Add a final validation note to `specs/architecture-migration/quickstart.md` recording that the completion validation (T130) passed
- [ ] T129a **(G2 prerequisite)** Extend the T080a/T080b persistence-equivalence harness into a full-database snapshot-comparison tool (`PersistedDataSnapshotComparisonTest`, in `torg-codex-data/src/test/java/de/paladinsinn/torg/codex/data/equivalence/`) that dumps a normalized snapshot (all rows, all 17 catalog tables) from a Testcontainers database seeded via the pre-migration path and compares it field-by-field against the same seed data round-tripped through the fully-migrated (post-Phase-4e) domain-model/adapter path, so T130 has a concrete snapshot-comparison test to run
- [ ] T130 Run `./mvnw clean verify`, the full Phase 3 characterization replay, the Liquibase diff guard, and the T129a persisted-data snapshot comparison as the completion gate for SC-001 through SC-006

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately.
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS Phase 3 and Phase 4.
- **User Story 1 (Phase 3)**: Depends on Foundational completion. Establishes the compatibility safety net that every Phase 4 task must keep green; can be staffed in parallel with early Phase 4a work, but each catalog area's Phase 4b–4d tasks depend on that area's Phase 3 fixtures already being captured.
- **User Story 2 (Phase 4)**: Depends on Foundational (Phase 2) and, per area, on that area's Phase 3 fixtures. Internally strictly ordered: 4a (ports) → 4b (domain models) → 4c (outbound adapters) → 4d (inbound adapters) → 4e (transactions/events), because each sub-phase's classes depend on the previous sub-phase's types compiling.
- **User Story 3 (Phase 5)**: The baseline enforcement exists from Phase 2; the hardening/drill tasks in Phase 5 depend on Phase 4 having introduced the real adapters/ports they harden against, so Phase 5 substantively runs after Phase 4.
- **Polish (Phase 6)**: Depends on Phase 4 and Phase 5 both being complete — freeze-list entries can only be closed once every violation they cover has been resolved by the corresponding migration or enforcement task.

### Within Phase 4 (User Story 2)

- Ports relocation (4a) before any domain-model/adapter work, since `CatalogQuery`/`CatalogPersistencePort` must exist in `torg-codex-application` before adapters can implement them against domain types.
- Domain models (4b) before outbound adapters (4c), since MapStruct mappers need both the JPA entity and the domain model to exist.
- Outbound adapters (4c) before inbound adapters (4d), since controllers/composition wiring need a working `CatalogPersistencePort` implementation to inject.
- Composition-root updates (T082–T098) are sequential (same file) and must land before their area's controller/mapper update (T099–T115) can compile against the domain-typed port bean.
- Transaction relocation (4e) after inbound rewiring (4d), since it moves demarcation onto the now-thin inbound boundary.

### Parallel Opportunities

- All Setup tasks marked [P] (T003, T004) can run in parallel once T001/T002 exist.
- All Foundational ArchUnit tasks marked [P] (T008, T009, T010) can run in parallel; T014's shared value objects can be split across contributors.
- All 17 per-area characterization fixture-capture tasks (T017–T033) can run fully in parallel.
- All 17 per-area domain-model tasks (T043–T059) can run fully in parallel.
- All 17 per-area outbound-adapter tasks (T062–T078) can run fully in parallel once 4a and 4b are done.
- All 17 per-area controller/mapper tasks (T099–T115) can run fully in parallel once that area's composition-root bean (T082–T098) is updated — but the composition-root updates themselves are sequential (same file).
- Phase 5 hardening tasks marked [P] can run in parallel with late Phase 4 work once the relevant adapters exist.
- Phase 6 documentation tasks marked [P] (T128, T129) can run in parallel; freeze-list closure (T126, T127) should be sequential per entry.

---

## Parallel Example: Domain model extraction (Phase 4b)

```bash
# Launch all 17 per-area domain-model tasks together once Phase 4a (ports relocation) is done:
Task: "Create the framework-independent Article domain model in torg-codex-domain/.../domain/model/Article.java"
Task: "Create the framework-independent Cosm domain model in torg-codex-domain/.../domain/model/Cosm.java"
Task: "Create the framework-independent Item domain model in torg-codex-domain/.../domain/model/Item.java"
# ... one per remaining catalog area (T046-T059)
```

## Parallel Example: Characterization fixture capture (Phase 3)

```bash
# Launch all 17 per-area fixture-capture tasks together once T016's tooling exists:
Task: "Capture baseline REST characterization fixtures for Articles"
Task: "Capture baseline REST characterization fixtures for Cosms"
Task: "Capture baseline REST characterization fixtures (incl. cosm filter variants) for Items"
# ... one per remaining catalog area (T020-T033)
```

---

## Implementation Strategy

### Safety Net First (User Story 1)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational (ArchUnit baseline, freeze list, domain events, shared value objects).
3. Complete Phase 3: User Story 1 — capture and wire the characterization/Liquibase safety net.
4. **STOP and VALIDATE**: Confirm the safety net passes against the current, unmigrated application.

### Incremental, Horizontal-by-Layer Delivery (User Story 2)

1. Complete Phase 4a (ports relocation) — one reactor-wide change, verified buildable.
2. Complete Phase 4b (domain models) for all 17 areas — can be parallelized across contributors.
3. Complete Phase 4c (outbound adapters/MapStruct) for all 17 areas.
4. Complete Phase 4d (inbound rewiring) for all 17 areas — composition-root changes sequential, controller/mapper changes parallel.
5. Complete Phase 4e (transaction relocation).
6. **STOP and VALIDATE** after every sub-phase and, ideally, after every area within 4b–4d: run `./mvnw clean verify` plus the Phase 3 characterization replay before starting the next task, per FR-015/FR-026.
7. If any single task risks leaving the reactor unbuildable, split it further and bridge the interim state with a new, rationale-backed freeze-list entry (per the spec's edge cases), never as a silent violation.

### Harden Enforcement (User Story 3)

1. Complete Phase 5 once the real ports/adapters from Phase 4 exist to harden against.
2. Run the deliberate-violation drill (T121) to prove the mechanism before declaring the migration durable.

### Final Cleanup

1. Complete Phase 6: resolve every freeze-list entry, update arc42 documentation, and run the full completion gate (T130).
2. The migration is only considered complete when the freeze list is empty and SC-001 through SC-006 all hold.

---

## Notes

- [P] tasks = different files, no dependencies.
- [Story] label maps task to specific user story (US1 = compatibility safety net, US2 = the hexagonal migration itself, US3 = automated enforcement) for traceability.
- Setup, Foundational, and Polish phases carry no [Story] label, per convention.
- No task in Phase 4 may be merged if it makes `./mvnw clean verify` fail or introduces an unlisted architecture violation (FR-023, FR-024, FR-026).
- No task may add, modify, or remove a Liquibase changeset (FR-018) — persistence identity and schema stay exactly as they are today.
- Every new, temporary deviation from target boundaries must be recorded in `specs/architecture-migration/freeze-list.md` with a rationale and planned removal phase before a task is considered complete (FR-020, FR-021).
- Commit after each task or logical group; stop at any checkpoint to validate the reactor and the characterization suite before continuing.
