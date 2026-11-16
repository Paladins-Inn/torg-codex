# Quickstart & Validation Guide: Asynchronous Library Login, User Notifications, Caching, and Request Observability

**Feature**: `007-async-library-login` | **Date**: 2026-11-16 (revised) | **Contracts**: [contracts/](./contracts/) | **Data Model**: [data-model.md](./data-model.md)

## 1. Prerequisites

- JDK 25 and the repository wrapper `./mvnw`.
- Docker (or rootless Podman socket) for Testcontainers-backed tests.

```bash
# rootless Podman only:
export DOCKER_HOST="unix:///run/user/$(id -u)/podman/podman.sock"
export TESTCONTAINERS_RYUK_DISABLED=true
```

## 2. Automated Validation

### 2.1 Application module (no Spring context, no Docker)

```bash
./mvnw -pl torg-codex-application -am test
```

Covers `UserNotificationService` (non-destructive pending, channel-scoped ack), `ApiKeyFingerprint`, `DrivethruToken#sessionCopy`, refresh-only renewal, `DriveThruRPGService` batch streaming and `DriveThruLibraryCacheService`/`LibraryLoadCoordinator` with real cache proxies.

### 2.2 Web, security, caching and observability (`torg-codex`, Docker)

```bash
./mvnw -pl torg-codex -am \
  -Dtest='DriveThruRPGServiceTest,DriveThruAuthenticationProviderTest,GenerationAwareSecurityContextRepositoryTest,DriveThruLibrarySynchronizerTest,AsyncLibraryAccessTest,AsyncLibraryHeaderAuthenticationTest,UserNotificationDeliveryTest,CodexNotificationControllerTest,AdminNotificationControllerTest,ToastBrowserTest,DriveThruLibraryCacheServiceTest,DriveThruLibraryCacheConcurrencyTest,CachedLibraryLoginTest,LibraryReloadTest,CodexAuthControllerTest,ApiKeySessionAuthenticationTest,CsrfCorsProtectionTest,WebPageMetricsTest,UserRequestLoggingFilterTest,UserRequestTelemetryTest,StructuredLoggingTest,*ArchitectureTest*' \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: WireMock (annotation + `src/test/resources/mappings` / `__files`) multi-batch login unlocks incrementally; cache hit after logout issues zero `/order_products` calls; reload with an expired refresh token keeps entitlements and yields a sticky re-sign-in toast; POSTs without CSRF token or from a foreign `Origin` return `403` without side effects; one `torg.codex.web.page` series per page incl. `logout-submit`; `DEBUG`/`INFO` boundary logs and `TRACE` for SSE stream/ack; no secrets in logs or tags.

### 2.3 DRM regression

```bash
./mvnw -pl torg-codex -am -Dtest='CatalogDrmDifferentialTest,CatalogOpenApiContractTest,CatalogDrmArchitectureTest*' -Dsurefire.failIfNoSpecifiedTests=false test
```

### 2.4 Full reactor

```bash
./mvnw clean verify
```

## 3. Manual End-to-End Scenarios

```bash
./mvnw -pl torg-codex -am -DskipTests install
./mvnw -pl torg-codex spring-boot:run
# optional: LOGGING_LEVEL_DE_PALADINSINN_TORG_CODEX_API_OBSERVABILITY=TRACE
```

| # | Scenario | Expected outcome |
|---|---|---|
| 1 | Sign in at `/login` with a valid key (cold cache) | Redirect `< 3 s`; free tier visible; `INFO` multi-minute toast (`SC-001`, `SC-003`) |
| 2 | Browse while syncing; keep a page open | Batches unlock on next view; `SUCCESS` toast `≤ 5 s` after completion, page not replaced; **Reload page** refreshes (`SC-002`, `FR-007`) |
| 3 | Sign out, sign in again (same key, `< 12 h`) | Logout reverts to free tier; re-login unlocks instantly with single `SUCCESS` "Publications Ready" toast; no `/order_products` call (`SC-004`) |
| 4 | Click **Reload Library** in the navbar | `INFO` toast in place; completion toast later; cache entry replaced (`FR-018`) |
| 5 | Reload after the refresh token expired | Entitlements unchanged; sticky `ERROR` "Sign-in Required" with **Sign in again** link |
| 6 | Inspect session contents (debugger/test) | Only fingerprint + token copy without application key; no cleartext API key |
| 7 | `curl -X POST localhost:8080/library/reload` (no token) and with `-H 'Origin: https://evil.example'` | `403`, no state change; `curl -H 'Authorization: ApiKey …' localhost:8080/api/…` unaffected |
| 8 | `GET /actuator/prometheus` | `torg_codex_web_page_seconds_count{page=…}` for all eight pages incl. `logout-submit`; tag keys `class,exception,method,page` only |
| 9 | Inspect structured console logs (or `logs/torg-codex.log` with `--spring.profiles.active=dev`) | `user-request-started` (`DEBUG`) / `user-request-completed` (`INFO`) for user requests; SSE stream/ack only at `TRACE`; none for assets/actuator; no file logs in default production profile; no keys, tokens, raw paths or `ROLE_` values |
| 10 | Interrupt a toast SSE stream/ack and navigate | Pending toast replayed once and deduplicated; admin layout shows all four severities in its own theme via a test-only Spring Event producer |
