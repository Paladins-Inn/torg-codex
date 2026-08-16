# Phase 0 Research: Unify Product-Ownership Censoring Authorization

All unknowns raised by the feature spec were resolved by direct inspection of the current
codebase (no external libraries or new technology choices are needed — this is a bug-fix
consolidation within the existing stack). Each decision below cites the exact files inspected.

---

## Decision 1 — Which class becomes "the" single production mechanism?

**Decision**: Keep `CurrentUserCensorFactory` (`torg-codex/src/main/java/de/paladinsinn/torg/codex/api/security/CurrentUserCensorFactory.java`) as the one production entry point, and fix its ownership derivation. Do **not** rewire the 15 gated controllers onto `SecuredMarkupService`.

**Rationale**:
- `CurrentUserCensorFactory` is already a `@Component` injected into and called (`censorFactory.create()`) by all 15 controllers that have gated fields (confirmed by `grep -rl CurrentUserCensorFactory torg-codex/src/main/java/.../api/controller/` → 15 hits: Article, Cosm, Item, Miracle, MiracleList, Perk, PerkGroup, Power, PowerList, Race, Shard, Spell, SpellList, Threat, Vehicle). Its return type `Censor` is consumed via MapStruct's `@Context` parameter (see `CosmMapper.toDetail(Cosm cosm, @Context Censor censor)` and `TorgMappingSupport.censorText(String, @Context Censor)`), which is a mapping-time integration point `SecuredMarkupService.render(String)` does not participate in at all.
- Rewiring all 15 controllers + their mappers to call `SecuredMarkupService` instead would touch ~30 files for no behavioral benefit and would risk violating FR-009 (zero regression for non-gated content) by touching working code paths unnecessarily.
- `SecuredMarkupService` (`torg-codex/src/main/java/de/paladinsinn/torg/codex/markup/SecuredMarkupService.java`) is confirmed dead in production: `grep -rln SecuredMarkupService --include=*.java . | grep -v /test/` returns only its own file plus three files that merely *mention* it in Javadoc (`TorgCodexSecurityConfig`, `NotLoggedInUserDetails`, `DriveThruUserDetails`) — no autowiring, no controller/service actually calls it.

**Alternatives considered**:
- *Rewire controllers onto `SecuredMarkupService`*: rejected — larger diff, breaks the proven MapStruct `@Context Censor` integration pattern, higher regression risk.
- *Keep both classes, have `SecuredMarkupService` delegate to `CurrentUserCensorFactory`*: rejected — `SecuredMarkupService.render(String)` has zero callers; keeping a live-but-uncalled pass-through is dead weight that still needs its own tests/maintenance for no reader benefit, and does not by itself satisfy "no independent ownership-resolution logic remains" unless its internals are gutted anyway (see Decision 2).

---

## Decision 2 — How to consolidate the (correct) authority-stripping logic without losing test coverage

**Decision**: Extract the authority→product-id resolution logic — currently only implemented correctly inside `SecuredMarkupService.ownedProducts()` (private method, `torg-codex/src/main/java/de/paladinsinn/torg/codex/markup/SecuredMarkupService.java:69-79`) — into a new, narrowly-scoped, unit-testable collaborator `ProductOwnershipResolver` in the same package as `CurrentUserCensorFactory` (`de.paladinsinn.torg.codex.api.security`). `CurrentUserCensorFactory.create()` delegates to it instead of to `DriveThruUserService.getCurrentUser()`. Port the 4 existing scenarios from `SecuredMarkupServiceTest` (`torg-codex/src/test/java/de/paladinsinn/torg/codex/markup/SecuredMarkupServiceTest.java`) onto a new `ProductOwnershipResolverTest`, then delete `SecuredMarkupService.java` and `SecuredMarkupServiceTest.java` entirely.

**Rationale**:
- The existing logic (verified in `SecuredMarkupService.ownedProducts()`):
  ```java
  Authentication auth = SecurityContextHolder.getContext().getAuthentication();
  if (auth == null || !auth.isAuthenticated()) return Collections.emptySet();
  return auth.getAuthorities().stream()
          .map(GrantedAuthority::getAuthority)
          .filter(a -> a.startsWith("ROLE_"))
          .map(a -> a.substring("ROLE_".length()))
          .collect(Collectors.toUnmodifiableSet());
  ```
  is already correct, principal-type-agnostic, and covered by 4 passing Mockito unit tests (no-auth → empty set; anonymous-with-`ROLE_ANONYMOUS` → stripped; authenticated-with-product-roles → correct set; non-`ROLE_` authorities like `SCOPE_read` filtered out). This is exactly the logic `CurrentUserCensorFactory` needs; re-deriving it from scratch would risk reintroducing subtle bugs (e.g. forgetting the `isAuthenticated()` guard) that this test suite already guards against.
- A separate resolver class keeps `CurrentUserCensorFactory` focused on one responsibility (build a `Censor`) and keeps the authority-scanning logic trivially unit-testable in isolation (as `SecuredMarkupServiceTest` already demonstrates) without needing a `TorgMarkupService`/`DriveThruUserService` mock in scope.
- This satisfies spec User Story 2, Acceptance Scenario 2's explicit allowance: "exactly one such component is found (plus its narrowly-scoped internal collaborators, e.g. the markup rendering pipeline it delegates to)" — `ProductOwnershipResolver` is such a narrowly-scoped internal collaborator of `CurrentUserCensorFactory`, not a second independent mechanism.
- Deleting `SecuredMarkupService` (rather than reducing it to a delegating stub) is preferred over FR-007's "thin delegation" alternative because it has **zero production callers** today — keeping an unused public `@Service` around after its logic has moved elsewhere would itself become a new piece of dead/misleading code, the exact problem this feature is fixing.

**Alternatives considered**:
- *Inline the stream logic directly into `CurrentUserCensorFactory.create()`*: works, but loses the isolated unit-testability the current `SecuredMarkupServiceTest` scenarios rely on (would require mocking `DriveThruUserService`/`TorgMarkupService` just to test authority-stripping behavior). Rejected in favor of the small dedicated resolver.
- *Reduce `SecuredMarkupService` to `return productOwnershipResolver.resolve();` and keep it as a public thin-delegate per FR-007's alternative wording*: rejected — since it has no callers, this only adds surface area (still a `@Service` in the container, still needs a Javadoc/test) with no reader ever exercising it; deletion is the more honest "single mechanism" outcome and directly satisfies User Story 2 Acceptance Scenario 3 ("the duplicate is either removed or reduced to a thin delegation... no independent ownership-resolution logic remains") via the "removed" branch.

---

## Decision 3 — `DriveThruUserService` dependency removal from `CurrentUserCensorFactory`

**Decision**: Remove the `DriveThruUserService userService` field/import from `CurrentUserCensorFactory`; `ProductOwnershipResolver` depends only on `SecurityContextHolder` (static Spring Security API), not on `DriveThruUserService`.

**Rationale**: `grep -rn DriveThruUserService --include=*.java . | grep -v /test/` shows its only production caller today is `CurrentUserCensorFactory`. `DriveThruUserService.getOwnedProductIds()`/`getOwnedProducts()` (direct DriveThruRPG API passthroughs, `torg-codex-data/src/main/java/de/paladinsinn/security/DriveThruUserService.java:88-105`) are unrelated to censoring and are not currently called by anything in production (no `LoginController` or other controller invokes them) — they remain untouched, available for future direct-API-call features, but the censoring path no longer needs to go through this class at all.

**Alternatives considered**: Leaving the field but unused — rejected as needless residual coupling that would fail a future "why does the censor factory depend on the DriveThru API client" code-review question.

---

## Decision 4 — Authentication-method independence (FR-001) and OIDC/Keycloak

**Decision**: Satisfy FR-001's "independent of the authentication method... (DriveThruRPG API key, OIDC/Keycloak token, or any other...)" requirement by having `ProductOwnershipResolver` operate purely against the `Authentication.getAuthorities()` interface-level abstraction (any `Authentication` implementation Spring Security ever produces already exposes this), rather than building a new OIDC/Keycloak authentication flow as part of this feature.

**Rationale**: Repository-wide search confirms **no OIDC/Keycloak wiring exists in production code today**: `grep -rln "oauth2\|Oidc\|OAuth2\|resourceserver\|resource-server" --include=*.java --include=*.yml -i .` returns only a single unrelated code comment in `SecuredMarkupServiceTest` (`// OAuth2 scope, no ROLE_ prefix`); there is no `SecurityFilterChain` segment, no `JwtAuthenticationConverter`, and no `spring-security-oauth2-*` Maven dependency anywhere in the reactor (`torg-codex/pom.xml`, `torg-codex-data/pom.xml`, root `pom.xml`). Constitution Principle V's "Identity management uses Keycloak OIDC" is an architectural target, not yet an implemented mechanism in this application. The spec's own Assumptions section frames "populating [ROLE_<codex-id> authorities]" as in-scope *if* an auth method exists but doesn't populate them (true today for DriveThruRPG, since `DriveThruUserDetails.getAuthorities()` already does this correctly — see below); it does **not** ask this feature to stand up a brand-new authentication method, which spec Assumptions explicitly rules out ("No new user-facing API surface... this is purely an internal authorization-mechanism consolidation").
Because `DriveThruUserDetails.getAuthorities()` (`torg-codex-data/src/main/java/de/paladinsinn/security/DriveThruUserDetails.java:99-107`) already emits one `ROLE_<codexId>` authority per owned publication (plus `ROLE_DRIVETHRU_USER`), and `NotLoggedInUserDetails` (the configured anonymous principal, `TorgCodexSecurityConfig.java:104-106`) already emits `ROLE_core-rulebook`, a generic `Authentication.getAuthorities()`-based resolver transparently fixes DriveThruRPG-authenticated *and* anonymous requests today, and will transparently support any future OIDC/Keycloak `Authentication` implementation the moment one is added — with zero further change to the censoring layer — because it depends only on the `GrantedAuthority` contract, not on any principal's concrete type.

**Alternatives considered**: Implementing a minimal OIDC/Keycloak login path as part of this feature so FR-001's "OIDC" clause has something concrete to test against — rejected: out of scope per spec Assumptions ("no new user-facing API surface"), would require new dependencies/config/ADR (constitution Additional Constraints: architectural changes require an ADR), and is unnecessary to prove FR-001/FR-004 because the spec's own Independent Test for User Story 1 explicitly allows exercising `ROLE_sourcebook-aysle` "via any principal type the security configuration accepts" — which the existing Spring Security Test `user(...).authorities(...)` principal (already used by `CharacterizationAuthVariant.SOURCEBOOK_AYSLE_OWNER`) satisfies without needing real OIDC infrastructure.

---

## Decision 5 — Regression-proof test coverage (FR-008, User Story 3)

**Decision**: Add a new differential test (`CensoringDifferentialTest` or equivalent, in `torg-codex/src/test/java/de/paladinsinn/torg/codex/characterization/`) that performs two **live** `MockMvc` requests against the Aysle cosm detail endpoint — one anonymous, one with `SecurityMockMvcRequestPostProcessors.user(...).authorities(new SimpleGrantedAuthority("ROLE_sourcebook-aysle"), ...)` (the same pattern `CharacterizationAuthVariant.SOURCEBOOK_AYSLE_OWNER` already uses) — and asserts within the test body that the two `worldLaws` values differ, that the owner response contains the known owner-only substring, and that the anonymous response contains the known upsell substring. Additionally, regenerate the `owner-detail.json`/`anonymous-detail.json` characterization fixtures for `cosms` (and any other area whose captured "detail" sample happens to change) by re-running `CharacterizationFixtureCaptureTest` with `-Dcharacterization.capture=true` after the fix, and commit the resulting fixtures.

**Rationale**:
- `CharacterizationReplayTest` (`torg-codex/src/test/java/de/paladinsinn/torg/codex/characterization/CharacterizationReplayTest.java`) only asserts that a live response's status/headers/body equal whatever was **previously captured** into the fixture JSON files. If the censoring regression were reintroduced, the capture step would simply record two identical fixtures again and the replay test would keep passing — replay alone provably cannot catch this class of regression (this is exactly what happened during the Hexagonal Architecture Migration per the constitution's Sync Impact Report). A new test must assert differential behavior *directly in test code*, not merely against a stored snapshot, per constitution Principle V's test-adequacy rule and FR-008.
- `CharacterizationFixtureCaptureTest.findDrmSensitiveId()` (`torg-codex/src/test/java/de/paladinsinn/torg/codex/characterization/CharacterizationFixtureCaptureTest.java:79-89`) already contains exactly this "do anonymous and owner differ?" probe, but only to *pick which sample id to capture* — today it always falls through to the arbitrary first entity because no id currently differs. Once `ProductOwnershipResolver` is wired in, this probe will start finding true DRM-sensitive ids (e.g. Aysle, whose seed CSV `torg-codex-data/src/main/resources/db/load/torg_cosm.csv` contains two `<IF:sourcebook-aysle>`/`</IF:sourcebook-aysle>`-delimited regions at lines 20 and 493), so re-running the capture step is expected to produce genuinely different `owner-detail.json`/`anonymous-detail.json` bodies for `cosms` for the first time.
- The Aysle cosm id used by fixtures today, `6cf031c3-ab0a-4d12-9173-91d74f7c809f` (from `torg-codex/src/test/resources/characterization/cosms/owner-detail.json`), is confirmed present in seed data with real gated markup, satisfying spec Assumption 4.

**Alternatives considered**:
- *Rely solely on regenerated characterization fixtures, no new dedicated test*: rejected — a byte-diff between two JSON files committed to the repo is easy to miss/revert accidentally in a future PR review, and doesn't self-document *why* the two fixtures must differ; an explicit assertion in test code (`assertThat(ownerBody).isNotEqualTo(anonymousBody)` plus substring assertions) is more resilient and directly matches FR-008/SC-001's wording.
- *Write a unit test on `Censor`/`ConditionalBlockProcessor` only*: rejected as insufficient alone — those already have adequate coverage of the markup mechanics in isolation; the missing coverage is specifically at the **security-context-to-controller-response** integration level, which only a `MockMvc` + Spring Security Test-based test can exercise end-to-end.

---

## Decision 6 — Documentation alignment (deferred to implementation/tasks phase)

**Decision**: `docs/modules/arc42/pages/08_concepts/torg_data/role-mapping.adoc` currently documents `SecuredMarkupService` as *the* production mechanism, including a fabricated example `CosmController` that injects `SecuredMarkupService` directly — this does not match the real `CosmController` (which injects `CurrentUserCensorFactory` and passes a `Censor` through MapStruct). This doc must be rewritten to describe the consolidated `CurrentUserCensorFactory` → `ProductOwnershipResolver` → `Censor` → `TorgMarkupService` pipeline once implemented. This is tracked as follow-up work for `/speckit.tasks`/`/speckit.implement` (constitution Development Workflow item 6, "Documentation Alignment"), not produced by this planning phase, and is noted here so it is not lost.

**Rationale**: Out of scope for `/speckit.plan` (which produces design artifacts, not documentation edits), but constitution governance requires `docs/modules/arc42` to stay synchronized with implementation, so it must appear as a task.

---

## Summary of resolved unknowns

| Unknown / NEEDS CLARIFICATION | Resolution |
|---|---|
| Which class is "the" single mechanism going forward? | `CurrentUserCensorFactory` (already wired), delegating to new `ProductOwnershipResolver`. |
| What happens to `SecuredMarkupService`? | Deleted (dead code, zero production callers); its correct/tested authority-stripping logic is ported into `ProductOwnershipResolver`. |
| Does `DriveThruUserService` need changes? | No — only its (sole) caller for censoring purposes, `CurrentUserCensorFactory`, stops calling it; `DriveThruUserService`'s other DriveThruRPG API methods are untouched and unaffected. |
| Is OIDC/Keycloak authentication in scope to implement? | No — not currently wired in the codebase at all; out of scope per spec Assumptions ("no new user-facing API surface"). The fix is authentication-method-agnostic by construction (reads the `GrantedAuthority` abstraction), so it will work automatically once/if OIDC is added later. |
| How is FR-008's "genuine regression-detecting test" satisfied? | New differential MockMvc test asserting live anonymous ≠ owner response for the Aysle cosm, plus regenerated (now genuinely-differing) characterization fixtures. |
| Does this feature change persisted data, schema, REST contracts, or markup rendering order? | No (FR-006, FR-009, FR-010) — confirmed no Liquibase changes needed, no DTO/controller signature changes needed, and `TorgMarkupService`/`ConditionalBlockProcessor` are not modified. |

**Output**: All NEEDS CLARIFICATION markers from the spec are resolved (the spec itself already had none — see `checklists/requirements.md`); the above are the implementation-design decisions this plan's Phase 1 artifacts are built on.
