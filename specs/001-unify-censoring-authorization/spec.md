# Feature Specification: Unify Product-Ownership Censoring Authorization

**Feature Branch**: `001-unify-censoring-authorization`

**Created**: 2026-08-16

**Status**: Draft

**Input**: User description: "Consolidate product-ownership censoring authorization onto a single ROLE_<codex-id>-based mechanism, per constitution Principle V (added 2026-08-16). Currently `CurrentUserCensorFactory` (wired into all 17 REST controllers) derives product ownership only from a DriveThruRPG API-key authentication principal (`DriveThruUserDetails`), never from `ROLE_<codex-id>` Spring Security `GrantedAuthority` entries, while a separate, documented-but-unwired class (`SecuredMarkupService`) implements the authority-based approach correctly but is never used by any controller. As a result, product-gated `<IF:product-id>...</IF>` markup never renders differently based on real role-based authorization. This was confirmed empirically: all 17 catalog areas' anonymous-vs-authenticated-owner characterization fixtures are byte-identical, including for content (the 'Aysle' cosm) known to contain real gated blocks that should differ."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Product owner sees full gated content via any supported authentication method (Priority: P1)

A user who owns a TORG Eternity product (e.g., a sourcebook) authenticates to the API — whether via a DriveThruRPG API key or via an OIDC-issued token carrying `ROLE_<codex-id>` authorities for that product — and requests catalog content (e.g., a Cosm's world laws) that contains product-gated markup. The user MUST see the content variant intended for owners of that product (the `<IF:product-id>...</IF>` block), regardless of which supported authentication method was used to establish their identity.

**Why this priority**: This is the core defect. Today, ownership recognized via `ROLE_<codex-id>` authorities (the officially documented mechanism) is silently ignored by the code path actually wired into every controller, so authenticated owners using anything other than the one specific API-key-derived principal type never see the content they are entitled to. This is a correctness and product-value issue (paying customers not receiving the content they paid for) as well as a security-consistency issue (unclear who governs access).

**Independent Test**: Can be fully tested by issuing an authenticated request carrying `ROLE_sourcebook-aysle` (via any principal type the security configuration accepts) against the Aysle cosm detail endpoint and asserting that the response's `worldLaws` field contains the owner-only text and omits the non-owner upsell text.

**Acceptance Scenarios**:

1. **Given** a request authenticated with a principal whose `SecurityContext` carries `ROLE_sourcebook-aysle`, **When** the client requests the Aysle cosm detail endpoint, **Then** the response's `worldLaws` field contains the content from the corresponding `<IF:sourcebook-aysle>...</IF>` block and does not contain the content from the corresponding `<IF:!sourcebook-aysle>...</IF>` block.
2. **Given** an anonymous (unauthenticated) request, **When** the client requests the Aysle cosm detail endpoint, **Then** the response's `worldLaws` field contains the content from the `<IF:!sourcebook-aysle>...</IF>` block (the non-owner upsell notice) and does not contain the owner-only block's content.
3. **Given** the two requests above, **When** their response bodies are compared, **Then** they MUST differ in the gated field(s).

---

### User Story 2 - Single, unambiguous censoring authorization mechanism for maintainers (Priority: P2)

A maintainer extending or auditing the codebase needs to be able to find exactly one place that decides "does this request's user own product X" for the purpose of content censoring, so that security-relevant behavior is easy to reason about, test, and change safely.

**Why this priority**: Today there are two independent implementations (`CurrentUserCensorFactory`/`Censor`, and the unwired `SecuredMarkupService`) that could each plausibly be "the" mechanism, and only one of them is actually exercised by production request handling. This ambiguity is exactly the condition that allowed the User Story 1 defect to go undetected through a full architecture migration. Consolidating to one mechanism removes the ambiguity and the duplicate/dead code.

**Independent Test**: Can be fully tested by inspecting the dependency graph of every REST controller and confirming exactly one component type is responsible for resolving product ownership for censoring, and that no other component in the codebase independently re-implements that decision.

**Acceptance Scenarios**:

1. **Given** the consolidated implementation, **When** any of the 17 catalog controllers renders censored content, **Then** it resolves ownership through the single authorized mechanism only.
2. **Given** the consolidated implementation, **When** the codebase is searched for components that derive product ownership from the `SecurityContext` or an authentication principal, **Then** exactly one such component is found (plus its narrowly-scoped internal collaborators, e.g. the markup rendering pipeline it delegates to).
3. **Given** the prior duplicate/unwired implementation, **When** the consolidation is complete, **Then** the duplicate is either removed or reduced to a thin delegation to the single authorized mechanism (no independent ownership-resolution logic remains).

---

### User Story 3 - Regression-proof test coverage for role-based censoring (Priority: P1)

A future contributor changes authentication, security configuration, or the censoring/mapping code. Before their change can be merged, the automated test suite MUST be able to catch it if role-based, product-gated content censoring stops working correctly for any of the 17 catalog areas that carry gated content in their fixture/seed data.

**Why this priority**: The existing characterization test suite (built during the Hexagonal Architecture Migration) proved migration equivalence but did not — and structurally could not, given its auth-variant setup — detect that role-based censoring was non-functional. Without a test that actually distinguishes ownership states for known gated content, this class of regression can silently reoccur or persist indefinitely.

**Independent Test**: Can be fully tested by intentionally reintroducing the current defect (e.g., reverting the single-mechanism consolidation) in a local branch and confirming that at least one automated test fails as a result.

**Acceptance Scenarios**:

1. **Given** the Aysle cosm's `world_laws` field containing real `<IF:sourcebook-aysle>`/`<IF:!sourcebook-aysle>` gated markup, **When** the automated test suite runs, **Then** at least one test asserts that the rendered output differs between an anonymous request and a request authenticated as an owner of `sourcebook-aysle`.
2. **Given** the existing characterization fixture pairs (`anonymous-detail.json` / `owner-detail.json`) for every catalog area whose underlying seed data contains gated markup, **When** the test suite runs, **Then** it fails if the two fixtures' bodies are byte-identical for any field known to contain gated content.
3. **Given** a hypothetical regression that reintroduces the User Story 1 defect, **When** the full test suite (`./mvnw clean verify`) runs, **Then** the build fails.

---

### Edge Cases

- What happens when a request is authenticated but the principal carries no `ROLE_<codex-id>` authorities at all (e.g., a bare `ROLE_DRIVETHRU_USER` with zero owned products)? The system MUST treat this identically to an anonymous/free-tier request for censoring purposes (only the free-tier product's content and upsell blocks visible).
- What happens for content that has no product-gated markup at all? Output MUST be identical regardless of authentication state (no observable behavior change for non-gated content).
- What happens if a request carries `ROLE_<codex-id>` authorities for a product that does not exist in the catalog (e.g., a stale or mistyped role)? The system MUST ignore the unknown product id for ownership purposes without erroring (matches current `<IF:id>` matching semantics: unmatched ids simply do not grant visibility).
- What happens for a request authenticated via OIDC/Keycloak (as opposed to a DriveThruRPG API key) that also carries `ROLE_<codex-id>` authorities? The system MUST recognize this ownership identically to an API-key-authenticated request with the same authorities, since the mechanism is principal-type-independent per the constitution's Principle V requirement.
- What happens to the previously-unwired `SecuredMarkupService`/duplicate logic during consolidation? It MUST be either removed or reduced to a thin delegation to the single authorized mechanism; no independent, divergent ownership-resolution logic may remain anywhere in the codebase.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST derive product ownership for content-censoring purposes exclusively from `ROLE_<codex-id>` Spring Security `GrantedAuthority` entries present in the current request's `SecurityContext`, independent of the authentication method (DriveThruRPG API key, OIDC/Keycloak token, or any other currently or future supported method) used to establish that context.
- **FR-002**: There MUST be exactly one production code path/component responsible for resolving product ownership for censoring across the entire application; no second, independently-implemented ownership-resolution mechanism may exist in parallel.
- **FR-003**: All 17 existing catalog-area REST controllers (Article, Cosm, Item, Miracle, MiracleList, Perk, PerkGroup, Power, PowerList, Publication, Race, Shard, Spell, SpellList, Tag, Threat, Vehicle) MUST resolve censoring/product-gated content exclusively through the single authorized mechanism (FR-001/FR-002).
- **FR-004**: Requests authenticated via any supported method whose `SecurityContext` carries `ROLE_<codex-id>` for a given product MUST see that product's owner-only gated content (`<IF:id>...</IF>` blocks) and MUST NOT see that product's non-owner upsell content (`<IF:!id>...</IF>` blocks) for that same product id.
- **FR-005**: Requests without any `ROLE_<codex-id>` authority for a given product (including fully anonymous requests) MUST see that product's non-owner upsell content (`<IF:!id>...</IF>` blocks) and MUST NOT see that product's owner-only gated content (`<IF:id>...</IF>` blocks).
- **FR-006**: The existing markup rendering order (conditional product blocks -> entity references -> raw HTML -> game tokens -> CommonMark) MUST remain unchanged; this feature changes only how product ownership is determined, not how gated/rendered text is produced once ownership is known.
- **FR-007**: The previously-unwired duplicate ownership-resolution component (`SecuredMarkupService` or equivalent) MUST be removed from production code, or reduced to a thin delegation with no independent ownership-resolution logic, once consolidation is complete.
- **FR-008**: The system MUST add or repair automated test coverage that asserts a genuine difference in rendered API output between an anonymous request and a request authenticated as an owner, for at least one catalog area whose seed/fixture data contains real product-gated markup (e.g., the Aysle cosm).
- **FR-009**: The consolidation MUST NOT change any externally-visible REST API response for requests and content that do not involve product-gated markup (no regression for the non-gated majority of fields/content).
- **FR-010**: The consolidation MUST NOT alter the persisted data model, Liquibase schema, or any non-censoring-related business logic.
- **FR-011**: The fix MUST comply with constitution Principle V (v1.1.0): single ROLE_<codex-id>-based ownership mechanism, no bypassing of censoring, and test coverage that proves differential behavior rather than mere byte-for-byte equivalence across auth variants.

### Key Entities *(include if feature involves data)*

- **Product ownership**: The set of product/codex identifiers a requesting user is entitled to see gated content for, represented uniformly as `ROLE_<codex-id>` Spring Security `GrantedAuthority` entries in the `SecurityContext`, regardless of which authentication method populated them.
- **Censor**: The existing value type that applies product-gated markup rules (via the markup rendering pipeline) to a raw text/map field, given a resolved set of owned product ids. Its responsibility (rendering given ownership) is unchanged; only how the owned-product set reaches it is being consolidated.
- **Gated markup block**: A `<IF:product-id>...</IF>` (owner-only) or `<IF:!product-id>...</IF>` (non-owner upsell) region embedded in a raw text field (e.g., Cosm `world_laws`, Article body, Perk text).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For 100% of the 17 catalog areas whose seed data contains product-gated markup, an authenticated owner and an anonymous request produce measurably different rendered output for the gated field(s), verified by automated tests.
- **SC-002**: Exactly one production component resolves product ownership for censoring, verified by code inspection/architecture test; zero duplicate or unwired parallel implementations remain.
- **SC-003**: 100% of existing REST API responses for non-gated content remain byte-for-byte identical before and after this change (zero unintended regression).
- **SC-004**: The full build (`./mvnw clean verify`) passes, including a newly added or repaired automated test that fails if role-based censoring regresses.
- **SC-005**: Product owners authenticating via any currently supported method receive the content variant appropriate to their actual ownership within the same request/response cycle as before (no added latency-affecting round-trips).

## Assumptions

- The existing `ROLE_<codex-id>` authority-naming convention (one authority per owned product, plus a base authenticated-user role) remains the source of truth for ownership going forward; this feature does not introduce a new ownership representation.
- Both currently supported authentication methods (DriveThruRPG API key and OIDC/Keycloak) are expected to populate `ROLE_<codex-id>` authorities into the `SecurityContext` before request handling reaches any controller; if either method currently does not do so, populating it is treated as in-scope enabling work for this feature (not a separate feature), since FR-001 requires it.
- The free-tier default behavior (anonymous/no-authority requests see only free-tier + upsell content) is unchanged; this feature does not alter what anonymous users can see, only ensures authenticated owners actually receive their entitled content.
- The "Aysle" cosm's real seed data (containing `<IF:sourcebook-aysle>` / `<IF:!sourcebook-aysle>` blocks) remains present in the Liquibase-loaded free-tier/test dataset and can be relied upon as a concrete test fixture for this feature's acceptance tests.
- No new user-facing API surface (new endpoints, request/response shape changes) is introduced; this is purely an internal authorization-mechanism consolidation with a behavioral bug fix for authenticated owners.
