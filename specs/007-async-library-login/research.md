# Phase 0 Research: Asynchronous Library Login, User Notifications, Caching, and Request Observability

**Feature**: `007-async-library-login` | **Date**: 2026-11-16 (revised after clarifications Q7–Q10) | **Spec**: [spec.md](./spec.md)

All `NEEDS CLARIFICATION` items are resolved. Decisions are design proposals; they are neither implemented nor architecturally approved until ADR-023 is accepted (D8).

## Verified Codebase Facts (inputs to the decisions)

| Fact | Evidence | Consequence |
|---|---|---|
| The session-held `DrivethruToken` carries the **cleartext API key** (`token.setApplicationKey(apiKey)`), stored via `DriveThruUserDetails` in the `SecurityContext`/`HttpSession`. Token renewal falls back to `requestToken(token.getApplicationKey(), …)`. | `torg-codex-application/.../drivethru/DriveThruRPGService.java` (`requestToken`, `renewToken`); `security/DriveThruUserDetails.java` | Violates Clarification Q9 → D9. |
| `ApiKeySessionAuthentication` already stores a SHA-256 digest of the stripped key (`…keyFingerprint`) in the session. | `security/ApiKeySessionAuthentication.java` | Reuse the same digest for session state and cache key (D3, D9). |
| CSRF is disabled in the only active chain (`TorgCodexSecurityConfig`, `@Order(1)`); no CORS configuration exists; JTE login/logout forms post without tokens. | `torg-codex/.../security/TorgCodexSecurityConfig.java`; `jte/codex/pages/login.jte`, `logout.jte` | FR-019 → D10; existing POST tests need `.with(csrf())`. |
| Boot 4.1.1 `MetricsAspectsAutoConfiguration` registers `TimedAspect` (`@ConditionalOnMissingBean`) when `management.observations.annotations.enabled=true`; `spring-boot-starter-aspectj` is present. | `spring-boot-micrometer-metrics-4.1.1.jar`; `torg-codex/pom.xml` | No hand-written aspect bean (D5). |
| Micrometer 1.17.1 `TimedAspect` tags: `class`, `method`, `exception` (default `none`) + `extraTags`. | `micrometer-core-1.17.1.jar` | Explicit logout timer must use the identical tag-key set (Prometheus requires one key set per meter name). |
| Caffeine is managed by both BOMs (`4.1.1` → `3.2.4` in `torg-codex`; `4.0.6` → `3.2.3` in `torg-codex-application`). | `spring-boot-dependencies-4.x.pom` | Declare versionless; `torg-codex` management wins at runtime. |
| ArchUnit allows shared non-controller web packages (only `web.codex` ↔ `web.admin` cross-dependencies are forbidden). | `WebUiSurfaceSeparationArchitectureTest` | Shared toast/CSRF view models in `web.notification`. |

---

## D1 — Asynchronous Post-Token Login and Incremental Session Entitlements

- **Decision**:
  1. Two entry modes in `de.paladinsinn.security`:
     - **Header mode** (`Authorization: ApiKey`, `ApiKeyAuthenticationFilter` → `DriveThruAuthenticationProvider`): full, synchronous ownership resolution per request (`FR-013`) via `DriveThruLibraryCacheService.loadLibrary(...)`. A header request carrying an existing partially loaded browser session still resolves fully and never downgrades or races the session's background generation.
     - **Interactive mode** (`POST /login`, `POST /library/reload`): `ApiKeySessionAuthentication` + `DriveThruLibrarySynchronizer`.
  2. `POST /login` validates the stripped key synchronously via `DriveThruRPGService.getToken(apiKey)`; invalid key → `401`, upstream outage → `503`, both with a sticky request-scoped `ERROR` toast and no background work (`FR-001`, `FR-008`).
  3. On a valid token the controller computes the `ApiKeyFingerprint`, applies `CompositeSessionAuthenticationStrategy(ChangeSessionIdAuthenticationStrategy, CsrfAuthenticationStrategy)` (session-ID and CSRF-token rotation, D10), stores a **session-safe** token copy without the application key (D9), and then:
     - **Cache hit** → one bulk `CatalogReferenceQuery.findPublicationsByProductIds(...)`, full `SecurityContext`, single `SUCCESS` "Publications Ready" toast, `302 → /`.
     - **Cache miss** → free-tier `SecurityContext` (empty `ownedCodexIds`; `ProductOwnershipResolver` adds `core-rulebook`), new `syncGenerationId`, `INFO` multi-minute toast, dispatch to bounded `drivethruSyncExecutor` (`ThreadPoolTaskExecutor` with `ContextPropagatingTaskDecorator`), `302 → /` (`SC-001`).
  4. `DriveThruRPGService.streamOwnedProductBatches(token, batchConsumer)` streams pages **sequentially**, delivering each page as a `DriveThruLibraryBatch`, and returns a complete `DriveThruLibrarySnapshot`. Running in the background already satisfies "parallel with browsing" (`FR-003`); bounded page fan-out via `links.last` is a later optimization, not an acceptance requirement.
  5. Per batch, the synchronizer resolves codex IDs, takes `WebUtils.getSessionMutex(session)`, checks session validity **and** `syncGenerationId`, then atomically replaces the immutable `DriveThruUserDetails` → `ApiKeyAuthenticationToken` → `SecurityContext` with the union of codex IDs (`FR-004`, `SC-002`).
  6. **Generation-aware context persistence**: request threads save the `SecurityContext` at response commit. A `GenerationAwareSecurityContextRepository` (wrapping `HttpSessionSecurityContextRepository`) refuses to overwrite a session context whose generation/batch counter is newer than the one loaded at request start, and never restores a context after logout or a newer login.
- **Rationale**: `AbstractAuthenticationToken` freezes authorities at construction, so only atomic replacement updates `getAuthorities()` for `ProductOwnershipResolver` (Principle V). Sequential streaming keeps pagination and token renewal deterministic.
- **Alternatives considered**: mutating `ownedCodexIds` in place (does not update cached authorities); asynchronous header mode (stateless callers have no follow-up cycle); parallel page fan-out up front (adds renewal/ordering races without acceptance value).

## D2 — Generic Cross-Surface Toast Notifications (Spring Events + `SseEmitter` Push with Ack Buffer)

- **Decision**:
  1. Framework-free model and ports in `de.paladinsinn.torg.codex.application` (`NotificationSeverity`, `NotificationActionKind`, `NotificationAction`, `UserNotification`, `UserNotificationCommand`, `UserNotificationQuery`, `UserNotificationStorePort`, `UserNotificationService`). Background producers publish a framework-free `UserNotificationRequested` value via `ApplicationEventPublisher`; storing any notification emits a `UserNotificationPublished` Spring event consumed by `SseNotificationBroadcaster` (`@EventListener`) to push server-rendered JTE toast fragments immediately to open browser tabs without periodic polling.
  2. Channel = random `UUID` in session attribute `torg.codex.notificationChannelId` (survives `changeSessionId()`); the channel is always derived from the session, never from request input.
  3. Delivery combines **immediate SSE push** with a **non-destructive pending buffer** for redirect/navigation safety: SSR and initial SSE stream connection read up to 20 pending items without deleting them, live notifications are pushed immediately as SSE `toast` events, and only `POST …/notifications/ack` removes IDs of the caller's channel. The browser deduplicates replayed IDs via `sessionStorage` and retries acks — at-least-once transport with deduplication and zero periodic HTTP polling. Emitter completion + store cleanup happen on logout/session destruction (`HttpSessionListener`), never by independent TTL or queue-cap eviction of undisplayed items.
  4. Surfaces: `CodexNotificationController` (`web.codex`, `GET /notifications/stream`, `POST /notifications/ack`) and `AdminNotificationController` (`web.admin`, `/admin/notifications/stream`, `/admin/notifications/ack`), separate templates/themes; shared presentation records, `SseNotificationBroadcaster`, and a MapStruct mapper in `web.notification`.
  5. Browser `EventSource` opens `GET …/notifications/stream` only when a session channel exists (anonymous catalog browsing opens zero SSE connections and creates zero sessions); `/js/notifications.js` initializes Bootstrap Toasts (`autohide` for `INFO`/`SUCCESS` 6 s; sticky `WARNING`/`ERROR`), handles `data-toast-action="reload"`, and sends acks with the CSRF header (D10).
  6. Stream and ack requests log at `TRACE` only and record no page metric (D5, D6).
- **Rationale**: Eliminates periodic browser polling completely while preserving domain-agnostic Spring Events decoupling, redirect/navigation durability, zero extra dependencies, and full MockMvc testability.
- **Alternatives considered**: Periodic HTTP polling (`every 2s` — rejected by user due to unnecessary polling traffic and latency); WebSocket/STOMP (requires additional Spring WebSocket/broker plumbing for unidirectional server→client notifications); destructive drain without ack (loses toasts across `302` redirects or interrupted responses); a single shared controller (violates `AdapterConventionArchitectureTest`).

## D3 — Annotation-Based 12-Hour In-Memory Library Cache Keyed by API-Key Digest

- **Decision**:
  1. Add versionless `com.github.ben-manes.caffeine:caffeine` to `torg-codex-application` and `torg-codex`. Register `"drivethru.library"` via `CacheManagerCustomizer<CaffeineCacheManager>#registerCustomCache` with `expireAfterWrite = drivethru.library.cache.ttl` (default `12h`) and `maximumSize = drivethru.library.cache.max-size` (default `1000`). Existing `"drivethru.product"`/`"drivethru.publisher"` caches stay registered with unchanged (unbounded, non-expiring) semantics.
  2. Cache key = `ApiKeyFingerprint` — lowercase hex SHA-256 of the stripped API key, the **same** digest the session already stores. Cache methods take the fingerprint (never the raw key) as first argument; a `libraryCacheKeyGenerator` bean extracts it. Use **only** `keyGenerator` (never together with `key`).
  3. `DriveThruLibraryCacheService` (separate proxied bean, avoids self-invocation):
     - `@Cacheable(cacheNames = "drivethru.library", keyGenerator = "libraryCacheKeyGenerator") loadLibrary(ApiKeyFingerprint, DrivethruToken)` — header mode.
     - `@CachePut(… same …) reloadLibraryStreaming(ApiKeyFingerprint, DrivethruToken, LibraryBatchListener)` — login cache miss and manual reload; writes only on normal return.
     - `findCachedLibrary(ApiKeyFingerprint)` — read-only probe.
  4. `LibraryLoadCoordinator` serializes fills/commits per fingerprint through the proxy's return; normal loads recheck the cache after acquiring coordination; superseded or aborted runs throw before return so they never write.
  5. Logout never evicts `"drivethru.library"` (`FR-011`); failures never write or overwrite (`FR-012`).
- **Rationale**: Honors "spring-boot caching with annotations", TTL + bound, no cleartext key in heap keys (`FR-010`), one shared digest for session and cache (`FR-018`).
- **Alternatives considered**: `ConcurrentMapCacheManager` (no TTL/bound); key by `customerId` (unknown before fetching, `null` for empty libraries); global `spring.cache.caffeine.spec` (would change product/publisher semantics); per-process HMAC key (stronger against offline guessing but diverges from the existing session digest; API keys are high-entropy, so SHA-256 is sufficient).

## D4 — Manual Asynchronous Library Reload (`POST /library/reload`)

- **Decision**: Navbar button (signed-in only) posting with the CSRF token to `POST /library/reload` (`CodexAuthController`). The handler reads the session's `ApiKeyFingerprint` and session-safe token, mints a new `syncGenerationId`, keeps current entitlements, enqueues the `INFO` start toast, and runs `reloadLibraryStreaming(...)` in the background. htmx requests receive the toast fragment (`200`); plain form posts redirect to a validated same-origin `Referer` path (default `/`). Anonymous → `302 /login` (`HX-Redirect` for htmx).
- **Expired upstream token**: on `401` the service tries `/auth/refresh` with the session's refresh token. No application key is available (D9), so if refresh is missing, expired or rejected, the run ends with `NoValidTokenException`: session entitlements and any valid cache entry stay untouched, the sync state becomes `REAUTH_REQUIRED`, and a sticky `ERROR` toast "Please sign in again" with a `NAVIGATE_URL` action to `/login` is enqueued (`FR-018`).
- **Rationale**: Unlocks newly purchased books inside the 12 h TTL without storing the key.
- **Alternatives considered**: re-prompting for the API key in the reload form (extra friction; reserved as the fallback via the toast's sign-in action); keeping the cleartext key in session (rejected by Q9).

## D5 — Single User-Page Timing Metric

- **Decision**:
  - Enable `management.observations.annotations.enabled: true` (Boot-managed `TimedAspect`).
  - Annotate page handlers with `@Timed(value = "torg.codex.web.page", extraTags = {"page", "<value>"})`: `home`, `category`, `detail`, `login-view`, `login-submit`, `logout-view`, `library-reload`.
  - `POST /logout` is handled by Spring Security's `LogoutFilter`, so `LogoutTimingFilter` (inserted before `LogoutFilter`, matching only `POST /logout`) records the **same** meter `torg.codex.web.page` with the identical tag-key set: `page=logout-submit`, `class=LogoutFilter`, `method=logout`, `exception=none|<SimpleName>`.
  - No `@Timed` on `CodexExceptionAdvice` (avoids double counting); handler failures appear in the `exception` tag. No second `torg.codex.web.handler` meter. Notification SSE stream/ack endpoints are not pages and are not timed.
  - Known limit (accepted by Q7): handler timing excludes JTE rendering after handler return; Boot's `http.server.requests` remains available for full-lifecycle latency.
- **Rationale**: Exactly one page metric, annotation-based where possible, bounded tags (`SC-005`).
- **Alternatives considered**: end-to-end page timer filter plus separate handler meter (rejected by Q7); `http.server.requests` only (not annotation-based, no business page names).

## D6 — Request Boundary Logging (`DEBUG` start / `INFO` end / `TRACE` stream-ack)

- **Decision**: `UserRequestLoggingFilter` (`OncePerRequestFilter`, `api.observability`), ordered after `ServerHttpObservationFilter` and before Spring Security's `FilterChainProxy`, so trace IDs are present and security-terminated requests (logout, CSRF/CORS `403`, header-auth `401`/`503`) are covered.
  - **Excluded** (no log): `/actuator/**`, `/webjars/**`, `/themes/**`, `/js/**`, `/css/**`, `/images/**`, `/favicon.ico`.
  - **Background control** (`GET|POST /notifications/{stream,ack}`, `/admin/notifications/{stream,ack}`): `TRACE` start + `TRACE` completion (via `AsyncListener` when SSE async processing completes).
  - **All other user requests** (reader UI, admin UI, `/api/**`): `DEBUG` `user-request-started`, `INFO` `user-request-completed`.
  - Fields: `http.request.method`, bounded route classification at start, matched pattern (`BEST_MATCHING_PATTERN_ATTRIBUTE`, else `unmatched`) at completion, `http.response.status_code`, `durationMs`. Never raw path, query string, parameters, headers, session ID, tokens or `ROLE_` sets.
  - Completion once per logical request: if async started, complete in an `AsyncListener`; if an exception escapes, report `500`; status-based domain outcomes (`401/403/404`) stay `INFO`.
- **Rationale**: One uniform place, covers filter-terminated requests, notification transport does not pollute `INFO` logs (Q8, `ADR-022`).
- **Alternatives considered**: `HandlerInterceptor` (misses security-terminated requests); `CommonsRequestLoggingFilter` (logs raw URIs/payloads, wrong levels).

## D7 — Hexagonal and ArchUnit Boundary Preservation

- **Decision**: Application ports/services stay Spring-free; DriveThru cache/sync types live in `de.paladinsinn.drivethru` / `de.paladinsinn.security` (`ADR-015`); controllers only in `web.codex`/`web.admin`; shared toast and CSRF view models in `web.notification`; filters in `api.observability`/`security`; no `GrantedAuthority`/`ProductOwnershipResolver` use in `web..`.
- **Rationale**: All existing ArchUnit suites pass without freeze-list exceptions.

## D8 — ADR-023 Governance

- **Decision**: Draft `docs/modules/arc42/pages/09_architecture_decisions/023_async-library-sync-and-notifications.adoc` with status **Proposed** in the setup phase (register in `_nav.adoc`, `index.adoc`, `_include.adoc`). It covers async incremental entitlements, the notification subsystem, digest-keyed caching across logout, session secret minimization, single page metric, and re-enabled CSRF/CORS. Maintainer acceptance is a **pre-merge gate**, not an implementation blocker (Q6).

## D9 — No Cleartext API Key in Session State

- **Decision**:
  - `DrivethruToken#sessionCopy()` returns a copy with `applicationKey = null`; `DriveThruUserDetails` and every session-stored context hold only that copy. The cleartext key lives only on the login/header request thread.
  - `renewToken` uses `/auth/refresh`; it re-exchanges the application key only when one is present (header mode / login thread). Without a key, renewal failure surfaces as `NoValidTokenException`.
  - Background workers own a private token copy; renewed access/refresh tokens are written back to the session together with the next generation-checked context replacement.
  - Header-mode contexts stored by `ApiKeySessionAuthentication` are stripped the same way.
- **Rationale**: Satisfies Q9/`FR-010`/`FR-018`; minimizes secret lifetime; reuses the existing digest.
- **Alternatives considered**: encrypting the key in session (still reversible, needs key management); keeping it only in the worker closure for the initial sync (inconsistent behavior between initial sync and manual reload).

## D10 — Spring Security CSRF and CORS for Browser UI

- **Decision**:
  - Re-enable CSRF in `torgCodexSecurityFilterChain` with `HttpSessionCsrfTokenRepository` and the default `XorCsrfTokenRequestAttributeHandler` (BREACH-safe, accepts header or `_csrf` parameter); `csrf.ignoringRequestMatchers("/api/**")`. Covers `POST /login`, `POST /logout`, `POST /library/reload`, `POST /notifications/ack`, `POST /admin/notifications/ack` and all future `/admin` forms. `CsrfLogoutHandler` (auto) clears the token on logout; `CsrfAuthenticationStrategy` rotates it on login.
  - Templates: hidden `_csrf` input in every form; `<meta name="_csrf">`/`<meta name="_csrf_header">` rendered only where state-changing controls exist (login/logout pages, signed-in reader layout, admin layout). Deferred token loading keeps anonymous catalog browsing session-free. `/js/csrf.js` adds the header on `htmx:configRequest`; `notifications.js` adds it to ack `fetch` calls. A `CsrfViewModel` (parameter name, header name, token) is passed through the layout view models.
  - CORS: `http.cors(...)` with a `CorsConfigurationSource` bean that registers a restrictive `CorsConfiguration` for all browser UI paths (`/**` except `/api/**` and `/actuator/**`), allowed origins from `torg.codex.web.cors.allowed-origins` (default empty → same-origin only). `DefaultCorsProcessor` rejects cross-origin requests from disallowed origins with `403`. `/api/**` CORS behavior is unchanged. Deployments behind a TLS proxy need `server.forward-headers-strategy: framework` so same-origin detection is correct.
  - Rejections return `403` without mutating session, cache or notification state; logged by D6 at `INFO` completion (no `WARN`).
- **Rationale**: Spring Boot/Security built-ins, no custom token code; FR-019.
- **Alternatives considered**: `CookieCsrfTokenRepository.withHttpOnlyFalse()` (exposes token to scripts, extra cookie, unnecessary for server-rendered pages); `SameSite` cookies only (not a complete CSRF defense, no `403` contract); custom Origin-check filter (re-implements framework features).
