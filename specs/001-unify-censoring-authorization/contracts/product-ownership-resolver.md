# Contract: `ProductOwnershipResolver`

**Type**: Internal Java component contract (not a REST/HTTP contract — this feature makes no
REST API changes; see `censoring-single-mechanism-invariant.md` for the architectural contract
and `../plan.md` Constitution Check / Principle III for why no OpenAPI artifact is affected).

**Module / package**: `torg-codex`, `de.paladinsinn.torg.codex.api.security` (new file,
alongside the existing `CurrentUserCensorFactory` it exclusively serves).

## Interface

```java
package de.paladinsinn.torg.codex.api.security;

import java.util.Set;

/**
 * The single production component that derives which product/codex ids the current
 * request's caller owns, for content-censoring purposes (constitution Principle V).
 *
 * <p>Resolves ownership exclusively from {@code ROLE_<codex-id>} {@link
 * org.springframework.security.core.GrantedAuthority} entries on the current {@link
 * org.springframework.security.core.context.SecurityContextHolder} {@code Authentication},
 * independent of the concrete authentication method or principal type that populated it
 * (DriveThruRPG API key today; any future method, e.g. OIDC/Keycloak, transparently supported
 * without further change here).</p>
 */
public interface ProductOwnershipResolver {

    /**
     * @return the set of codex ids (ROLE_ prefix stripped) the current request's caller owns;
     *         never {@code null}; empty if there is no authenticated caller or no matching
     *         authorities.
     */
    Set<String> resolve();
}
```

(A concrete `@Component` implementation is an implementation-phase task; this contract only
fixes the public shape/behavior a future implementation and its unit tests must satisfy.)

## Preconditions

- None. Callable at any point during request processing where a `SecurityContext` may or may
  not be populated (mirrors `SecuredMarkupService.ownedProducts()`'s existing null-safety).

## Postconditions / Behavioral Contract

| Input state of `SecurityContextHolder.getContext().getAuthentication()` | `resolve()` output |
|---|---|
| `null` | `Set.of()` (empty) |
| non-null but `isAuthenticated() == false` | `Set.of()` (empty) |
| authenticated, authorities = `[ROLE_core-rulebook]` (anonymous production principal, `NotLoggedInUserDetails`) | `{"core-rulebook"}` |
| authenticated, authorities = `[ROLE_DRIVETHRU_USER, ROLE_core-rulebook, ROLE_sourcebook-aysle]` (`DriveThruUserDetails`) | `{"core-rulebook", "sourcebook-aysle"}` — `DRIVETHRU_USER` is included too (harmless; no `<IF:DRIVETHRU_USER>` block exists in any content) |
| authenticated, authorities = `[ROLE_sourcebook-aysle]` (any other principal type Spring Security accepts, e.g. a plain test `User`, or a future `JwtAuthenticationToken`) | `{"sourcebook-aysle"}` — **this is the case the current production code gets wrong today** |
| authenticated, authorities include a non-`ROLE_`-prefixed entry, e.g. `SCOPE_read` | that entry is excluded from the result |
| authenticated, authorities include `ROLE_<unknown-or-stale-id>` not present in the catalog | included in the result set as-is (harmless — `ConditionalBlockProcessor` simply never finds a matching `<IF:unknown-or-stale-id>` block, per spec edge case) |

## Invariants

1. **Single caller of `SecurityContextHolder` for this purpose**: In production code, only
   `ProductOwnershipResolver` (and, transitively, its sole caller `CurrentUserCensorFactory`) may
   read `SecurityContextHolder.getContext().getAuthentication()` in order to decide product
   ownership for censoring. No controller, repository, JPA entity, or mapper may do so directly
   (constitution Principle V: "Repository queries, entities, controllers, and mappers MUST NOT
   bypass censoring").
2. **No I/O**: `resolve()` must not perform any database query, HTTP call, or other blocking I/O
   — it is a pure, in-memory read of already-populated `Authentication` state (SC-005: no added
   latency/round-trips).
3. **Idempotent within a request**: Calling `resolve()` multiple times within the same request
   thread returns an equal set each time (no mutation of `SecurityContext` as a side effect).

## Consumers

- `CurrentUserCensorFactory.create()` — the only production caller; passes the result to
  `Censor.of(markupService, ownedProducts)` unchanged from today's contract.

## Test Contract

A `ProductOwnershipResolverTest` (or equivalently named) unit test MUST cover, at minimum, the
same scenarios `SecuredMarkupServiceTest` already proves today for its (now-deleted) internal
`ownedProducts()` method:

1. No `Authentication` in context → empty set.
2. Anonymous authentication (`AnonymousAuthenticationToken`, `ROLE_ANONYMOUS`) → `{"ANONYMOUS"}`
   (prefix stripped; the processor decides whether any `<IF:ANONYMOUS>` block exists — none does
   in current content, so this is inert but must not throw).
3. Authenticated user with product-role authorities → exact stripped set, regardless of
   concrete principal type (this is the regression-proof case: the test MUST use a generic
   `UsernamePasswordAuthenticationToken`/`user(...)`-style principal, **not** a
   `DriveThruUserDetails`, to prove principal-type independence).
4. Mixed `ROLE_` and non-`ROLE_` (e.g. `SCOPE_read`) authorities → only `ROLE_`-prefixed entries
   included.
