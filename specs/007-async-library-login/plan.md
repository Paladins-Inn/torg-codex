# Implementation Plan: Asynchronous Library Login, User Notifications, Caching, and Request Observability

**Branch**: `007-async-library-login` | **Date**: 2026-11-16 (revised after clarifications Q7–Q10) | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/007-async-library-login/spec.md`

> **Implementation handoff**: [tasks.md](./tasks.md) is regenerated against clarifications Q7–Q11, including Spring Event + `SseEmitter` push notifications (replacing browser polling), the single page metric, `TRACE` SSE-stream/ack logging, digest-only session state with re-sign-in on expired tokens, and CSRF/CORS `FR-019`.

## Summary

Reader sign-in validates the DriveThruRPG API key synchronously, signs the user in immediately and streams the purchased library in the background, unlocking each batch through the single `ROLE_<codex-id>` mechanism. A generic, framework-free notification subsystem propagates notifications via Spring Events (`ApplicationEventPublisher` / `@EventListener`) and pushes Bootstrap toast fragments to open reader and `/admin` layouts over Server-Sent Events (`SseEmitter` / `EventSource`) with zero periodic browser polling, backed by a session-scoped pending buffer + acknowledgment for redirect/navigation safety. Complete libraries are cached for 12 h in Caffeine via `@Cacheable`/`@CachePut`, keyed by the API-key SHA-256 digest that the session already holds; logout never evicts. Signed-in users can reload their library; if the upstream token cannot be renewed they keep their access and get a sticky re-sign-in toast. Page handlers are timed with one `@Timed` meter (`torg.codex.web.page`), logout explicitly. A servlet filter logs `DEBUG` start / `INFO` completion per user request (`TRACE` for toast stream/ack). Spring Security CSRF and CORS protect every browser state change.

Design detail: [research.md](./research.md) (D1–D10), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md).

## Design Rules (binding for task generation)

1. **Notifications** are Spring Event-driven push (`ApplicationEventPublisher` → `@EventListener` → `SseEmitter` / `EventSource`) with non-destructive session buffering + acknowledgment (≤ 20 per SSR/initial connect flush, channel-scoped idempotent ack, browser `sessionStorage` de-duplication, emitter completion + cleanup on session end only — zero periodic HTTP polling). Producers use a framework-free port/event (`Constitution II`).
2. **Session secrets**: the session stores `ApiKeyFingerprint` and a `DrivethruToken#sessionCopy()` without the application key — never the cleartext key (Q9). Renewal without a key is refresh-only; failure → `REAUTH_REQUIRED` + sticky `ERROR` toast, entitlements and cache untouched.
3. **Cache**: key is `ApiKeyFingerprint` via `libraryCacheKeyGenerator` only (no `key` attribute); dedicated Caffeine registration for `"drivethru.library"` (12 h / 1000) without changing product/publisher caches; per-fingerprint `LibraryLoadCoordinator`; superseded/failed runs never write; tests use real proxies.
4. **Session generations**: background batch writes and request-side `SecurityContext` saves are generation-checked (`GenerationAwareSecurityContextRepository`); stale requests never overwrite newer batches, resurrect logged-out sessions or replace a newer login. Header-mode requests resolve fully and do not disturb a running browser generation.
5. **Streaming**: sequential page streaming in the background worker; bounded page fan-out is optional optimization only.
6. **Metrics** (Q7): one meter `torg.codex.web.page`, `@Timed` on page handlers via Boot `management.observations.annotations.enabled=true`; explicit `LogoutTimingFilter` for `POST /logout` with the identical tag keys `page,class,method,exception`; no `@Timed` on exception advice; no second handler meter.
7. **Logging** (Q8): `DEBUG` start / `INFO` completion for user requests incl. `/api/**` and `/admin/**`; `TRACE` for toast SSE stream/ack; nothing for assets/actuator; bounded route values, never raw paths or secrets; exactly one completion per logical request with the final status.
8. **CSRF/CORS** (Q10): CSRF on for the UI chain (`HttpSessionCsrfTokenRepository`, default XOR handler), `/api/**` ignored; token rotated on login, cleared on logout; tokens only rendered where state-changing controls exist (anonymous catalog browsing stays session-free); restrictive `CorsConfigurationSource` for UI paths (configurable allowed origins, default same-origin only); rejections `403` without side effects. Existing POST tests must add `.with(csrf())`.
9. **Presentation**: records ≤ 7 components, MapStruct mappers, reader/admin isolation, auto-escaped JTE only.
10. **ADR-023 and ADR-024** are drafted as *Proposed* in the setup phase (`ADR-023` covering async library sync, caching, CSRF/CORS, and observability; `ADR-024` dedicated to the event-driven session + authenticated-broadcast SSE notification subsystem); maintainer acceptance is a pre-merge gate (Q6).

## Technical Context

**Language/Version**: Java 25, Maven wrapper `./mvnw`, 4-module reactor.

**Primary Dependencies**:
- *Existing (verified)*: Spring Boot 4.1.1 (Web MVC, Security, Cache, `spring-boot-starter-aspectj`, Actuator), Micrometer 1.17.1 + Prometheus registry + Brave tracing, JTE 3.2.3, Bootstrap 5.3.8, Bootstrap Icons 1.13.1, htmx 2.0.8, Lombok, MapStruct 1.6.3, ArchUnit 1.4.1, JUnit 5/AssertJ/Mockito, WireMock 3.13.2, Testcontainers PostgreSQL, `spring-security-test`.
- *Added*: `com.github.ben-manes.caffeine:caffeine`, versionless (BOM-managed: `3.2.4` via Boot 4.1.1 in `torg-codex`, `3.2.3` via Boot 4.0.6 BOM in `torg-codex-application`; runtime resolves `3.2.4`).

**Storage**: In-memory Caffeine caches and `HttpSession` only. No PostgreSQL/Liquibase changes.

**Testing**: Application unit tests (`torg-codex-application`); MockMvc + WireMock (annotation and file-based) + Testcontainers tests in `torg-codex` (Surefire `*Test`); ArchUnit suites; full `./mvnw clean verify`. See [quickstart.md](./quickstart.md).

**Target Platform**: Linux container running the `torg-codex` Spring Boot service (single instance; reverse proxies need `server.forward-headers-strategy` for correct same-origin detection).

**Project Type**: Self-contained web application + REST service (hexagonal reactor).

**Performance Goals**: Login response `< 3 s` with `30 s+` libraries (`SC-001`); batch unlock `< 1 s` (`SC-002`); toasts `≤ 5 s` via immediate SSE push (`SC-003`); cached sign-in/API calls 0 `/order_products` requests, unlock `< 1 s` (`SC-004`).

**Constraints**: Application/domain purity; controllers only in `web.codex`/`web.admin`; single ownership mechanism; no cleartext key, token, fingerprint or `ROLE_` set in logs/tags; Checkstyle (≤ 7 parameters, no magic numbers, type Javadoc); backward-compatible `/api/**` behavior.

**Scale/Scope**: ≤ 1000 cached libraries, ≤ 20 toasts per response, 8 timed pages, 4 notification endpoints, 1 reload endpoint, 2 servlet filters (`UserRequestLoggingFilter`, `LogoutTimingFilter`), 2 new scripts (`csrf.js`, `notifications.js`).

## Constitution Check

*GATE: evaluated before Phase 0 and re-evaluated after Phase 1.*

| Principle | Pre-design | Post-design | Notes |
|---|---|---|---|
| I. Hexagonal Architecture | PASS | PASS | Notification ports/service framework-free in `application`; DriveThru cache/sync/fingerprint in `de.paladinsinn.drivethru`/`security` (`ADR-015`); controllers in `web.codex`/`web.admin`; shared view models + MapStruct mapper in `web.notification`; filters in `api.observability`/`security`; `torg-codex-data` untouched. |
| II. SCS & Async Integration | PASS | PASS | In-process `TaskExecutor`; background notifications via Spring Events bridge (required in-process eventing); no broker or external push service. |
| III. REST & OpenAPI | PASS | PASS | `/api/**` contract unchanged; CSRF ignored and CORS untouched for `/api/**`; header mode keeps full synchronous resolution. |
| IV. Zero-Downtime Migrations | PASS | PASS | No schema/data change. |
| V. Security, DRM & Data Protection | PASS | PASS | Single `ROLE_<codex-id>` mechanism with atomic context replacement; fail-closed batches; digest-only session and cache keys, application key removed from session tokens; CSRF re-enabled and CORS restricted for browser UI (closes the documented "CSRF disabled" gap). |
| VI. Markdown & Template Safety | PASS | PASS | Pipeline untouched; toasts and CSRF values only via auto-escaped `${…}`. |
| VII. Observability | PASS | PASS | Native ECS logs per `ADR-017`/`ADR-022` (`DEBUG`/`INFO`/`TRACE`, domain outcomes at `INFO`); bounded `torg_codex_web_page_seconds_*` on `/actuator/prometheus`. |
| VIII. Test-First | PASS | PASS | Unit, ArchUnit, MockMvc (incl. CSRF/CORS negative cases), WireMock annotation + file mappings, Testcontainers; DRM tests assert differing output across ownership states. |
| Governance / ADR-001 | PASS | PASS | ADR-023 and ADR-024 explicitly reviewed and accepted by maintainer (`klenkes74`) on 2026-11-16. |
| Workflow 5: Backward compatibility | PASS | PASS | Externally visible changes (CSRF on forms, async login) are explicitly specified (`FR-002`, `FR-019`); `/api/**` unchanged. |

**Gate result**: All constitutional and ADR governance gates pass.

## Project Structure

### Documentation (this feature)

```text
specs/007-async-library-login/
├── plan.md            # this file
├── research.md        # Phase 0 (verified facts + D1–D10)
├── data-model.md      # Phase 1
├── quickstart.md      # Phase 1 validation guide
├── contracts/
│   ├── web-ui-and-notifications.md   # login, reload, logout, toasts, CSRF/CORS
│   ├── ports-and-caching.md          # ports, cache API, session secrets, ArchUnit
│   └── observability-and-logging.md  # page timer, request logs
├── checklists/requirements.md
└── tasks.md           # regenerated story-ordered implementation tasks
```

### Source Code (repository root)

```text
torg-codex-application/
├── pom.xml                                         # + caffeine (versionless)
└── src/main/java/de/paladinsinn/
    ├── drivethru/
    │   ├── ApiKeyFingerprint.java                  # NEW SHA-256 digest value
    │   ├── DriveThruLibraryBatch.java              # NEW
    │   ├── DriveThruLibrarySnapshot.java           # NEW cache value
    │   ├── LibraryBatchListener.java               # NEW callback (+ SupersededSyncException)
    │   ├── LibraryLoadCoordinator.java             # NEW per-fingerprint serialization
    │   ├── DriveThruLibraryCacheConfig.java        # NEW Caffeine registration + key generator + properties
    │   ├── DriveThruLibraryCacheService.java       # NEW @Cacheable/@CachePut
    │   ├── DriveThruRPGService.java                # MOD streamOwnedProductBatches, key-optional renewal
    │   └── token/DrivethruToken.java               # MOD sessionCopy()
    ├── security/
    │   ├── ApiKeySessionAuthentication.java        # MOD fingerprint type, session-safe token, cache-hit/miss split, CSRF strategy
    │   ├── DriveThruAuthenticationProvider.java    # MOD use loadLibrary(fingerprint, token)
    │   ├── DriveThruLibrarySynchronizer.java       # NEW background batches, generation checks, lifecycle events
    │   ├── DriveThruSessionSyncState.java          # NEW (+ SyncStatus)
    │   └── GenerationAwareSecurityContextRepository.java # NEW
    └── torg/codex/application/
        ├── port/in/{NotificationSeverity,NotificationActionKind,NotificationAction,UserNotification,
        │            UserNotificationRequested,UserNotificationCommand,UserNotificationQuery}.java  # NEW
        ├── port/out/UserNotificationStorePort.java # NEW
        └── service/UserNotificationService.java    # NEW

torg-codex/
├── pom.xml                                         # + caffeine (versionless)
├── src/main/java/de/paladinsinn/torg/codex/
│   ├── api/
│   │   ├── configuration/
│   │   │   ├── NotificationConfiguration.java      # NEW service/store wiring + Spring Events bridge + sync executor
│   │   │   └── WebObservabilityConfiguration.java  # NEW servlet filter registration before Spring Security
│   │   └── observability/
│   │       ├── UserRequestClassification.java      # NEW bounded route classification
│   │       └── UserRequestLoggingFilter.java       # NEW DEBUG/INFO/TRACE boundary filter
│   ├── security/
│   │   ├── TorgCodexSecurityConfig.java            # MOD CSRF, CORS, LogoutTimingFilter, logout channel cleanup
│   │   ├── LogoutTimingFilter.java                 # NEW explicit logout-submit timer
│   │   └── WebCorsProperties.java                  # NEW torg.codex.web.cors.*
│   └── web/
│       ├── admin/AdminNotificationController.java  # NEW SSE stream + ack
│       ├── codex/
│       │   ├── CodexAuthController.java            # MOD async login, reload, CSRF view model, @Timed
│       │   ├── CodexCatalogController.java         # MOD reserved prefixes, @Timed
│       │   ├── CodexHomeController.java            # MOD @Timed
│       │   ├── CodexNotificationController.java    # NEW SSE stream + ack
│       │   └── model/NavContextViewModel.java      # MOD toast region + CSRF
│       └── notification/                           # NEW shared (non-controller)
│           ├── InMemoryUserNotificationStoreAdapter.java
│           ├── NotificationSessionListener.java    # closes emitters + clears channel on session destroy
│           ├── NotificationSurface.java            # CODEX vs ADMIN fragment selector
│           ├── SseNotificationBroadcaster.java     # Spring @EventListener -> SseEmitter push
│           ├── UserNotificationPublished.java      # Spring event emitted when notification stored
│           ├── ToastViewModel.java (+ ToastContent, ToastPresentation, ToastActionView)
│           ├── ToastRegionViewModel.java
│           ├── ToastViewModelMapper.java           # MapStruct
│           ├── CsrfViewModel.java
│           └── WebNotificationSupport.java
├── src/main/jte/
│   ├── admin/fragments/toasts.jte                  # NEW
│   ├── admin/layout/page.jte                       # MOD toast region, CSRF meta, scripts
│   ├── codex/fragments/toasts.jte                  # NEW
│   ├── codex/layout/{page,navbar}.jte              # MOD toast region, CSRF meta, Reload Library form
│   └── codex/pages/{login,logout}.jte              # MOD hidden CSRF field
└── src/main/resources/
    ├── application.yaml                            # MOD console-only ECS, cache, sync executor, SSE, CORS, annotations.enabled
    ├── application-dev.yaml                        # MOD dev-only ECS file logging
    └── static/js/{csrf,notifications}.js           # NEW

docs/modules/arc42/pages/
├── 05_building_block_view/level2_drivethrurpg_security_userservice.adoc  # MOD
├── 08_concepts/observability.adoc                                        # MOD
├── 08_concepts/data-protection.adoc                                      # MOD session secrets (digest only), CSRF/CORS
└── 09_architecture_decisions/
    ├── 023_async-library-sync-and-notifications.adoc                     # NEW (Proposed)
    └── 024_event-driven-user-notifications.adoc                          # NEW (Proposed) + _nav/index/_include
```

**Structure Decision**: Keep the 4-module hexagonal reactor. Framework-free notification core in `torg-codex-application` (`application` packages); DriveThru caching, fingerprint and session sync in `de.paladinsinn.drivethru`/`de.paladinsinn.security`; web, security filters, observability and templates in `torg-codex`. `torg-codex-domain` and `torg-codex-data` stay untouched.

## Complexity Tracking

No constitutional exception is proposed. ADR-023 and ADR-024 are accepted (`2026-11-16`).
