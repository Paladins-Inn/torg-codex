# Phase 1 Data Model: Asynchronous Library Login, User Notifications, Caching, and Request Observability

**Feature**: `007-async-library-login` | **Date**: 2026-11-16 (revised) | **Spec**: [spec.md](./spec.md) | **Research**: [research.md](./research.md)

**Database impact**: **none.** All structures are volatile runtime, session, cache or view-model values. No JPA entity or Liquibase changeset changes (`ddl-auto: validate` unchanged).

---

## 1. Generic User Notification Model (`de.paladinsinn.torg.codex.application.port.in`)

Framework-free; usable by any producer on both reader and `/admin` surfaces.

### 1.1 `NotificationSeverity` (enum)

| Value | `autoDismiss()` | UI behavior | Usage |
|---|---|---|---|
| `INFO` | `true` | 6000 ms, manually closeable | Background task started (`FR-006`) |
| `SUCCESS` | `true` | 6000 ms, manually closeable | Sync complete, cache-hit ready (`FR-006`, `FR-007`) |
| `WARNING` | `false` | Sticky until closed | Degraded non-fatal conditions |
| `ERROR` | `false` | Sticky until closed | Auth failure, sync failure, re-authentication required (`FR-008`, `FR-009`, `FR-018`) |

### 1.2 `NotificationActionKind` (enum) and `NotificationAction` (record)

| Field | Type | Rules |
|---|---|---|
| `label` | `String` | Non-blank, ≤ 40 chars (e.g. `"Reload page"`, `"Sign in again"`) |
| `kind` | `NotificationActionKind` | `RELOAD_CURRENT_PAGE` or `NAVIGATE_URL` |
| `targetPath` | `String` (nullable) | `null` for `RELOAD_CURRENT_PAGE`; for `NAVIGATE_URL` a relative path starting with a single `/` (no scheme, no `//`) |

### 1.3 `UserNotification` (record)

| Field | Type | Rules |
|---|---|---|
| `id` | `UUID` | Generated at creation |
| `severity` | `NotificationSeverity` | Non-null |
| `title` | `String` | Non-blank plain text |
| `message` | `String` | Non-blank plain text; never secrets, raw upstream payloads or stack traces |
| `createdAt` | `Instant` | Non-null |
| `action` | `NotificationAction` (nullable) | Optional |

### 1.4 `NotificationTarget` (sealed interface) and `UserNotificationRequested` (record, integration event payload)

`NotificationTarget` (`SessionChannel(channelId)` or `AllAuthenticatedUsers()` — the latter reserved for future `/admin` broadcast announcements) + notification content, published via the Spring Events bridge and consumed by an adapter that calls `UserNotificationCommand#dispatch(...)`. Framework-free values; the bridge listener lives outside `application..` (`ADR-024`).

**Store invariants (`UserNotificationService` / `UserNotificationStorePort`)**:
- Keyed by opaque `channelId` (session attribute `torg.codex.notificationChannelId`), derived only from the session.
- Signed-in sessions register their `channelId` via `registerAuthenticatedChannel(channelId)`; anonymous sessions are excluded from `authenticatedChannels()` so `AllAuthenticatedUsers` broadcasts reach only logged-in users, minting an independent UUID per recipient channel.
- `pending(channelId)` returns ≤ 20 oldest items **without removing** them; `acknowledge(channelId, ids)` removes only IDs in that channel; unknown IDs are ignored (idempotent).
- No TTL or cap eviction of undisplayed items in a live session; `clear(channelId)` on logout and session destruction removes both pending items and authenticated-channel registration.

---

## 2. API-Key Digest, Library Batch and Cache Model (`de.paladinsinn.drivethru`)

### 2.1 `ApiKeyFingerprint` (record)

| Field | Type | Rules |
|---|---|---|
| `value` | `String` | 64-char lowercase hex SHA-256 of `apiKey.strip()` (UTF-8); `toString()` returns a redacted form |

Factory `ApiKeyFingerprint.of(String apiKey)`; constant-time `equals` comparison against the session value. Single source for the session digest and the cache key (`FR-010`, `FR-018`).

### 2.2 `DriveThruLibraryBatch` (record)

| Field | Type | Rules |
|---|---|---|
| `pageNumber` | `int` | `>= 1` |
| `productIds` | `List<Integer>` | Immutable, positive IDs from the page |
| `customerId` | `String` (nullable) | First non-null customer ID on the page |

### 2.3 `DriveThruLibrarySnapshot` (record) — cache value

| Field | Type | Rules |
|---|---|---|
| `productIds` | `List<Integer>` | Immutable, deterministically ordered, de-duplicated; empty for empty libraries |
| `customerId` | `String` (nullable) | `null` for empty libraries |
| `loadedAt` | `Instant` | Completion time |

**Cache entry invariants (`"drivethru.library"`)**:
- Key: `ApiKeyFingerprint.value()` via `libraryCacheKeyGenerator`; raw keys never enter the cache, logs, metrics or Actuator views.
- Value never contains tokens.
- Caffeine `expireAfterWrite = 12h` (configurable), `maximumSize = 1000` (configurable).
- Written only on complete success (`@Cacheable`/`@CachePut` normal return); failed, aborted or superseded runs throw and write nothing (`FR-012`).
- Never evicted by logout (`FR-011`); replaced only by a successful reload or expiry/size eviction.

### 2.4 Session-safe `DrivethruToken`

`DrivethruToken#sessionCopy()` → same access token, refresh token, refresh TTL, customer ID and expiry; `applicationKey == null`. Only session copies are stored in `DriveThruUserDetails` (D9). Renewal without an application key is refresh-only.

---

## 3. Session Synchronization State (`de.paladinsinn.security`)

### 3.1 `SyncStatus` (enum) and `DriveThruSessionSyncState` (record)

Stored in `HttpSession` under `DriveThruLibrarySynchronizer.SYNC_STATE_KEY`, mutated only under `WebUtils.getSessionMutex(session)`.

| Field | Type | Rules |
|---|---|---|
| `syncGenerationId` | `UUID` | Current login/reload run; a newer value supersedes older workers |
| `status` | `SyncStatus` | `IDLE`, `RUNNING`, `COMPLETED`, `FAILED`, `REAUTH_REQUIRED` |
| `startedAt` | `Instant` | Run start |
| `appliedBatches` | `int` | `>= 0`; monotonically increasing per generation (used by the generation-aware context repository) |

Session attributes used together: `ApiKeySessionAuthentication…keyFingerprint` (`ApiKeyFingerprint`), `torg.codex.notificationChannelId`, `SPRING_SECURITY_CONTEXT`, sync state, CSRF token.

### 3.2 State transitions

```text
[Anonymous]
  ├─ POST /login, CSRF/CORS invalid ─────────────► 403, nothing mutated
  ├─ POST /login, invalid key / 503 ─────────────► [Anonymous] + request-scoped ERROR toast
  ├─ POST /login, valid token + cache hit ───────► [IDLE, full entitlements] + SUCCESS "Publications Ready"
  └─ POST /login, valid token + cache miss ──────► [RUNNING, free tier] + INFO start toast
                                                     (session ID + CSRF token rotated)
[RUNNING]
  ├─ batch applied (generation matches) ─────────► [RUNNING] union of codex IDs, appliedBatches++
  ├─ all batches done ───────────────────────────► @CachePut snapshot → [COMPLETED] + SUCCESS toast (Reload page)
  ├─ upstream failure ───────────────────────────► keep applied batches, no cache write → [FAILED] + sticky ERROR
  ├─ token not renewable (refresh-only) ─────────► keep entitlements & cache → [REAUTH_REQUIRED] + sticky ERROR (Sign in again → /login)
  ├─ POST /logout ───────────────────────────────► session invalidated, channel cleared, CSRF token cleared;
  │                                                worker sees invalid session / mismatch and stops; cache untouched
  └─ newer login / reload ───────────────────────► new generation; old worker results discarded

[Signed in: IDLE | COMPLETED | FAILED | REAUTH_REQUIRED | RUNNING]
  └─ POST /library/reload (valid CSRF) ──────────► new generation, keep entitlements, INFO start toast → [RUNNING]
```

---

## 4. Web Presentation Models (`de.paladinsinn.torg.codex.web.notification`)

Shared by `codex/…` and `admin/…` templates without `web.codex` ↔ `web.admin` dependencies. Records have at most seven components; mapped with a Spring MapStruct mapper using `TorgMappingSupport`.

### 4.1 `ToastViewModel` = `ToastContent` + `ToastPresentation` + `ToastActionView`

| Component | Fields |
|---|---|
| `ToastContent` | `id` (`"toast-" + uuid`), `notificationId` (UUID string for ack), `title`, `message` |
| `ToastPresentation` | `severityKey` (`info`/`success`/`warning`/`error`), `iconCss`, `ariaRole` (`status`/`alert`), `ariaLive` (`polite`/`assertive`), `autoHide`, `delayMs` (6000 or 0) |
| `ToastActionView` (nullable) | `label`, `kind` (`reload`/`navigate`), `targetPath` (validated internal path or `null`) |

### 4.2 `ToastRegionViewModel`

| Field | Type | Description |
|---|---|---|
| `initialToasts` | `List<ToastViewModel>` | Pending items read non-destructively during SSR, plus request-scoped login errors |
| `streamEndpoint` | `String` (nullable) | `/notifications/stream`, `/admin/notifications/stream`, or `null` when no channel exists |
| `ackEndpoint` | `String` (nullable) | Matching `…/notifications/ack` |

### 4.3 `CsrfViewModel`

| Field | Type | Description |
|---|---|---|
| `parameterName` | `String` | Form field name (default `_csrf`) |
| `headerName` | `String` | Header name (default `X-CSRF-TOKEN`) |
| `token` | `String` | Masked per-request token value (escaped by JTE) |

Supplied only for views with state-changing controls (login/logout pages, signed-in reader layout, admin layout) so anonymous catalog views do not create sessions.

---

## 5. Telemetry Records (no persistence)

| Record | Content |
|---|---|
| Page timer `torg.codex.web.page` | Tags `page`, `class`, `method`, `exception` — bounded values only ([observability contract](./contracts/observability-and-logging.md)) |
| Request boundary log | `event.action`, `http.request.method`, `http.route`, `http.response.status_code`, `durationMs`, trace/span IDs |
