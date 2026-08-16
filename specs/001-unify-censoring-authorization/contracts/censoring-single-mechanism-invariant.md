# Contract: Single Censoring-Authorization-Mechanism Invariant

**Type**: Architectural/testable invariant contract (constitution Principle V, spec FR-002 /
User Story 2). This is not a REST contract; it formalizes an ArchUnit-style rule so the
"exactly one mechanism" requirement is independently verifiable rather than only asserted in
prose.

## Rule

> Exactly one production class may derive product ownership for content-censoring purposes from
> the Spring Security `SecurityContext`/`Authentication` (directly or by delegating to a single,
> narrowly-scoped internal collaborator dedicated solely to that purpose). No second,
> independently-implemented component may perform the same derivation in parallel.

## Formal statement (for an ArchUnit test, implementation-phase task)

```java
// Illustrative — not implementation code for this plan; a concrete ArchUnit rule is an
// implementation-phase task (tracked via /speckit.tasks).
//
// classes that call Authentication.getAuthorities() (or SecurityContextHolder.getContext()
// .getAuthentication()) for the purpose of deriving a Set<String> of owned product ids
// SHOULD be limited to:
//   - de.paladinsinn.torg.codex.api.security.ProductOwnershipResolver
//   - de.paladinsinn.torg.codex.api.security.CurrentUserCensorFactory (its sole caller)
//
// classes().that().callMethod(GrantedAuthority.class, "getAuthority")
//     .should().resideInAPackage("de.paladinsinn.torg.codex.api.security..")
```

## Verification approach (matches spec's own Independent Test for User Story 2)

Per spec User Story 2's Independent Test: "inspecting the dependency graph of every REST
controller and confirming exactly one component type is responsible for resolving product
ownership for censoring, and that no other component in the codebase independently re-implements
that decision." Concretely, after implementation:

1. `grep -rn "SecurityContextHolder\|Authentication" --include=*.java torg-codex/src/main/java torg-codex-data/src/main/java | grep -i "getAuthorities\|getAuthentication"` — every production hit resolving *product ownership* (as opposed to unrelated authentication/authorization checks, e.g. `LoginController`'s login flow) MUST resolve to `ProductOwnershipResolver` and its sole caller `CurrentUserCensorFactory`.
2. `grep -rl "SecuredMarkupService" --include=*.java .` MUST return zero results (class removed entirely, per Decision 2 in `../research.md`).
3. All 15 gated REST controllers (Article, Cosm, Item, Miracle, MiracleList, Perk, PerkGroup,
   Power, PowerList, Race, Shard, Spell, SpellList, Threat, Vehicle) MUST inject
   `CurrentUserCensorFactory` and no other ownership-resolution type.

## Baseline (pre-fix) violation this contract documents

Before this feature, **two** independent, divergent implementations existed:

| Component | Correctness | Wired into controllers? |
|---|---|---|
| `CurrentUserCensorFactory` → `DriveThruUserService.getCurrentUser()` | **Incorrect** — only recognizes `ApiKeyAuthenticationToken`/`DriveThruUserDetails`; ignores generic `ROLE_<codex-id>` authorities from any other principal type | **Yes** (all 15 gated controllers) |
| `SecuredMarkupService.ownedProducts()` | **Correct** — reads `Authentication.getAuthorities()` generically | **No** (zero production callers) |

This dual-implementation state is exactly what constitution Principle V's added rule prohibits
("Parallel or duplicate censoring/ownership-resolution implementations... MUST NOT coexist").

## Post-fix target state

| Component | Correctness | Wired into controllers? |
|---|---|---|
| `CurrentUserCensorFactory` → `ProductOwnershipResolver` | Correct — reads `Authentication.getAuthorities()` generically (ported from `SecuredMarkupService`) | Yes (all 15 gated controllers, unchanged call sites) |
| *(no second component exists)* | n/a | n/a |

## No REST/OpenAPI contract changes

This feature introduces, removes, or modifies **no** REST endpoint, request/response DTO, or
media type (FR-009). No OpenAPI specification file exists in this repository today (verified: no
`springdoc`/`swagger` Maven dependency in any module), so there is no OpenAPI contract artifact to
update for this feature. REST-response stability for non-gated content is instead verified via
the existing characterization fixture suite's byte-for-byte comparisons (see `../quickstart.md`).
