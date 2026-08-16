package de.paladinsinn.torg.codex.api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * The single production component that derives which product/codex ids the current request's
 * caller owns, for content-censoring purposes (constitution Principle V,
 * feature {@code 001-unify-censoring-authorization}).
 *
 * <p>Resolves ownership exclusively from {@code ROLE_<codex-id>} {@link GrantedAuthority} entries
 * on the current {@link SecurityContextHolder} {@link Authentication}, independent of the concrete
 * authentication method or principal type that populated it (DriveThruRPG API key today; any
 * future method, e.g. OIDC/Keycloak, transparently supported without further change here, because
 * this reads only the {@link GrantedAuthority} abstraction, not a principal-specific type).
 *
 * <p>This is the correct, principal-type-agnostic logic previously implemented only in the
 * never-wired {@code SecuredMarkupService}; it is the sole collaborator through which
 * {@link CurrentUserCensorFactory} determines product ownership.
 */
@Component
public class ProductOwnershipResolver {

    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * @return the set of codex ids ({@code ROLE_} prefix stripped) the current request's caller
     *         owns; never {@code null}; empty if there is no authenticated caller or no matching
     *         authorities. The returned set is unmodifiable.
     */
    public Set<String> resolve() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Set.of();
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .collect(Collectors.toUnmodifiableSet());
    }
}
