# Contract: Web UI Routes, Asynchronous Login, Library Reload, Toast Notifications, CSRF and CORS

**Feature**: `007-async-library-login` | **Date**: 2026-11-16 (revised) | **Extends**: `specs/006-codex-web-ui/contracts/web-ui-routes.md`

---

## 0. Cross-Cutting Browser Protection (`FR-019`)

| Aspect | Rule |
|---|---|
| CSRF scope | Every state-changing browser request on the reader and `/admin` surfaces: `POST /login`, `POST /logout`, `POST /library/reload`, `POST /notifications/ack`, `POST /admin/notifications/ack`, and future `/admin` forms |
| Token transport | Hidden form field `${csrf.parameterName()}` (default `_csrf`) or header `${csrf.headerName()}` (default `X-CSRF-TOKEN`) for htmx/`fetch` |
| Missing/invalid token | `403 Forbidden`; no session, cache, entitlement or notification mutation |
| Exempt | `/api/**` (stateless `Authorization: ApiKey` REST); safe methods (`GET`, `HEAD`, `OPTIONS`) |
| Token lifecycle | Rotated on successful sign-in, cleared on sign-out |
| CORS (UI paths) | Cross-origin requests whose `Origin` is not in `torg.codex.web.cors.allowed-origins` (default empty) → `403 Forbidden`; same-origin requests unaffected |
| CORS (`/api/**`, `/actuator/**`) | Unchanged by this feature |
| Session creation | Anonymous catalog `GET`s must not create a session; only views with state-changing controls request a token |

---

## 1. Route Namespace and Reserved Prefix Guard

`CodexCatalogController` category regex excludes the new prefixes:

```text
^(?!api$|admin$|login$|logout$|library$|notifications$|js$|webjars$|themes$|actuator$|static$|error$)[a-zA-Z0-9-]+$
```

---

## 2. Authentication and Library Synchronization (`web.codex`)

### 2.1 `GET /login`

Renders `codex/pages/login` with a `CsrfViewModel`; the form contains the hidden CSRF field. Creates a session for the token (expected: sign-in is about to create one).

### 2.2 `POST /login` (`FR-001`–`FR-008`, `SC-001`)

- **Request**: `application/x-www-form-urlencoded`: `apiKey`, `_csrf`.
- **Outcomes**:

| Case | Status | Session | Toast |
|---|---|---|---|
| Invalid/missing CSRF token or disallowed origin | `403` | unchanged | none |
| Blank/invalid key | `401`, `codex/pages/login`, `Cache-Control: private, no-store` | none established | request-scoped sticky `ERROR` "Authentication Failed" / "Invalid DriveThruRPG API key. Please check your Application Key and try again." |
| Token service unavailable | `503`, same view | none established | sticky `ERROR` "Service Unavailable" / "DriveThruRPG is temporarily unavailable. Please try again later." |
| Valid token, cache hit | `302 → /` | session ID + CSRF token rotated; fingerprint, channel, full context | one `SUCCESS` "Publications Ready" / "You are signed in and your DriveThruRPG publications are unlocked." |
| Valid token, cache miss | `302 → /` (before any `/order_products` call completes) | rotated; fingerprint, channel, free-tier context, `RUNNING` generation | `INFO` "Library Sync Started" / "You are signed in. Loading your purchased publications from DriveThruRPG in the background—this could take several minutes until all publications become available." |

- **Background (cache miss)**:
  - Each batch atomically unions codex IDs into the session context (`FR-004`, `SC-002`).
  - Success → cache write, `SUCCESS` "Library Sync Complete" / "All purchased DriveThruRPG publications have been loaded and are now available." with action `RELOAD_CURRENT_PAGE` "Reload page" (`FR-007`).
  - Failure → keep applied batches, no cache write, sticky `ERROR` "Library Sync Failed" / "Could not finish loading your DriveThruRPG library. Use Reload Library to retry." (`FR-009`).
- The session never stores the cleartext API key; only `ApiKeyFingerprint` and a session-safe token copy.

### 2.3 `POST /library/reload` (`FR-018`)

- **Trigger**: navbar control rendered when `nav.signedIn()`: `<form method="post" action="/library/reload">` with hidden CSRF field, enhanced with `hx-post`, `hx-target="#codex-toast-container"`, `hx-swap="beforeend"` (header added by `/js/csrf.js`).
- **Outcomes**:

| Case | Response |
|---|---|
| Invalid CSRF / disallowed origin | `403` |
| Anonymous | `302 → /login` (htmx: `200` with `HX-Redirect: /login`) |
| Signed in, plain form | new generation, `INFO` start toast enqueued, `302` to validated same-origin `Referer` path (default `/`) |
| Signed in, htmx | same, `200 text/html` (`Cache-Control: private, no-store`) toast fragment |

- **Background**: bypasses the cached entry, streams batches as in 2.2, replaces the cache entry only on full success.
- **Token not renewable** (refresh missing/expired/rejected, no application key in session): session entitlements and cache unchanged; state `REAUTH_REQUIRED`; sticky `ERROR` "Sign-in Required" / "Your DriveThruRPG session has expired. Please sign in again to reload your library." with action `NAVIGATE_URL` "Sign in again" → `/login`.

### 2.4 `GET /logout` and `POST /logout` (`FR-011`)

- `GET /logout` renders the confirmation page with the hidden CSRF field.
- `POST /logout` (CSRF required): invalidates session and context, clears the CSRF token and the session's notification channel; in-flight workers stop on invalid session/generation mismatch. **Does not** evict `"drivethru.library"`. `302 → /login?logout`. Timed explicitly as `page=logout-submit` (observability contract §1).

---

## 3. Generic Toast Endpoints (`FR-005`)

### 3.1 Endpoints

| Surface | Endpoint | Controller | Responses |
|---|---|---|---|
| Reader | `GET /notifications/stream` | `web.codex.CodexNotificationController` | `204` without session/channel; `200 text/event-stream` (`SseEmitter`) with `Cache-Control: private, no-store`, `X-Accel-Buffering: no`, pushing `event: toast` (`codex/fragments/toasts.jte`) via Spring `@EventListener` |
| Reader | `POST /notifications/ack` | same | `204` |
| Admin | `GET /admin/notifications/stream` | `web.admin.AdminNotificationController` | `204` / `200 text/event-stream` (`SseEmitter`) pushing `event: toast` (`admin/fragments/toasts.jte`) |
| Admin | `POST /admin/notifications/ack` | same | `204` |

- No endpoint creates a session (`getSession(false)`); without a session: `GET` → `204`, `POST` → CSRF `403` (no token can exist) — clients only open SSE streams and ack when a session channel exists.
- `GET …/stream` flushes ≤ 20 currently pending items on initial connect without deleting them, then immediately pushes any subsequent `UserNotificationPublished` Spring event on that channel. Active emitters are completed and removed on logout and session destruction.
- `POST …/ack`: form-encoded repeated `id` (UUID, ≤ 20) + CSRF header/field; channel derived from session; unknown IDs ignored; malformed IDs → `400`.
- Logged at `TRACE` only; not page-timed.

### 3.2 Layout and DOM contract

- **Reader** (`codex/layout/page.jte`): `<div id="codex-toast-container" class="toast-container position-fixed top-0 end-0 p-3 codex-toast-region" …>` with SSR `initialToasts`, plus `data-stream-url="/notifications/stream"` and `data-ack-url="/notifications/ack"` when `streamEndpoint != null` (zero periodic HTTP polling); `<meta name="_csrf" …>`/`<meta name="_csrf_header" …>` when a `CsrfViewModel` is present.
- **Admin** (`admin/layout/page.jte`): `#admin-toast-container` in the admin dark theme, `data-stream-url="/admin/notifications/stream"` and `data-ack-url="/admin/notifications/ack"` when a channel exists, CSRF meta tags always present.
- **Toast element**: `class="toast …"`, `data-notification-id`, `data-toast-init="pending"`, `role`, `aria-live`, `aria-atomic="true"`, `data-bs-autohide`, `data-bs-delay`; close button on all severities; optional `data-toast-action="reload"` button or `<a href="${targetPath}">` link. Text only via auto-escaped `${…}` (no `$unsafe`).
- **Scripts** (self-hosted): `/js/csrf.js` (adds CSRF header in `htmx:configRequest`), `/js/notifications.js` (Bootstrap Toast init, `EventSource` SSE subscription without polling, `sessionStorage` de-duplication, ack via `fetch` with CSRF header and retry, reload action).
