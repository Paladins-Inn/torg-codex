# Tasks: Asynchronous Library Login, User Notifications, Caching, and Request Observability

**Input**: `specs/007-async-library-login/{spec,plan,research,data-model,quickstart}.md` and `contracts/`.
**Tests**: Explicitly required by FR-017 and Constitution VIII. Write tests before the corresponding implementation, demonstrate the intended failure, then make them pass. Use latches, controlled responses and fake clocks rather than arbitrary sleeps.
**Organization**: Setup, shared foundations, four prioritized user stories, then cross-cutting completion. Every path below is relative to the repository root.

## Execution Rules

- `[P]` means disjoint files runnable concurrently **after prerequisites**; the dependency table defines the ready waves.
- Preserve existing uncommitted changes. Reuse existing bulk publication lookup, API-key normalization, authentication spans/logging and outbound HTTP metrics; do not overwrite earlier work.
- Use JDK 25, `./mvnw`, Lombok, Spring-component MapStruct mappers sharing `TorgMappingSupport`, and records/constructors with at most seven parameters.
- Generic application ports/services remain framework-free. Spring integration belongs in `de.paladinsinn.drivethru..`, `de.paladinsinn.security..` or web composition; reader/admin controllers stay in their own surface packages.
- No new schema, Liquibase changes, public JSON API, broker, admin maintenance functionality or second ownership mechanism. Current-session entitlements grow monotonically; a successful reload replaces the cached snapshot for future logins without inventing current-session revocation.
- The session and background closures never retain the cleartext API key. Worker token copies are refresh-only; header requests may use the key only within their synchronous request.
- Notification delivery is Spring Event-driven push (`ApplicationEventPublisher` -> `@EventListener` -> `SseEmitter` / `EventSource` with zero periodic browser polling) backed by non-destructive session buffering + acknowledgment and browser deduplication for redirect/navigation safety. Do not silently evict undisplayed messages.
- One page meter: `torg.codex.web.page`. Handler timing excludes later JTE rendering per Q7. SSE stream/ack logs are TRACE only per Q8. All browser POSTs require framework CSRF protection per Q10.
- Keep each completed implementation task buildable. Tasks introducing an endpoint must include its protection and required POST-test updates in the same increment.
- ADR-023 starts Proposed; only explicit maintainer acceptance closes the pre-merge approval task.

## Phase 1: Setup

**Purpose**: Establish architecture, dependencies and configuration without implementing story behavior.

- [X] T001 Draft ADR-023 (`023_async-library-sync-and-notifications.adoc`) and dedicated notification ADR-024 (`024_event-driven-user-notifications.adoc`) with status Proposed under `docs/modules/arc42/pages/09_architecture_decisions/` and register both in `_nav.adoc`, `index.adoc` and `_include.adoc`; cover async incremental ownership, Spring Event + SSE push notifications (session-targeted and all-authenticated-users broadcast), digest-keyed caching, key-free session tokens, CSRF/CORS, the single page meter and TRACE controls without claiming approval.
- [X] T002 [P] Add only missing versionless Caffeine dependencies in `torg-codex-application/pom.xml` and `torg-codex/pom.xml`; inspect each effective BOM separately, preserve existing Spring cache/AspectJ/Micrometer dependencies and avoid pinning or aligning unrelated dependency versions.
- [X] T003 Add the contract defaults in `torg-codex/src/main/resources/application.yaml`: library TTL 12h/max-size 1000, synchronization executor core-size 4/queue-capacity 100, notification SSE timeout 30m, empty UI CORS allowlist and `management.observations.annotations.enabled=true`; do not change product/publisher cache semantics or add notification expiry.

## Phase 2: Foundational Prerequisites

**Purpose**: Shared credential, streaming, notification and browser-protection primitives. Complete this phase before story integration.

### Tests First

- [X] T004 [P] Add fingerprint normalization/digest/redacted-string/equality tests in `torg-codex-application/src/test/java/de/paladinsinn/drivethru/ApiKeyFingerprintTest.java` and token-copy independence tests in `torg-codex-application/src/test/java/de/paladinsinn/drivethru/token/DrivethruTokenTest.java`; assert session copies preserve identity/expiry/access/refresh values but contain no application key.
- [X] T005 [P] Extend `torg-codex/src/test/java/de/paladinsinn/drivethru/DriveThruRPGServiceTest.java` using annotation-configured WireMock plus declarative mappings/bodies in `torg-codex/src/test/resources/mappings/` and `torg-codex/src/test/resources/__files/`; cover callback-before-next-page, server page-size caps, absent links, empty/duplicate products, partial failure, one renewal, refresh-only missing/expired/rejected tokens, and preserved outbound metrics.
- [X] T006 [P] Add framework-free validation/FIFO/replay/channel-isolation/idempotent-ack tests in `torg-codex-application/src/test/java/de/paladinsinn/torg/codex/application/service/UserNotificationServiceTest.java`; cover four severities, optional actions, more than 20 pending items without loss and rejection of external/scheme-relative/backslash/control-character navigation targets.
- [X] T007 [P] Add shared browser-protection tests in `torg-codex/src/test/java/de/paladinsinn/torg/codex/security/CsrfCorsProtectionTest.java`; cover login/logout missing or invalid tokens, genuine rendered form-token round trips, token rotation, same-origin and disallowed-Origin requests/preflights, explicitly allowed origins, unchanged API behavior and anonymous catalog GETs without session creation.

### Implementation

- [X] T008 Implement `torg-codex-application/src/main/java/de/paladinsinn/drivethru/ApiKeyFingerprint.java` and `sessionCopy()` in `torg-codex-application/src/main/java/de/paladinsinn/drivethru/token/DrivethruToken.java`; use stripped-key SHA-256, redacted diagnostic output, constant-time comparison against the session digest and independent token copies preserving the same subject.
- [X] T009 Add `DriveThruLibraryBatch.java`, `DriveThruLibrarySnapshot.java`, `LibraryBatchListener.java` and `SupersededSyncException.java` under `torg-codex-application/src/main/java/de/paladinsinn/drivethru/` and sequential streaming in `torg-codex-application/src/main/java/de/paladinsinn/drivethru/DriveThruRPGService.java`; return only complete immutable deduplicated snapshots, deliver each page immediately, preserve synchronous APIs and metrics, and permit key re-exchange only when the request-local token actually has an application key.
- [X] T010 Implement notification values/ports and framework-free `UserNotificationRequested` in `torg-codex-application/src/main/java/de/paladinsinn/torg/codex/application/port/in/`, `UserNotificationStorePort.java` in `torg-codex-application/src/main/java/de/paladinsinn/torg/codex/application/port/out/` and `UserNotificationService.java` in `torg-codex-application/src/main/java/de/paladinsinn/torg/codex/application/service/`; validate via services/factories, return at most 20 pending items without deletion and acknowledge only matching channel IDs.
- [X] T011 Add `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/notification/CsrfViewModel.java` and `torg-codex/src/main/resources/static/js/csrf.js`, then update `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/CodexAuthController.java`, `torg-codex/src/main/jte/codex/pages/login.jte`, `torg-codex/src/main/jte/codex/pages/logout.jte`, `torg-codex/src/main/jte/codex/layout/page.jte` and `torg-codex/src/main/jte/admin/layout/page.jte` to render escaped framework tokens in POST forms/meta tags and attach them only to same-origin htmx requests; avoid resolving deferred tokens for anonymous catalog pages with no controls.
- [X] T012 Enable UI CSRF and restrictive CORS in `torg-codex/src/main/java/de/paladinsinn/torg/codex/security/TorgCodexSecurityConfig.java`, add validated `torg-codex/src/main/java/de/paladinsinn/torg/codex/security/WebCorsProperties.java` and integrate the shared `HttpSessionCsrfTokenRepository` plus composite session-ID/CSRF authentication strategy into `torg-codex-application/src/main/java/de/paladinsinn/security/ApiKeySessionAuthentication.java`; ignore only `/api/**` for browser CSRF, preserve API/actuator CORS behavior, retain framework XOR handling, rotate/clear tokens and update existing POST tests in `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/codex/CodexAuthControllerTest.java` and `torg-codex/src/test/java/de/paladinsinn/torg/codex/security/ApiKeySessionAuthenticationTest.java` with valid CSRF where appropriate.
- [X] T013 Verify and complete session-secret integration in `torg-codex-application/src/main/java/de/paladinsinn/security/ApiKeySessionAuthentication.java` and `torg-codex-application/src/main/java/de/paladinsinn/security/DriveThruAuthenticationProvider.java` with regression assertions in `torg-codex/src/test/java/de/paladinsinn/torg/codex/security/ApiKeySessionAuthenticationTest.java`; migrate stored fingerprints to `ApiKeyFingerprint`, store only key-free principal/token copies on both form and header paths and make all T004-T007 scenarios pass without broad exception suppression.

**Checkpoint**: Shared contracts and framework protections are available. Key-free token copies work before any worker is introduced.

## Phase 3: User Story 1 - Immediate Login and Incremental Unlocking (P1, Technical MVP)

**Goal**: Sign in immediately after token validation, stream batches concurrently with browsing and unlock through the existing unified ownership/censoring path.

**Independent test**: Token exchange under 2s, library held pending for 30s or more: login plus destination rendering under 3s. Release batches one at a time; each unlock is visible on the next request within 1s while subsequent pages remain pending.

### Tests First

- [X] T014 [P] [US1] Extend `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/codex/CodexAuthControllerTest.java` for token-first redirect/free-tier destination, invalid-key 401/upstream-token 503 with no worker, session/CSRF rotation and retained authentication observations; use valid form tokens and controllable WireMock responses to prove login does not await the library.
- [X] T015 [P] [US1] Add synchronizer and stale-context-save tests in `torg-codex/src/test/java/de/paladinsinn/security/DriveThruLibrarySynchronizerTest.java` and `torg-codex/src/test/java/de/paladinsinn/security/GenerationAwareSecurityContextRepositoryTest.java`; cover monotonic authority union, immutable replacement, batch counters, partial failure, empty libraries, duplicate/changed-key sign-ins, logout, stale request completion, token-copy isolation and executor rejection.
- [X] T016 [P] [US1] Add actual gated-output tests in `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/codex/AsyncLibraryAccessTest.java` and complete-header tests in `torg-codex/src/test/java/de/paladinsinn/security/AsyncLibraryHeaderAuthenticationTest.java`; use public/schema-only fixtures to compare home/category/filter/embedded/detail/teaser output before and after each batch, and prove a header request carrying a partially loaded session still resolves fully without overwriting newer session state.

### Implementation

- [X] T017 [US1] Implement `DriveThruSessionSyncState.java`, `SyncStatus.java` and `GenerationAwareSecurityContextRepository.java` under `torg-codex-application/src/main/java/de/paladinsinn/security/`; share the session mutex, generation and applied-batch counter across worker writes and request saves, reject stale writes without recreating invalidated sessions and support IDLE/RUNNING/COMPLETED/FAILED/REAUTH_REQUIRED states.
- [X] T018 [US1] Add bounded `drivethruSyncExecutor` wiring with `ContextPropagatingTaskDecorator` in `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/configuration/NotificationConfiguration.java`; apply T003 executor properties and expose explicit rejection/failure handling without spawning unbounded threads or retaining request credentials.
- [X] T019 [US1] Implement `torg-codex-application/src/main/java/de/paladinsinn/security/DriveThruLibrarySynchronizer.java` to own a private key-free token copy, stream sequential batches, bulk-resolve publication references and atomically replace the active session context with unioned authorities; publish generation-checked Spring lifecycle events, persist renewed token copies, retain verified batches on failure and detach stale/logout runs.
- [X] T020 [US1] Add the token-only interactive entry point to `torg-codex-application/src/main/java/de/paladinsinn/security/ApiKeySessionAuthentication.java` and wire it in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/CodexAuthController.java`; validate the key before establishing authentication, preserve the composite session strategy, save free-tier context before dispatch, return 302/401/503 promptly and coordinate concurrent sign-ins without duplicate active runs.
- [X] T021 [US1] Wire generation-aware context persistence and logout detachment into `torg-codex/src/main/java/de/paladinsinn/torg/codex/security/TorgCodexSecurityConfig.java`; retain CSRF/CORS, invalidate the active generation before clearing the context/session and ensure old worker/request saves cannot resurrect or downgrade authentication.
- [X] T022 [US1] Preserve synchronous full-resolution header authentication in `torg-codex-application/src/main/java/de/paladinsinn/security/ApiKeyAuthenticationFilter.java` and `torg-codex-application/src/main/java/de/paladinsinn/security/DriveThruAuthenticationProvider.java`; do not reuse partial interactive ownership as complete, keep truly stateless requests session-free and prevent header completion from replacing newer browser state.
- [X] T023 [US1] Complete sanitized worker observations and explicit lifecycle failures in `torg-codex-application/src/main/java/de/paladinsinn/security/DriveThruLibrarySynchronizer.java`; distinguish refresh-required, upstream technical failure, task rejection and expected supersession without leaking payloads/tokens/product sets or clearing valid session access.
- [X] T024 [US1] Execute the US1 scenarios in `specs/007-async-library-login/quickstart.md` through T014-T016 tests; verify measured SC-001/SC-002 thresholds, real gated-output differences, partial/empty-library behavior, header compatibility and stale-save/logout races before accepting this increment.

**Checkpoint**: Technical MVP is independently testable without cache or toast presentation. Add US2 before user-facing rollout because both stories are P1.

## Phase 4: User Story 2 - Generic Asynchronous Toasts (P1)

**Goal**: Reusable reader/admin notifications with four severity modes, acknowledged delivery, isolated styling and stable reading content.

**Independent test**: Emit all four severities from a non-DriveThru producer on both layouts; verify replay/ack/navigation behavior. Real uncached login start/completion/auth-failure/library-failure toasts appear within 5s, with no unsolicited replacement of page content.

### Tests First

- [X] T025 [P] [US2] Add Spring-Event/SSE delivery, session-vs-broadcast target routing, bridge and session-cleanup tests in `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/notification/UserNotificationDeliveryTest.java`; cover ID rotation, immediate `@EventListener` -> `SseEmitter` push for both single-session and all-logged-in-users targets (excluding anonymous sessions), non-destructive pending flush, over-20 backlogs, concurrent enqueue/ack, channel isolation, emitter closure/invalidation versus late events and generic Spring-Event producers independent of DriveThru.
- [X] T026 [P] [US2] Add reader notification SSE/ack contract tests in `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/codex/CodexNotificationControllerTest.java`; assert private/no-store 200 `text/event-stream` (`GET /notifications/stream`) with initial pending flush + live Spring-Event push and 204 sessionless GETs, at-most-20 UUID ack batches, idempotency, malformed-ID 400 after valid CSRF, tokenless/sessionless POST 403, forbidden-Origin rejection without queue mutation and no session creation on anonymous GETs.
- [X] T027 [P] [US2] Add admin notification SSE/ack contract tests in `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/admin/AdminNotificationControllerTest.java`; verify identical SSE/CSRF/CORS/ack rules on `/admin/notifications/stream` and `/admin/notifications/ack`, generic Spring-Event producers, escaped content/actions, dark-theme isolation and preservation of the existing admin security policy without adding admin login or maintenance features.
- [X] T028 [P] [US2] Add behavioral browser coverage in `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/notification/ToastBrowserTest.java` using existing infrastructure or a minimal test-only runner in `torg-codex/pom.xml`; verify actual Bootstrap show/autohide/sticky-close, `EventSource` SSE reception without periodic polling, same-origin CSRF headers, replay deduplication after interrupted streams/acks and navigation, current-view reload only on action and delivery within 5s.

### Implementation

- [X] T029 [US2] Implement concurrency-safe session-channel storage with `UserNotificationPublished` Spring-Event emission in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/notification/InMemoryUserNotificationStoreAdapter.java`, `UserNotificationPublished.java`, `SseNotificationBroadcaster.java` and cleanup in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/notification/NotificationSessionListener.java`; preserve FIFO pending windows without TTL/cap eviction, push live JTE toast fragments to registered `SseEmitter`s on `@EventListener`, close emitters on logout/session destroy and prevent late events from recreating a destroyed channel.
- [X] T030 [US2] Wire `UserNotificationService`, generic ports, `SseNotificationBroadcaster` and the Spring event bridge in `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/configuration/NotificationConfiguration.java`; translate active-run lifecycle events to safe generic notification commands while keeping producers independent of JTE/htmx/Bootstrap and avoiding cross-module dependency cycles.
- [X] T031 [US2] Implement session-derived channel binding and non-destructive SSR/SSE-stream/ack support in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/notification/WebNotificationSupport.java`; survive session-ID rotation, support request-scoped login-error toasts, return no channel for sessionless SSE requests and never accept a request-supplied channel identifier.
- [X] T032 [US2] Add composed presentation records in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/notification/ToastViewModel.java` with `ToastContent.java`, `ToastPresentation.java`, `ToastActionView.java`, `ToastRegionViewModel.java` and `ToastViewModelMapper.java` in the same directory; map severity/ARIA/dismissal/actions through MapStruct, keep each record at most seven components and preserve both DOM ID and UUID for acknowledgments.
- [X] T033 [US2] Implement protected `GET /notifications/stream` (`SseEmitter`) and `POST /notifications/ack` in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/CodexNotificationController.java`; apply the exact status/no-store/SSE/UUID/channel contracts, rely on framework CSRF/CORS and do not annotate these control endpoints with page timing.
- [X] T034 [P] [US2] Implement protected admin SSE stream/ack counterparts (`GET /admin/notifications/stream`, `POST /admin/notifications/ack`) in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/admin/AdminNotificationController.java` using only shared support and admin templates, not reader classes; retain the existing surface access policy and sessionless behavior.
- [X] T035 [US2] Create accessible escaped fragments in `torg-codex/src/main/jte/codex/fragments/toasts.jte` and `torg-codex/src/main/jte/admin/fragments/toasts.jte`, then integrate SSR regions, CSRF metadata, scripts and `data-stream-url`/`data-ack-url` attributes (with zero periodic polling) in `torg-codex/src/main/jte/codex/layout/page.jte` and `torg-codex/src/main/jte/admin/layout/page.jte` with `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/model/NavContextViewModel.java`; retain separate visual themes and avoid anonymous catalog sessions/streams.
- [X] T036 [US2] Implement `torg-codex/src/main/resources/static/js/notifications.js` to initialize SSR toasts, subscribe via `EventSource` to the session SSE stream without polling, deduplicate displayed UUIDs in sessionStorage, acknowledge only displayed messages using fresh same-origin CSRF headers, retry missed acknowledgments and dispose dismissed DOM instances; handle rejected/expired sessions and page unload explicitly without reconnect loops or logging secrets.
- [X] T037 [US2] Connect lifecycle and request-scoped auth-error toasts in `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/configuration/NotificationConfiguration.java` and `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/CodexAuthController.java`, and reserve `library`, `notifications` and `js` in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/CodexCatalogController.java`; produce exactly one applicable start/completion/error sequence with the contracted wording and optional reload action.
- [X] T038 [US2] Execute the US2 generic-producer, route and browser scenarios in `specs/007-async-library-login/quickstart.md`; verify SC-003 timing, all four dismissal modes, no lost pending messages across redirects, channel isolation, CSRF-protected ack, theme isolation and stable main content.

**Checkpoint**: US1 + US2 are the recommended user-visible MVP. Both layouts can consume notifications from unrelated producers.

## Phase 5: User Story 3 - Shared Cache and Manual Reload (P2)

**Goal**: Reuse complete digest-keyed libraries across login/logout/header callers and support asynchronous manual refresh without retaining the API key.

**Independent test**: Load once, sign out, confirm free-tier-only access, sign in with the same valid key and observe zero library HTTP calls plus one ready toast. Use a fake cache ticker for the 12h TTL, then verify forced reload, incremental purchases and refresh-token failure without losing prior access/cache.

### Tests First

- [X] T039 [P] [US3] Add real-Spring-proxy tests in `torg-codex/src/test/java/de/paladinsinn/drivethru/DriveThruLibraryCacheServiceTest.java`; cover digest-only keys, exact default 12h expiry with controlled ticker, max-size/configuration, empty snapshots, key isolation, normal hit versus forced reload, complete-only writes, prior-entry preservation and unchanged product/publisher caches.
- [X] T040 [P] [US3] Add latch-driven races in `torg-codex/src/test/java/de/paladinsinn/drivethru/DriveThruLibraryCacheConcurrencyTest.java`; cover normal load versus reload, concurrent same-key fills/refreshes, recheck after coordination, supersession/logout at the final cache-commit boundary and cleanup of coordination state; assert no stale or aborted commit.
- [X] T041 [P] [US3] Add cache-aware session/header scenarios in `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/codex/CachedLibraryLoginTest.java` and `torg-codex/src/test/java/de/paladinsinn/security/DriveThruAuthenticationProviderTest.java`; verify fresh token validation, one ready toast, zero library calls after logout/re-login or repeated stateless calls, cached unlock within 1s and no session/key retention in stateless mode.
- [X] T042 [P] [US3] Add `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/codex/LibraryReloadTest.java` with annotation/file-based WireMock fixtures under `torg-codex/src/test/resources/mappings/` and `torg-codex/src/test/resources/__files/`; cover valid-token form/htmx contracts, missing/invalid CSRF and forbidden Origin without side effects, anonymous/unsupported principals, safe redirects, incremental purchases, old-cache preservation, superseded runs and expired/missing/rejected refresh tokens prompting re-sign-in without key re-exchange.

### Implementation

- [X] T043 [US3] Configure the dedicated library cache and `libraryCacheKeyGenerator` in `torg-codex-application/src/main/java/de/paladinsinn/drivethru/DriveThruLibraryCacheConfig.java`; register a custom bounded expiring Caffeine cache using T003 properties, accept only `ApiKeyFingerprint` for keys and preserve existing cache names/semantics without competing managers.
- [X] T044 [US3] Implement `torg-codex-application/src/main/java/de/paladinsinn/drivethru/DriveThruLibraryCacheService.java` with the contracted fingerprint-first `@Cacheable` normal load, `@CachePut` streaming reload and read-only probe; use only `keyGenerator`, cache token-free complete snapshots and propagate upstream/aborted/superseded failures before any normal-return write.
- [X] T045 [US3] Implement `torg-codex-application/src/main/java/de/paladinsinn/drivethru/LibraryLoadCoordinator.java` around actual proxied calls; serialize same-fingerprint operations through annotation commit, recheck ordinary misses after acquiring coordination, make active-generation validation and commit atomic against supersession/logout and release/remove lock state safely without serializing unrelated keys.
- [X] T046 [US3] Route header-provider loading through the coordinated shared cache in `torg-codex-application/src/main/java/de/paladinsinn/security/DriveThruAuthenticationProvider.java`; retain full single-request resolution, existing bulk publication mapping and token validation while avoiding external library requests on hits and storing no cleartext key in any session copy.
- [X] T047 [US3] Integrate the read-only cache probe and single-ready-toast path in `torg-codex-application/src/main/java/de/paladinsinn/security/ApiKeySessionAuthentication.java` and coordinated miss loading in `torg-codex-application/src/main/java/de/paladinsinn/security/DriveThruLibrarySynchronizer.java`; restore cached rights in one bulk query, skip the loading warning on hits and preserve incremental behavior and complete-only writes on misses.
- [X] T048 [US3] Add reload orchestration to `torg-codex-application/src/main/java/de/paladinsinn/security/DriveThruLibrarySynchronizer.java`; read only the session fingerprint and private key-free token copy, mint a newer generation, retain existing rights, force retrieval and emit lifecycle notifications; on non-renewable token set REAUTH_REQUIRED, retain session/cache and send the sticky Sign-in Required error with internal `/login` action.
- [X] T049 [US3] Implement CSRF-protected `POST /library/reload` in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/CodexAuthController.java` and its signed-in navbar form in `torg-codex/src/main/jte/codex/layout/navbar.jte`; provide htmx start-fragment and standard safe-redirect paths, hidden/header CSRF transport, anonymous redirect only after valid protection and no raw key input or current-page replacement.
- [X] T050 [US3] Execute cache/logout/reload scenarios in `specs/007-async-library-login/quickstart.md` using T039-T042 tests and recheck logout wiring in `torg-codex/src/main/java/de/paladinsinn/torg/codex/security/TorgCodexSecurityConfig.java`; verify SC-004 exact call counts/latency, TTL, empty libraries, failed/aborted no-write, no logout eviction, refresh success and re-sign-in failure handling.

**Checkpoint**: Shared caching and manual reload work for browser and stateless callers without weakening logout or retaining the key.

## Phase 6: User Story 4 - Single Page Metric and Request Boundaries (P2)

**Goal**: One bounded page timer per handled reader page and safe paired request logs with TRACE-only automated controls.

**Independent test**: Exercise all eight page classifications, category filters and detail/teaser/not-found outcomes; verify exact timer deltas and matching Prometheus tag keys. Check DEBUG/INFO boundaries for UI/API/security-rejected requests, TRACE-only SSE stream/ack and zero sensitive sentinel values in logs/tags.

### Tests First

- [X] T051 [P] [US4] Add `torg-codex/src/test/java/de/paladinsinn/torg/codex/web/codex/WebPageMetricsTest.java` for home/category/filter/detail/teaser/not-found/login GET+POST/logout GET+POST/reload; assert exactly one `torg.codex.web.page` sample, all eight fixed page values, identical `page,class,method,exception` key sets, no second handler meter and no page samples for advice/stream/ack/assets/actuator/API or pre-dispatch CSRF/CORS rejections.
- [X] T052 [P] [US4] Add `torg-codex/src/test/java/de/paladinsinn/torg/codex/api/observability/UserRequestLoggingFilterTest.java` and extend `torg-codex/src/test/java/de/paladinsinn/torg/codex/api/observability/StructuredLoggingTest.java`; cover DEBUG-start/INFO-completion, TRACE-only SSE stream/ack on both surfaces, INFO-only production behavior, final statuses on normal/error/async/security-terminated requests, no duplicate dispatch completion and exclusion of assets/actuator.
- [X] T053 [P] [US4] Add cardinality/privacy scenarios in `torg-codex/src/test/java/de/paladinsinn/torg/codex/api/observability/UserRequestTelemetryTest.java`; inject sentinel keys/tokens/CSRF/session/product values into paths/queries/forms/headers and vary slugs/UUIDs/search terms, asserting bounded metric labels/routes and no leaked sentinels, raw paths or WARN/ERROR for domain outcomes.

### Implementation

- [X] T054 [US4] Add bounded route classification and the lifecycle-aware request filter in `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/observability/UserRequestClassification.java` and `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/observability/UserRequestLoggingFilter.java`; retain logical-request state through error/async dispatch, log once with final status, use DEBUG/INFO for users and TRACE/TRACE for controls, and never include raw paths/headers/parameters or exception payloads.
- [X] T055 [US4] Register `UserRequestLoggingFilter` after server observations and before Spring Security through `torg-codex/src/main/java/de/paladinsinn/torg/codex/api/configuration/WebObservabilityConfiguration.java`; avoid duplicate servlet/security registration, preserve tracing and cover security-handled logout, auth failures and CSRF/CORS 403 outcomes.
- [X] T056 [US4] Add explicit logout timing in `torg-codex/src/main/java/de/paladinsinn/torg/codex/security/LogoutTimingFilter.java` and wire it before `LogoutFilter` in `torg-codex/src/main/java/de/paladinsinn/torg/codex/security/TorgCodexSecurityConfig.java`; match only dispatched `POST /logout`, use `torg.codex.web.page` with `page=logout-submit,class=LogoutFilter,method=logout,exception=none|SimpleName` and record once without timing pre-dispatch CSRF rejections.
- [X] T057 [US4] Annotate the seven contracted page handlers in `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/CodexHomeController.java`, `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/CodexCatalogController.java` and `torg-codex/src/main/java/de/paladinsinn/torg/codex/web/codex/CodexAuthController.java` with `@Timed(value="torg.codex.web.page", extraTags={"page", fixedValue})`; rely on Boot's enabled aspect, leave advice/notification controls unannotated and introduce neither an end-to-end page timer nor a separate handler meter.
- [X] T058 [US4] Preserve native ECS output and configurable request DEBUG/TRACE levels in `torg-codex/src/main/resources/application.yaml`; ensure annotation auto-configuration is active exactly once, existing authentication/outbound metrics remain unchanged and no payload logging or Logstash dependencies are added.
- [X] T059 [US4] Resolve any feature-path domain-outcome severity/leakage failures identified by T052-T053 in `torg-codex-application/src/main/java/de/paladinsinn/drivethru/DriveThruRPGService.java` and `torg-codex-application/src/main/java/de/paladinsinn/security/DriveThruLibrarySynchronizer.java`; keep invalid credentials/teasers/missing entries at INFO, distinguish genuine technical incidents and omit sensitive upstream payloads rather than suppressing failures.
- [X] T060 [US4] Execute the telemetry scenarios in `specs/007-async-library-login/quickstart.md` using T051-T053 tests; verify SC-005/SC-006 exact count/tag/log-level contracts, final request statuses, trace correlation and zero sentinel leakage, documenting handler-only latency rather than claiming JTE render coverage.

**Checkpoint**: One page metric and one safe boundary pair per in-scope logical request; background controls do not generate INFO noise.

## Phase 7: Polish and Cross-Cutting Completion

- [X] T061 [P] Extend architecture coverage in `torg-codex/src/test/java/de/paladinsinn/torg/codex/architecture/ApplicationPurityArchitectureTest.java`, `torg-codex/src/test/java/de/paladinsinn/torg/codex/architecture/ModuleBoundaryArchitectureTest.java`, `torg-codex/src/test/java/de/paladinsinn/torg/codex/architecture/AdapterConventionArchitectureTest.java`, `torg-codex/src/test/java/de/paladinsinn/torg/codex/architecture/WebUiSurfaceSeparationArchitectureTest.java` and `torg-codex/src/test/java/de/paladinsinn/torg/codex/architecture/CensoringSingleMechanismArchitectureTest.java`; cover new wiring without freeze exceptions and enforce `DrivethruToken#getApplicationKey` access only inside DriveThru/security integration packages.
- [X] T062 [P] Update `docs/modules/arc42/pages/05_building_block_view/level2_drivethrurpg_security_userservice.adoc`, `docs/modules/arc42/pages/08_concepts/observability.adoc`, `docs/modules/arc42/pages/08_concepts/data-protection.adoc` and `specs/006-codex-web-ui/contracts/web-ui-routes.md` for implemented async/cache/generation/token/notification behavior, single page timer, TRACE controls, CSRF/CORS and trusted reverse-proxy origin handling.
- [X] T063 Reconcile implementation details in `specs/007-async-library-login/plan.md`, `specs/007-async-library-login/research.md`, `specs/007-async-library-login/data-model.md`, `specs/007-async-library-login/contracts/web-ui-and-notifications.md`, `specs/007-async-library-login/contracts/ports-and-caching.md` and `specs/007-async-library-login/contracts/observability-and-logging.md`; keep selected semantics unchanged, record actual types/paths and leave ADR acceptance open unless explicitly approved.
- [X] T064 Complete the runnable validation matrix and exact implemented test selectors in `specs/007-async-library-login/quickstart.md`; run slow/empty/large libraries, first/middle/final-page failures, duplicate logins/reloads/logout/cache-commit races, expired tokens, notification browser behavior and real CSRF-token/CORS round trips, using public or schema-only fixtures.
- [X] T065 Run JDK-25 `./mvnw clean verify` as required by `.specify/memory/constitution.md` plus the DRM regression selection in `specs/007-async-library-login/quickstart.md`; fix only feature-caused Checkstyle/JTE/MapStruct/architecture/DRM/OpenAPI failures, preserve unrelated worktree changes and verify no Liquibase identities/manifests changed.
- [X] T066 Obtain explicit maintainer review/acceptance of `docs/modules/arc42/pages/09_architecture_decisions/023_async-library-sync-and-notifications.adoc` and `docs/modules/arc42/pages/09_architecture_decisions/024_event-driven-user-notifications.adoc` and record the real decision; if unavailable, keep this task and the merge gate open rather than setting Accepted autonomously.

## Dependencies and Execution Order

### Story Completion Graph

```text
Setup T001-T003 -> Foundations T004-T013
                              |
                              +-> US1 T014-T024 -> US2 T025-T038 -> US3 T039-T050
                              |
                              +-> US4 tests T051-T053
                                       |
                                       +-> logging T054-T055 (independent implementation)
                                       |
US1 + US2 + US3 ------------------------+-> US4 integration T056-T060

All stories -> T061 + T062 -> T063 -> T064 -> T065
ADR draft T001 + final design -> maintainer approval T066 (required before merge)
```

US2 generic delivery can be developed/tested with a non-DriveThru producer after foundations; its actual library lifecycle integration needs US1. US3 needs US1 loading and US2 toast delivery. US4's filter is independently testable, but final page coverage includes US3 reload and US2 controls. Cross-story tasks that edit the same files must be serialized.

### Concrete Prerequisites

Ranges denote every task in that range, not an implied serial dependency between its members. Tasks not marked `[P]` may still be sequenced in multiple ready branches by an implementer, but no shared-file edit may run concurrently.

| Task(s) | Prerequisites |
|---|---|
| T001, T002 | None |
| T003 | T002 |
| T004-T007 | T001-T003 |
| T008 | T004 |
| T009 | T005, T008 |
| T010 | T006 |
| T011 | T007 |
| T012 | T011 |
| T013 | T008-T012 |
| T014-T016 | Foundations T004-T013 |
| T017 | T015 |
| T018 | T014-T016, T003 |
| T019 | T009, T017, T018 |
| T020 | T014, T019 |
| T021 | T020 |
| T022 | T016, T021 |
| T023 | T019, T022 |
| T024 | T014-T023 |
| T025-T028 | Foundations; US1 for real lifecycle cases |
| T029 | T025, T010 |
| T030 | T029, T019 |
| T031 | T029, T030 |
| T032 | T026-T028, T010 |
| T033, T034 | T031, T032, T012 |
| T035 | T033, T034 |
| T036 | T028, T035 |
| T037 | T020, T030, T036 |
| T038 | T025-T037 |
| T039-T042 | US1 + US2 |
| T043 | T039, T002, T003 |
| T044 | T043, T009 |
| T045 | T040, T044 |
| T046 | T041, T045, T022 |
| T047 | T041, T045, T046, T037 |
| T048 | T042, T047 |
| T049 | T048, T042 |
| T050 | T039-T049 |
| T051-T053 | Foundations; may be authored against contracts before final story routes |
| T054 | T052, T053 |
| T055 | T054 |
| T056 | T051, T055, US1-US3 integration complete |
| T057 | T056 |
| T058 | T057 |
| T059 | T052, T053, T058 |
| T060 | T051-T059, T024, T038, T050 |
| T061, T062 | All stories complete |
| T063 | T061, T062 |
| T064 | T063 |
| T065 | T064 |
| T066 | T001 and final design available; explicit acceptance required before merge |

### Parallel Execution Examples

| Story | Independent ready work | Join before |
|---|---|---|
| US1 | T014 controller tests; T015 synchronizer/repository tests; T016 gated-output/header tests | T017-T020 production integration |
| US2 | T025 delivery tests; T026 reader contracts; T027 admin contracts; T028 browser behavior | Their corresponding implementation tasks; later T033 reader and T034 admin controllers can run together |
| US3 | T039 cache-proxy tests; T040 concurrency tests; T041 cached login/provider tests; T042 reload contracts | T043-T049 integration; serialize T047/T048 synchronizer edits |
| US4 | T051 page metrics; T052 filter/ECS logs; T053 privacy/cardinality | T054-T059 implementation; final annotations wait for page routes |

Additional opportunities: T001/T002 setup; T004-T007 foundational tests; T061/T062 architecture and documentation. These are dependency-ready waves, not an instruction to launch agents.

## Requirement Coverage

| Requirement | Primary tasks |
|---|---|
| FR-001, FR-002 | T014, T020, T024, T047 |
| FR-003, FR-004 | T005, T009, T015-T024 |
| FR-005 | T006, T010, T025-T038 |
| FR-006, FR-007 | T028, T030, T035-T038, T041, T047 |
| FR-008, FR-009 | T014-T015, T019, T023, T030, T037-T038 |
| FR-010 | T002-T004, T008, T013, T039-T047 |
| FR-011 | T015, T017, T021, T025, T029, T041, T050 |
| FR-012 | T039-T040, T044-T045, T048, T050 |
| FR-013 | T013, T016, T022, T041, T046 |
| FR-014 | T051, T056-T058, T060 |
| FR-015, FR-016 | T023, T052-T055, T058-T060 |
| FR-017 | All Tests First groups; T024, T038, T050, T060-T061, T064-T065 |
| FR-018 | T004-T005, T008-T009, T042, T048-T050 |
| FR-019 | T007, T011-T012, T026-T028, T033-T036, T042, T049, T064 |
| SC-001, SC-002 | T014-T016, T024 |
| SC-003 | T025-T028, T035-T038 |
| SC-004 | T039-T042, T050 |
| SC-005, SC-006 | T051-T053, T060 |

## Implementation Strategy

1. Complete setup and foundations, including key-free tokens and working framework CSRF/CORS before adding background workers or browser POST endpoints.
2. Deliver US1 as the technical MVP: prove real latency thresholds and rendered DRM differences. Add US2 before user-facing rollout so delayed access is explained.
3. Add US3 caching/reload with real proxy/commit-race tests and explicit re-sign-in handling; never trade token validation or session isolation for cache speed.
4. Finish US4 with exactly one handler-level page meter (explicit logout only), final-status request logging and TRACE-only controls. Do not reinstate the superseded page/handler split or require full-render timing under this meter.
5. Reconcile documentation, run the complete validation matrix and reactor, then obtain actual maintainer ADR acceptance. Do not mark implementation tasks complete merely because this list was generated.

**Inventory**: 66 tasks: Setup 3, Foundations 10, US1 11, US2 14, US3 12, US4 10, Polish 6. Twenty-two tasks carry `[P]`; actual concurrency follows the prerequisites above.
