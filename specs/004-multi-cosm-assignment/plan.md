# Implementation Plan: Multi-Cosm Assignment for Catalog Entries

**Branch**: `004-multi-cosm-assignment` | **Date**: 2026-09-13 (re-planned) | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-multi-cosm-assignment/spec.md`

**Scope**: three defect families — cosm references (62 rows), unlocking-perk references (5 rows),
prose/markup fields (97 rows). See the Scope table in [spec.md](./spec.md).

## Summary

A full scan of the shipped catalog data found stringified Python list literals
(`['core-earth', 'tharkold']`) stored in single-valued columns across three families of fields. Two
of them are reference fields whose entries consequently match no lookup; the third is prose that is
shown to readers verbatim.

Technical approach, per family:

1. **Cosm references** — replace the single-valued reference with a JPA `@ElementCollection` of slug
   strings in `torg_<entity>_cosms`, mirroring the existing `products` collection pattern exactly;
   switch the persistence filter to the established `MEMBER OF` JPQL idiom so driving and driven port
   signatures stay untouched. The externally visible `cosm` field keeps its shape by explicit user
   decision — one cosm behaves exactly as today, several fold into a comma-separated list of display
   names with a `null` id.
2. **Unlocking-perk references** — identical treatment in `torg_<entity>_unlocking_perks` for the
   three list types. The exposed `unlockingPerk` field stays a `String` and folds to sorted,
   comma-separated **slugs**; it is never resolved to display names.
3. **Prose fields** — repaired in place by a Liquibase `customChange` running after the data load, so
   that separately delivered proprietary data is covered too. Several statements become a Markdown
   bullet list in source order; exactly one statement becomes plain text; anything that is not
   entirely an encoded list is left byte-identical.

Both reference families are backfilled through ordered Liquibase Expand + Migrate + Contract
changesets in `data-fix-lists.yaml`, which splits encoded values generically and then drops the
legacy columns in the same release.

## Technical Context

**Language/Version**: Java 25 (Temurin 25 JDK)

**Primary Dependencies**: Spring Boot (Spring MVC, Spring Data JPA, Spring Security), Hibernate,
Liquibase, MapStruct, Lombok, JTE, flexmark-java

**Storage**: PostgreSQL, schema managed exclusively by Liquibase; application runs with
`ddl-auto: validate`

**Testing**: JUnit 5, AssertJ, Mockito (Surefire); Testcontainers PostgreSQL for `*IT` under
Failsafe; characterization fixture replay for API regression

**Target Platform**: Linux server, containerised Spring Boot application

**Project Type**: Four-module Maven reactor web service (`torg-codex-domain`,
`torg-codex-application`, `torg-codex-data`, `torg-codex`)

**Performance Goals**: Catalog list and detail responses stay perceptually instantaneous; the
added collection join must not introduce N+1 behaviour (eager fetch, mirroring `products`)

**Constraints**: Applied Liquibase changesets and shipped CSV data files are immutable; the bulk of
the game data is proprietary and git-ignored, so the migration must be data-agnostic; existing
externally visible behaviour must remain compatible; zero Checkstyle violations

**Scale/Scope**: 19 cosms; 8 catalog entity types affected by cosms, 3 by unlocking perks, 3 by
prose repair. Public free-tier data set: 62 encoded cosm rows across 20 distinct combinations with
at most 7 cosms per entry; 5 encoded unlocking-perk rows; 97 encoded prose values of which 45 hold a
single statement. Unknown additional volume in the proprietary data set.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Initial | Post-Design |
|-----------|------|---------|-------------|
| I. Hexagonal Architecture & Clean Ports | Domain stays free of Spring/JPA; separate persistence and domain models; MapStruct for all layer conversion; `torg-codex-data` persistence-only; boundaries enforced by architecture tests | PASS | PASS |
| II. Self-Contained Systems & Async Integration | No new inter-system coupling; no new messaging | PASS (n/a) | PASS (n/a) |
| III. Standardized REST APIs & OpenAPI | Classic REST retained; thin controllers; summary/detail DTOs; media-type versioning | PASS | PASS |
| IV. Zero-Downtime Evolutionary Migrations | Expand/Migrate/Contract split; `<ISO-date>-<sequence>-<phase>` naming; applied changesets never altered; `db-updater` executes | PASS | **DEVIATION** — see Complexity Tracking |
| V. Multi-Tier Security & DRM Censorship | Single censoring mechanism untouched; no repository, mapper or controller bypasses censoring | PASS | PASS |
| VI. Deterministic Markdown Pipeline | Markup pipeline and its rendering order untouched | PASS (n/a) | PASS — see gate notes |
| VII. Production Observability | No change to logging, metrics, or alerting surface | PASS (n/a) | PASS (n/a) |
| VIII. Test-First & Integration Verification | Automated coverage for the schema update and the bug fix; Testcontainers for `*IT`; git sign-off | PASS | PASS |
| ADR-001 Architecture Decision Records | Architecture-level change requires an accepted ADR | PASS — ADR-018 planned (research R7) | PASS |
| Four-Module Reactor ownership | Changes land in the correct module for each concern | PASS | PASS |
| Proprietary Fixtures | Code and tests must work against the public free-tier fixture set | PASS | PASS |

### Gate notes

- **Principle I**: The domain records change `String cosm` to `Set<String> cosms`, and the three list
  records additionally `String unlockingPerk` to `Set<String> unlockingPerks`, which introduces no
  framework dependency. The `@ElementCollection`s live only in `torg-codex-data`, as does the
  `customChange` class that repairs prose. MapStruct maps `Set<String>` to `Set<String>` without
  hand-written code, and mappers stay in adapter packages.
- **Principle III**: The response schema is unchanged for all three families, so ADR-008 requires no
  media-type version bump. Controllers are not touched at all — the `?cosm=` request parameter and
  the port signatures `findByCosm(String)` remain identical, confining the change to the persistence
  adapter. `findByUnlockingPerk` is repository-only and unreachable from any port (research R9), so
  no request contract changes there either.
- **Principle IV**: This release ships **Expand + Migrate + Contract in one deployment**. By explicit
  user decision (clarification 2026-09-12) the legacy `cosm` and `unlocking_perk` columns are dropped
  in the same release as the backfill, because no production installation depends on a rolling
  update. This is a deliberate deviation from the mandated three-deployment split and is recorded in
  Complexity Tracking below. Changeset naming still follows `<ISO-date>-<sequence>-<phase>`, the
  existing CSV `loadData` changesets stay untouched, and `data-fix-lists.yaml` runs after them.
  The contract changeset MUST run after all migrate
  changesets within the same changelog so a failed backfill aborts before any column is dropped
  (FR-015). The prose repair is a `customChange` rather than SQL, but a `customChange` is still a
  checksummed Liquibase changeset recorded in `DATABASECHANGELOG`, so ADR-012 is satisfied
  (research R10).
- **Principle V**: Product-ownership authorities, `ProductOwnershipResolver`, and
  `CurrentUserCensorFactory` are untouched. FR-012 makes it explicit that the number of assigned
  cosms must not affect visibility, and this is asserted by the existing censoring differential
  test. The prose repair runs at migration time, strictly before any censoring decision.
- **Principle VI**: The rendering order — conditional product blocks, entity references, raw HTML,
  game tokens, CommonMark — is not modified. Prose repair changes the *stored source* only, and only
  where the entire value is an encoded list. Because the emitted Markdown is a plain `- ` bullet list
  with no Torg markup constructs, it flows through the existing pipeline deterministically.
- **Principle VIII**: Coverage combines new unit tests for the presentation folding edge cases and
  for the encoded-list tokenizer (which the public data set only partially exercises), the
  Testcontainers-backed `LiquibaseImportIT` for the migration including an idempotency re-run, and
  recaptured characterization fixtures for API regression. See [quickstart.md](./quickstart.md).
- **Proprietary fixtures**: All three migrations derive their results from the stored values rather
  than enumerating the combinations visible in the public data (FR-027), so proprietary rows are
  converted too (research R4, R9, R11).

**Result**: One documented deviation (Principle IV) and one documented complexity (the first
`customChange`), both recorded with justification in Complexity Tracking. No other violations.

## Project Structure

### Documentation (this feature)

```text
specs/004-multi-cosm-assignment/
├── plan.md                             # This file
├── spec.md                             # Feature specification
├── research.md                         # Phase 0 output
├── data-model.md                       # Phase 1 output
├── quickstart.md                       # Phase 1 output
├── contracts/
│   ├── cosm-reference-api.md           # Phase 1 output — cosm field
│   └── list-and-prose-fields.md        # Phase 1 output — unlockingPerk + prose fields
├── checklists/
│   └── requirements.md                 # Specification quality checklist
└── tasks.md                            # Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```text
torg-codex-domain/src/main/java/de/paladinsinn/torg/codex/domain/model/
├── Item.java                           # String cosm -> Set<String> cosms
├── MiracleList.java                    # (same) + String unlockingPerk -> Set<String> unlockingPerks
├── Perk.java                           # (same)
├── PowerList.java                      # (same) + unlockingPerks
├── Shard.java                          # (same)
├── SpellList.java                      # (same) + unlockingPerks
├── Threat.java                         # (same)
└── Vehicle.java                        # (same)

torg-codex-application/src/main/java/de/paladinsinn/torg/codex/application/
├── port/in/CatalogQuery.java           # findByCosm(String) — unchanged
├── port/out/CatalogPersistencePort.java# findByCosm(String) — unchanged
└── service/CatalogQueryService.java    # unchanged

torg-codex-data/src/main/
├── java/de/paladinsinn/torg/codex/data/
│   ├── model/                          # 8 entities: add @ElementCollection Set<String> cosms;
│   │                                   #   3 list entities also Set<String> unlockingPerks
│   ├── repository/                     # 8 repositories: findByCosm via MEMBER OF;
│   │                                   #   findByCosmAndProduct on Perk and Threat;
│   │                                   #   3 list repositories: findByUnlockingPerk via MEMBER OF
│   ├── mapper/                         # MapStruct entity mappers: collections map by name
│   └── migration/
│       ├── EncodedListTokenizer.java   # NEW: parses an encoded list into statements (research R11)
│       └── ProseListToMarkdownChange.java # NEW: Liquibase CustomTaskChange for prose repair
└── resources/db/changelog/
    ├── data-fix-lists.yaml             # NEW: ordered expand, migrate, prose repair, contract
    └── db.changelog-master.yaml        # include the data fix after all imports

torg-codex/src/main/java/de/paladinsinn/torg/codex/api/
├── mapper/TorgMappingSupport.java      # toCosmRef(Set<String>) + unlocking-perk slug folding
└── mapper/*Mapper.java                 # 8 web mappers: pass collections to the folding helpers

torg-codex-data/src/test/
├── java/de/paladinsinn/torg/codex/data/migration/
│   ├── EncodedListTokenizerTest.java   # NEW: tokenizer edge cases (research R11 table)
│   └── ProseListToMarkdownChangeTest.java # NEW: folding + idempotency
└── resources/liquibase-changelog-manifest.sha256 # regenerate for the new + modified changelogs

torg-codex/src/test/
├── java/.../api/mapper/TorgMappingSupportTest.java  # NEW: folding edge cases, both families
└── resources/characterization/                      # recapture affected fixtures

docs/modules/arc42/pages/09_architecture_decisions/
├── 018_multi-valued-references-and-encoded-list-repair.adoc  # NEW ADR
├── _nav.adoc                           # register ADR-018
└── _include.adoc                       # register ADR-018
```

**Structure Decision**: The existing four-module Maven reactor is used unchanged. Each concern
lands in the module the constitution assigns to it: cardinality in the domain records, the
`@ElementCollection`s, repositories, Liquibase changesets and the prose-repair `customChange` in
`torg-codex-data`, and the presentation folding in the web adapter's MapStruct support class in
`torg-codex`. The application module needs no change at all, because the port signatures are
deliberately preserved — which is the clearest evidence that the hexagonal boundary is holding.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|--------------------------------------|
| **Principle IV** — the Contract phase (dropping the legacy `cosm` and `unlocking_perk` columns) ships in the **same deployment** as Expand + Migrate, instead of a separate later release. | Explicit user decision (clarification 2026-09-12, FR-014, FR-020): no backward compatibility is required. The system has no production installation that needs a rolling update, so a side-by-side window buys nothing. Keeping the columns would leave two overlapping representations of the same fact, which invites divergence and forces every future reader to know which one is authoritative. | Deferring the drop to a later release was rejected because it keeps dead, unread columns in eleven tables across an unbounded interval, and because the `ddl-auto: validate` schema check plus the domain records would have to keep carrying the legacy fields solely to satisfy a rollout scenario that cannot occur. The deviation is bounded: it applies to this feature only, Principle IV remains binding for all future schema changes, and FR-015 requires the contract changeset to run after the backfill so a failed migration aborts before any data is dropped. |
| **First `customChange` in the repository** — prose repair is implemented as a Java `CustomTaskChange` rather than a SQL changeset, with no existing precedent in this codebase. | The 97 affected values cannot be parsed safely in SQL: 12 contain `, ` inside a statement, 3 mix `'` and `"` delimiters, one embeds a literal `"` inside a single-quoted statement, and one contains a newline (research R10). Any regex or `split_part` approach corrupts data. A `customChange` gives a real tokenizer with unit tests while remaining a checksummed Liquibase changeset. | Editing the CSV files was rejected because the bulk of the data is proprietary and delivered separately, so CSV edits would fix only the public subset (clarification 2026-09-12). A pure-SQL migration was rejected on the measured evidence above. Repairing at read time in the markup pipeline was rejected because it hides broken data indefinitely and pays the cost on every request. |
