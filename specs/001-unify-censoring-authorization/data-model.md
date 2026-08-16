# Phase 1 Data Model: Unify Product-Ownership Censoring Authorization

This feature makes **no persisted-data-model, JPA entity, or Liquibase schema changes** (FR-010).
There are no new database tables, columns, or migrations. This document instead describes the
runtime **value objects**, **authority-mapping rules**, and their invariants that this feature
introduces or changes, since those are the entities relevant to the spec's Key Entities section
(`Product ownership`, `Censor`, `Gated markup block`).

## 1. Owned-Product Set (conceptual, request-scoped, not persisted)

**Description**: The set of product/codex identifiers the current request's caller is entitled
to see gated content for. Represented uniformly as a `Set<String>` of *codex ids* (the `ROLE_`
prefix already stripped), regardless of which authentication method populated the underlying
`Authentication`.

| Field | Type | Description | Validation / Invariants |
|---|---|---|---|
| `ownedProducts` | `Set<String>` | Codex ids (e.g. `sourcebook-aysle`) the caller owns for this request | Never `null`; empty set means "owns nothing but the free tier's upsell blocks are visible" (an anonymous *`Authentication`-less* caller yields empty — but see below, production anonymous callers always carry at least `ROLE_core-rulebook` per `TorgCodexSecurityConfig`, so in practice the set is never truly empty in this application). Unknown/stale codex ids (no matching `<IF:id>` block anywhere) are permitted in the set and are simply inert (edge case, spec line 65). |

**Source of truth / derivation rule**: For each `GrantedAuthority` on the current
`SecurityContextHolder`'s `Authentication` whose string value starts with the literal prefix
`ROLE_`, strip the prefix; the remainder is a member of `ownedProducts`. Authorities that do not
start with `ROLE_` (e.g. an OAuth2 `SCOPE_read` scope) are ignored. If there is no
`Authentication`, or `Authentication.isAuthenticated()` is `false`, the resulting set is empty.

**Owning component**: `ProductOwnershipResolver` (new, `torg-codex` module, package
`de.paladinsinn.torg.codex.api.security`) — see `contracts/product-ownership-resolver.md`.

**Lifecycle**: Computed fresh on every request inside `CurrentUserCensorFactory.create()`; never
cached or persisted; not shared across requests (aligns with existing `@Component`
request-time-computed behavior — `CurrentUserCensorFactory` is a singleton bean but `create()`
reads the *current* thread's `SecurityContextHolder` state each call, exactly as
`SecuredMarkupService.render(String)` did today).

## 2. `Censor` (existing value type — behavior unchanged, only its construction input changes)

**Description**: Immutable value object (`torg-codex-data`,
`de.paladinsinn.torg.codex.data.markup.Censor`) that pairs a resolved owned-product set with the
`TorgMarkupService` rendering pipeline, and exposes `apply(String rawText): String`. **No
change** to this class's public API, fields, or behavior is required by this feature — the only
change is what `Set<String>` gets passed into `Censor.of(markupService, ownedProducts)` by its
sole production caller, `CurrentUserCensorFactory.create()`.

| Field | Type | Description |
|---|---|---|
| `markupService` | `TorgMarkupService` | The five-stage rendering pipeline (unchanged) |
| `ownedProducts` | `Set<String>` | Defensive copy (`Set.copyOf`) of the owned-product set for this request/response cycle |

**Relationships**: `Censor` is constructed once per request by `CurrentUserCensorFactory.create()`
and passed by MapStruct mappers as an `@Context` parameter to `toDetail(entity, censor)` methods
(e.g. `CosmMapper.toDetail(Cosm, @Context Censor)`), which in turn call
`TorgMappingSupport.censorText(rawText, censor)` / `censorMap(rawMap, censor)` for each raw text
field being rendered.

## 3. Gated Markup Block (existing concept — unchanged)

**Description**: A `<IF:product-id>…</IF>` (owner-only) or `<IF:!product-id>…</IF>` (non-owner
upsell) region embedded in a raw text field (e.g. `Cosm.worldLaws`, `Article.text`). Parsed by
the existing `ConditionalBlockProcessor` (`torg-codex-data`) via the regex
`<IF:(!?)([a-z0-9-]+)>([\s\S]*?)</IF>`. **Not modified by this feature** — FR-006 requires the
rendering pipeline and its ordering to remain byte-for-byte identical; this feature only changes
what `Set<String> ownedProducts` argument reaches `ConditionalBlockProcessor.process(text, ownedProducts)`.

| Field (regex group) | Description |
|---|---|
| negation flag (`!` present?) | If present, block is upsell (visible when product NOT owned); if absent, block is owner-only (visible when product IS owned) |
| `productId` | The codex id the block is gated on; matched by simple set-containment against the resolved owned-product set — unknown/stale ids simply never match (spec edge case, line 65) |
| `content` | Raw text to emit conditionally; itself may contain further pipeline-stage syntax (entity references, raw HTML, game tokens, Markdown) processed in later stages |

## 4. Authority ↔ Product-Id Mapping Rule (the core rule this feature makes universally correct)

| Spring Security `GrantedAuthority` string | Resolved product-id | Populated by |
|---|---|---|
| `ROLE_core-rulebook` | `core-rulebook` | `NotLoggedInUserDetails.getAuthorities()` (anonymous principal, always present per `TorgCodexSecurityConfig.torgCodexSecurityFilterChain`) **and** `DriveThruUserDetails.getAuthorities()` for any authenticated user who owns the free-tier core rulebook |
| `ROLE_<codexId>` (e.g. `ROLE_sourcebook-aysle`) | `<codexId>` (e.g. `sourcebook-aysle`) | `DriveThruUserDetails.getAuthorities()`, one per entry in `getOwnedCodexIds()` — populated at authentication time by `DriveThruAuthenticationProvider` resolving DriveThruRPG-owned product ids to codex ids via the catalog |
| `ROLE_DRIVETHRU_USER` | *(ignored — has `ROLE_` prefix but does not correspond to any `<IF:...>` block in practice; harmless per spec edge case, unmatched ids are simply inert)* | `DriveThruUserDetails.getAuthorities()` |
| Any non-`ROLE_`-prefixed authority (e.g. a future OAuth2 `SCOPE_...` scope) | *(not resolved — filtered out)* | n/a |
| A future OIDC/Keycloak `ROLE_<codexId>` authority, once/if that authentication method is added | `<codexId>` | *(out of scope for this feature — see research.md Decision 4; the resolver already supports this transparently because it only depends on the `Authentication.getAuthorities()` abstraction, not on any concrete principal type)* |

**Invariant this feature restores**: This mapping rule MUST be applied identically regardless of
which concrete `Authentication`/principal implementation populated the `SecurityContext` — this
is precisely constitution Principle V's added single-mechanism rule, and the defect being fixed
is that `CurrentUserCensorFactory` previously bypassed this generic rule in favor of a
DriveThruRPG-principal-specific lookup.
