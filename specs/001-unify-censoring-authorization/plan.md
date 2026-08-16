# Implementation Plan: Unify Product-Ownership Censoring Authorization

**Branch**: `001-unify-censoring-authorization` | **Date**: 2026-08-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-unify-censoring-authorization/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command; its definition describes the execution workflow.

## Summary

The live REST API's censoring path (`CurrentUserCensorFactory`, wired into 15 of the 17
catalog controllers) derives product ownership **only** by unwrapping a DriveThruRPG-specific
`DriveThruUserDetails` principal via `DriveThruUserService.getCurrentUser()`. It never inspects
the generic `ROLE_<codex-id>` `GrantedAuthority` entries Spring Security already exposes on
`Authentication.getAuthorities()` — entries that `DriveThruUserDetails.getAuthorities()` itself
correctly emits, and that a second, fully unit-tested but never-wired class,
`SecuredMarkupService`, already reads correctly. Because of this, any principal type other than
`ApiKeyAuthenticationToken`/`DriveThruUserDetails` (e.g. the plain `User` principal Spring
Security's test support constructs, or a future OIDC/Keycloak `JwtAuthenticationToken`) is
silently treated as owning nothing, and the characterization test suite's "owner" auth variant
(`SOURCEBOOK_AYSLE_OWNER`, a plain test `User` with `ROLE_sourcebook-aysle`) never actually
exercised the gate — hence the byte-identical anonymous/owner fixtures across all 17 areas.

The technical approach: make ownership resolution **principal-type-agnostic** by reading
`ROLE_<codex-id>` authorities directly off the current `SecurityContextHolder` `Authentication`
(the same logic already proven correct and unit-tested in `SecuredMarkupService`), extract that
logic into one small, dedicated, unit-testable collaborator (`ProductOwnershipResolver`), make
`CurrentUserCensorFactory` — the one class already wired into every gated controller — delegate
to it, and delete the now-fully-redundant `SecuredMarkupService` (and its test, ported onto the
new resolver) so exactly one production component derives ownership for censoring. No REST
controller, MapStruct mapper, DTO, database schema, or markup-rendering-order change is
required; the fix is confined to the authorization/ownership-resolution layer plus new/repaired
tests that assert an actual anonymous-vs-owner difference for the Aysle cosm.

## Technical Context

**Language/Version**: Java 25 (`pom.xml` `<java.version>25</java.version>`; JDK 25 required to build/test — verified locally with Eclipse Temurin 25.0.3 via `./mvnw -v`)

**Primary Dependencies**: Spring Boot (Web MVC, Security, Data JPA, Actuator), Spring Security Test (`SecurityMockMvcRequestPostProcessors`), MapStruct (`componentModel = "spring"`), Lombok, flexmark-java + OWASP Java HTML Sanitizer (markup pipeline — untouched by this feature), Liquibase, ArchUnit, JUnit 5 / AssertJ / Mockito, Testcontainers (PostgreSQL, Failsafe `*IT`)

**Storage**: PostgreSQL via Liquibase-managed schema (Testcontainers in tests). **N/A for this feature** — FR-010 explicitly forbids any persisted-data-model or schema change; nothing here touches JPA entities or `torg-codex-data/src/main/resources/db`.

**Testing**: `./mvnw test` (unit: JUnit 5/Mockito/AssertJ, e.g. a resolver unit test replacing `SecuredMarkupServiceTest`); `./mvnw clean verify` (adds Testcontainers-backed `*IT`/`@SpringBootTest` + `MockMvc` integration tests under Failsafe, including the existing `CharacterizationReplayTest`/`CharacterizationFixtureCaptureTest` and the new differential censoring test); ArchUnit architecture tests (`torg-codex/src/test/java/.../architecture/*ArchitectureTest.java`) must continue to pass unmodified.

**Target Platform**: Linux server — Spring Boot web application (`torg-codex` module), deployed as the existing single deployable artifact; no new deployment unit.

**Project Type**: Web service (existing 4-module Maven reactor: `torg-codex-domain`, `torg-codex-application`, `torg-codex-data`, `torg-codex`). This feature only touches `torg-codex` (adapter/security layer) and its tests; `torg-codex-domain`/`torg-codex-application` are untouched, and `torg-codex-data`'s markup pipeline (`Censor`, `TorgMarkupService`, `ConditionalBlockProcessor`) is untouched (still receives a plain `Set<String>`, still knows nothing about Spring Security, per the existing, correct module boundary).

**Performance Goals**: No added latency and no added round-trips (SC-005): ownership resolution must remain a single, in-memory read of the already-populated `SecurityContextHolder` authorities — no new database query, no new DriveThruRPG API call, per request.

**Constraints**: FR-009 (zero regression for non-gated REST responses — verified by the existing characterization fixture byte-comparisons for all non-gated fields); FR-010 (no schema/domain/business-logic changes outside the ownership-resolution layer); FR-006 (markup rendering order `conditional blocks → entity references → raw HTML → game tokens → CommonMark` unchanged — `TorgMarkupService`/`ConditionalBlockProcessor` are not modified); constitution workflow gate ("Architectural Migrations MUST be incremental and leave the repository buildable after every task" — confirmed baseline `./mvnw -o test-compile` passes cleanly today under JDK 25).

**Scale/Scope**: 15 of the 17 catalog REST controllers are wired to `CurrentUserCensorFactory` (`Article, Cosm, Item, Miracle, MiracleList, Perk, PerkGroup, Power, PowerList, Race, Shard, Spell, SpellList, Threat, Vehicle`); `Publication` and `Tag` have no gated markup fields and stay untouched. Production code touched: 1 existing class fixed (`CurrentUserCensorFactory`), 1 new small collaborator added (`ProductOwnershipResolver`), 1 dead class removed (`SecuredMarkupService`). No controller/mapper/DTO signatures change.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|---|---|---|
| **I. Hexagonal Architecture & Clean Ports** | **PASS** | `torg-codex-domain`/`torg-codex-application` are untouched. `torg-codex-data`'s `Censor`/`TorgMarkupService`/`ConditionalBlockProcessor` keep their existing, Spring-free `(String, Set<String>)` contract. `CurrentUserCensorFactory` and the new `ProductOwnershipResolver` live in `torg-codex` (an inbound/primary adapter — REST-request-scoped security resolution), which already depends on Spring Security; this is the same layer `CurrentUserCensorFactory` and `SecuredMarkupService` already occupied, so no new module dependency or boundary crossing is introduced. Existing `ModuleBoundaryArchitectureTest`/`DomainPurityArchitectureTest`/`ApplicationPurityArchitectureTest` ArchUnit suites are expected to keep passing unmodified. |
| **II. Self-Contained Systems & Async Integration** | **N/A** | No inter-service/AMQP/event-bus surface is touched. |
| **III. Standardized REST APIs & OpenAPI Contracts** | **PASS** | No endpoint, media-type, or DTO shape changes (FR-009). No OpenAPI spec file exists in the repo today to update (verified: no `springdoc`/`swagger` dependency in any module `pom.xml`); REST contract stability for this feature is instead verified via the existing characterization fixtures' non-gated-field byte-equality. |
| **IV. Zero-Downtime Evolutionary DB Migrations** | **N/A / PASS** | FR-010 forbids schema changes; none are made. No new Liquibase changesets. |
| **V. Multi-Tier Security, DRM Censorship & Data Protection** | **FAIL today → PASS by design** | This principle (v1.1.0) is the one this feature exists to satisfy: (a) single ROLE_<codex-id>-based mechanism — delivered by consolidating onto `CurrentUserCensorFactory` → `ProductOwnershipResolver` and deleting `SecuredMarkupService`; (b) no bypass of censoring — no repository/entity/controller/mapper changes, all gated fields still flow through `Censor.apply(...)`; (c) principal-type independence — `ProductOwnershipResolver` reads `Authentication.getAuthorities()` generically, not a DriveThruRPG-specific `UserDetails` subtype; (d) test-adequacy rule — satisfied by the new differential test (Phase 1 design, research.md Decision 5) that asserts an actual output difference between anonymous and owner responses for the Aysle cosm, not mere fixture byte-equality. **Gate re-check after Phase 1**: design satisfies all four sub-rules; see research.md Decisions 1–5. |
| **VI. Deterministic Markdown Pipeline & Template Safety** | **PASS** | FR-006: rendering order and the flexmark-java/OWASP-sanitizer pipeline are untouched; only the *input* (`Set<String> ownedProducts`) computation changes, not `TorgMarkupService`/`ConditionalBlockProcessor`/downstream stages. |
| **VII. Production Observability & Quality Standards** | **N/A** | No new operational surface; existing Slf4j logging in touched classes is preserved as-is. |
| **VIII. Test-First & Integration Verification (NON-NEGOTIABLE)** | **PASS (post-design)** | New unit test for `ProductOwnershipResolver` (porting `SecuredMarkupServiceTest`'s 4 scenarios), a new differential integration test asserting anonymous ≠ owner for the Aysle cosm (FR-008/SC-001), and regenerated `owner-detail.json`/`anonymous-detail.json` characterization fixtures (now genuinely differing) so `CharacterizationReplayTest` also guards the fix going forward. Full `./mvnw clean verify` (Testcontainers/Failsafe) must stay green. |

**No Constitution violations require justification** — the Complexity Tracking table below is intentionally empty.

## Project Structure

### Documentation (this feature)

```text
specs/001-unify-censoring-authorization/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   ├── product-ownership-resolver.md
│   └── censoring-single-mechanism-invariant.md
├── checklists/
│   └── requirements.md  # Already validated (spec quality gate)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

**Structure Decision**: Existing single Maven reactor, 4-module hexagonal layout (Option 1
variant, already established by the prior Hexagonal Architecture Migration — no new module or
project is introduced). This feature's changes are confined to the `torg-codex` module's
security/adapter package and its tests; `torg-codex-domain`, `torg-codex-application`, and the
markup pipeline inside `torg-codex-data` are not modified.

```text
torg-codex-domain/                # Domain model (untouched by this feature)
torg-codex-application/           # Application/use-case ports (untouched by this feature)

torg-codex-data/
└── src/main/java/de/paladinsinn/
    ├── security/                                    # DriveThruRPG auth (untouched)
    │   ├── DriveThruUserDetails.java                 #  - already emits ROLE_<codexId> correctly
    │   ├── DriveThruUserService.java                 #  - getCurrentUser() no longer used for censoring
    │   ├── NotLoggedInUserDetails.java                #  - anonymous principal, ROLE_core-rulebook
    │   ├── ApiKeyAuthenticationToken.java / ApiKeyAuthenticationFilter.java / DriveThruAuthenticationProvider.java
    │   └── DriveThruSecurityConfig.java
    └── torg/codex/data/markup/                       # Rendering pipeline (untouched — Spring-Security-free)
        ├── Censor.java
        ├── TorgMarkupService.java
        └── ConditionalBlockProcessor.java (+ EntityReferenceProcessor, RawHtmlProcessor, GameTokenProcessor, MarkdownProcessor)

torg-codex/
└── src/main/java/de/paladinsinn/torg/codex/
    ├── api/security/
    │   ├── CurrentUserCensorFactory.java              # MODIFIED — delegates to ProductOwnershipResolver
    │   └── ProductOwnershipResolver.java               # NEW — single source of truth for ROLE_<codex-id> -> product-id
    ├── markup/
    │   └── SecuredMarkupService.java                   # REMOVED (dead, unwired duplicate; FR-007)
    ├── security/
    │   └── TorgCodexSecurityConfig.java                # unchanged (anonymous principal already correct)
    └── api/controller/
        ├── CosmController.java, ArticleController.java, ... (13 more)  # UNCHANGED — already call censorFactory.create()
        └── PublicationController.java, TagController.java              # UNCHANGED — no gated fields, no censor factory

torg-codex/
└── src/test/java/de/paladinsinn/torg/codex/
    ├── api/security/
    │   └── ProductOwnershipResolverTest.java           # NEW — ports SecuredMarkupServiceTest's 4 scenarios
    ├── markup/
    │   └── SecuredMarkupServiceTest.java                # REMOVED (tests deleted class)
    └── characterization/
        ├── CharacterizationAuthVariant.java, CatalogArea.java, CharacterizationReplayTest.java,
        │   CharacterizationFixtureCaptureTest.java, CharacterizationFixtureSupport.java   # unchanged
        └── CensoringDifferentialTest.java               # NEW — FR-008/SC-001: asserts live anon ≠ owner body for Aysle
    └── src/test/resources/characterization/cosms/
        ├── owner-detail.json / anonymous-detail.json    # REGENERATED — will now genuinely differ once the id search in
        └── (16 other areas' fixtures unchanged unless their captured detail id also becomes a true DRM-sensitive id)
```

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

*No violations — table intentionally empty.*
