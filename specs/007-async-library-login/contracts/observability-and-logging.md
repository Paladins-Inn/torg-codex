# Contract: User Page Timing Metric and Request Boundary Logging

**Feature**: `007-async-library-login` | **Date**: 2026-11-16 (revised after Q7/Q8) | **Aligns with**: `ADR-017`, `ADR-022`, `specs/006-codex-web-ui/contracts/observability-and-logging.md`

---

## 1. Single User-Page Timer (`FR-014`, `SC-005`)

- **Meter**: `torg.codex.web.page` (Timer) — the only page metric. No `torg.codex.web.handler` meter.
- **Mechanism**: `@Timed(value = "torg.codex.web.page", extraTags = {"page", "<value>"})` on page handlers, intercepted by the Boot-managed `TimedAspect` (`management.observations.annotations.enabled=true`). Explicit timing only for `POST /logout` via `LogoutTimingFilter` (before `LogoutFilter`).
- **Tag keys** (identical for annotated and explicit timers): `page`, `class`, `method`, `exception` (`none` or exception simple name).
- **Prometheus**: `torg_codex_web_page_seconds_count|_sum|_max{class,exception,method,page}`.

| Handler | `page` | Routes |
|---|---|---|
| `CodexHomeController#home` | `home` | `GET /` |
| `CodexCatalogController#browseCategory` | `category` | `GET /{category}` (page + htmx filter fragment) |
| `CodexCatalogController#viewEntryDetail` | `detail` | `GET /{category}/{identifier}` (granted, teaser `403`, not-found `404`) |
| `CodexAuthController#login` | `login-view` | `GET /login` |
| `CodexAuthController#performLogin` | `login-submit` | `POST /login` |
| `CodexAuthController#logoutPage` | `logout-view` | `GET /logout` |
| `CodexAuthController#reloadLibrary` | `library-reload` | `POST /library/reload` |
| `LogoutTimingFilter` (explicit, `class=LogoutFilter`, `method=logout`) | `logout-submit` | `POST /logout` |

**Rules**
- One recording per handled page request; `CodexExceptionAdvice` is not annotated (no second count).
- Handler timing excludes JTE view rendering after handler return (accepted, Q7); `http.server.requests` remains for full-lifecycle latency.
- Not timed: notification SSE stream/ack, static assets, actuator, `/api/**` (covered by `http.server.requests`), CSRF/CORS rejections before dispatch.
- No raw URI, slug, UUID, query, API key, customer ID or owned-product set in any tag.

---

## 2. Request Boundary Logging (`FR-015`, `FR-016`, `SC-006`)

- **Component**: `api.observability.UserRequestLoggingFilter` (`OncePerRequestFilter`), ordered after `ServerHttpObservationFilter` and before `FilterChainProxy`.
- **Format**: Spring Boot native ECS JSON (`ADR-017`).

### 2.1 Request classes

| Class | Paths | Start | Completion |
|---|---|---|---|
| Excluded | `/actuator/**`, `/webjars/**`, `/themes/**`, `/js/**`, `/css/**`, `/images/**`, `/favicon.ico` | — | — |
| Background control | `/notifications/stream`, `/notifications/ack`, `/admin/notifications/stream`, `/admin/notifications/ack` | `TRACE` | `TRACE` |
| User request | everything else (reader UI, admin UI, `/api/**`) | `DEBUG` | `INFO` |

### 2.2 Fields

| Field | Start | Completion |
|---|---|---|
| `message` | `"User request started."` | `"User request completed."` |
| `event.action` | `user-request-started` (`background-request-started` for control class) | `user-request-completed` (`background-request-completed`) |
| `http.request.method` | ✓ | ✓ |
| `http.route` | bounded classification (`reader-page`, `admin-page`, `api`, `auth`, `library`, `notification`) | `BEST_MATCHING_PATTERN_ATTRIBUTE` or `unmatched` |
| `http.response.status_code` | — | final status |
| `durationMs` | — | elapsed ms |
| `trace.id` / `span.id` | ✓ (MDC) | ✓ |

**Lifecycle**: exactly one completion per logical request; async requests complete via `AsyncListener`; an escaping exception reports `500`; security-terminated requests (logout redirect, CSRF/CORS `403`, header-auth `401`/`503`) are included.

### 2.3 Data protection and severity (`ADR-022`)

- Never logged: raw path, query string, form parameters (`apiKey`, `_csrf`), `Authorization`/`Cookie`/CSRF headers, `JSESSIONID`, DriveThruRPG tokens, API-key fingerprints, `ROLE_<codex-id>` sets.
- Domain/user outcomes (`401`, `403` teaser or CSRF rejection, `404`) complete at `INFO`, never `WARN`/`ERROR`.
