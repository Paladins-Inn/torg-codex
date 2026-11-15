# Contract: Web UI Routes, Hypermedia Fragments, and Theme Surfaces

**Feature**: `006-codex-web-ui` | **Applies to**: FR-001–FR-016, FR-019 | **ADRs**: ADR-019, ADR-020, ADR-021

## 1. Surface, Security, and Namespace Separation

| Surface | URL Prefix | Auth Mechanism | Controller Package | JTE Template Root | Stylesheet | Status in `006` |
|---|---|---|---|---|---|---|
| REST API | `/api/**` | Optional `Authorization: ApiKey <key>` or session | `de.paladinsinn.torg.codex.api.controller` | *(JSON only)* | — | Unchanged (`catalog-api.openapi.yaml` remains authoritative) |
| Reader Codex UI | `/`, `/<category>/**`, `/login`, `/logout` | Optional session via **DriveThruRPG API Key** (`/login`) | `de.paladinsinn.torg.codex.web.codex` | `src/main/jte/codex/` | `/themes/codex/codex.css` | Implemented |
| Admin UI (Reserved) | `/admin`, `/admin/**` | Reserved for separate OIDC/RBAC chain (`ADR-007`) | `de.paladinsinn.torg.codex.web.admin` | `src/main/jte/admin/` | `/themes/admin/admin.css` | Layout/theme shell & namespace reserved; CRUD screens out of scope |

Static assets served locally without external CDN calls:
- `/webjars/bootstrap/**` (Bootstrap 5 CSS & vanilla JS bundle)
- `/webjars/bootstrap-icons/**` (Bootstrap Icons CSS & fonts)
- `/webjars/htmx.org/**` (htmx 2.x)
- `/themes/codex/codex.css` and `/themes/admin/admin.css`

---

## 2. Reader UI HTTP Endpoints

### 2.1 Landing Overview — `GET /`

- **Auth**: Optional (anonymous = `core-rulebook`; authenticated via DriveThruRPG API key = owned products + `core-rulebook`).
- **Status**: `200 OK`, `Content-Type: text/html;charset=UTF-8`, `Cache-Control: private, no-store`.
- **Content**:
  - Global reader navigation bar (`codex/layout/navbar.jte`) with all 17 categories grouped under *Storm Knights*, *Gear & Vehicles*, *Game Master*, and *Reference*, plus authentication status badge and link to `/login` (anonymous) or `/logout` (authenticated). **Zero inline credential `<input>` elements.**
  - Category overview cards showing strictly the caller's accessible entry count per category.
  - Ownership progress bar and "Missing Publications" / "Owned Publications" discovery cards with DriveThruRPG links.

### 2.2 Category Tabular View — `GET /{category}` (and alias `/perk-groups` → `302` to `/groups`)

- **Path `{category}`**: One of the 17 canonical `CatalogArea` slugs (`cosms`, `races`, `perks`, `groups`, `spells`, `spell-lists`, `miracles`, `miracle-lists`, `powers`, `power-lists`, `items`, `vehicles`, `shards`, `threats`, `publications`, `articles`, `tags`). Reserved prefixes (`admin`, `api`, `login`, `logout`, `webjars`, `themes`, `actuator`) never match `{category}`; unknown `{category}` returns `404 Not Found` with `codex/pages/notFound.jte`.
- **Query Parameters** (all optional, repeatable for multi-select facets):
  - `q`: free-text substring search on entry name
  - `cosm`: one or more cosm slugs (e.g., `cosm=aysle&cosm=core-earth`)
  - `clearance`: `alpha` | `beta` | `gamma` | `delta` | `omega`
  - `group`: perk group slug(s)
  - `list`: spell/miracle/power list slug(s)
  - `type`: item type or vehicle type key(s)
  - `race`: race slug(s)
  - `tag`: tag slug(s)
  - `sort`: column key (default `name`)
  - `dir`: `asc` (default) | `desc`
- **Response Headers**:
  - `Content-Type: text/html;charset=UTF-8`
  - `Vary: HX-Request` *(mandatory so browser cache separates full HTML documents from htmx fragments)*
  - `Cache-Control: private, no-store` *(prevents cross-session DRM caching)*
- **Rendering Modes**:
  - **Full Page** (`HX-Request` absent, or `HX-Target != "catalog-results"`): `200 OK` rendering `codex/pages/category.jte` (filter panel + `#catalog-results` table container inside `codex/layout/page.jte`).
  - **htmx Partial Update** (`HX-Request: true` and `HX-Target: catalog-results`): `200 OK` rendering `codex/fragments/categoryResults.jte` (matching count, active filter reset link, sortable `<thead>`, and `<tbody>` rows or empty-state row). Filter form uses `hx-get="/{category}"`, `hx-target="#catalog-results"`, `hx-push-url="true"`.
- **DRM Guarantees (`FR-009`, `FR-010`, `FR-012`)**:
  - Only granted entries appear in `<tbody>` and counts.
  - Zero teaser rows and zero hidden `data-*` or JSON attributes for unowned or product-less entries.

### 2.3 Single-Element Detail / Teaser / Not-Found View — `GET /{category}/{identifier}`

- **Path `{identifier}`**: Either the entry's canonical human-readable slug (e.g., `/perks/ace-pilot`, `/groups/prowess`, `/cosms/aysle`, `/articles/changelog`, `/publications/core-rulebook`) or its 36-char UUID (`/perks/550e8400-...`).
- **Response Headers**: `Content-Type: text/html;charset=UTF-8`, `Cache-Control: private, no-store`.
- **Outcomes**:
  1. **`200 OK` — Full Detail (`codex/pages/detail.jte`)**: Returned when the entry is an accessible `ProductBoundEntry` (`DRM_GRANTED`) or an existing public reference entry (`PUBLIC_REFERENCE`). Displays metadata-driven attribute/stat sections, `Censor`-rendered & sanitized rich-text prose (`<IF:…>` blocks applied for caller's effective `OwnedProducts`, `<type:slug>` cross-references linked to UI routes), DRM-filtered embedded entry links, and source publication links.
  2. **`403 Forbidden` — Purchase Teaser (`codex/pages/teaser.jte`)**: Returned when a `ProductBoundEntry` exists with non-empty `products`, and the caller owns none of them (`TEASER`). Displays **only** the entry's `name`, its `CatalogArea` display label, and the unlocking publication(s) (`codexId` + publication title/cover/DriveThruRPG purchase link when known). Contains **zero** statistics, attributes, clearance, cosms, or prose.
  3. **`404 Not Found` — Styled Not Found (`codex/pages/notFound.jte`)**: Returned when no entry matches `{identifier}` OR when the matching `ProductBoundEntry` has no assigned products (`UNASSIGNED`, fail-closed identical to missing).

### 2.4 Dedicated Non-Admin Authentication Pages (`FR-014`, `SC-006`)

- **`GET /login` → `200 OK` (`codex/pages/login.jte`)**:
  - Dedicated reader-themed sign-in page exclusively for **DriveThruRPG API Key** login (`POST /login`, `name="apiKey"`, `type="password"`), with a help link to DriveThruRPG account settings (`https://www.drivethrurpg.com/account_edit.php`), optional signed-out info banner (`?logout`), and validation error alert on invalid key.
- **`POST /login`**:
  - Authenticates `ApiKeyAuthenticationToken.unauthenticated(apiKey)` through `AuthenticationManager` (note: conflicting `HttpSecurity.formLogin(...)` is removed from `TorgCodexSecurityConfig` so `UsernamePasswordAuthenticationFilter` does not intercept `POST /login`).
  - Valid key: stores `SecurityContext` in `HttpSession`, logs completion at `INFO` (never logging the key), redirects `302 Found` to `/`.
  - Invalid/blank key: logs domain rejection at `INFO` (`ADR-022`), returns `401 Unauthorized` rendering `codex/pages/login.jte` with a user-friendly error message.
- **`GET /logout` → `200 OK` (`codex/pages/logout.jte`)**:
  - Dedicated reader-themed sign-out page displaying current session status and a `POST /logout` confirmation button plus a Cancel link to `/`.
- **`POST /logout`**:
  - Invalidates `HttpSession`, clears `SecurityContext`, logs sign-out at `INFO`, and redirects `302 Found` to `/login?logout`.
