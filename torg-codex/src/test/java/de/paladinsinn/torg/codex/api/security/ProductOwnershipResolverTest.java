package de.paladinsinn.torg.codex.api.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link ProductOwnershipResolver} (feature
 * {@code 001-unify-censoring-authorization}, User Story 1, FR-001).
 *
 * <p>Ports the four ownership scenarios previously proven by the now-removed
 * {@code SecuredMarkupServiceTest}, deliberately using <em>generic</em> Spring Security
 * principals ({@code UsernamePasswordAuthenticationToken}, {@code AnonymousAuthenticationToken})
 * rather than a {@code DriveThruUserDetails}, to prove the resolution is principal-type
 * independent — the specific case the pre-fix production code got wrong.
 */
class ProductOwnershipResolverTest {

    private final ProductOwnershipResolver resolver = new ProductOwnershipResolver();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthentication_resolvesToEmptySet() {
        SecurityContextHolder.clearContext();

        assertThat(resolver.resolve()).isEmpty();
    }

    @Test
    void anonymousAuthentication_stripsRolePrefix() {
        var anon = new AnonymousAuthenticationToken(
                "key", "anonymous",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anon);

        assertThat(resolver.resolve()).containsExactly("ANONYMOUS");
    }

    @Test
    void authenticatedGenericPrincipal_productRolesStrippedOfPrefix() {
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                "user",
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_core-rulebook"),
                        new SimpleGrantedAuthority("ROLE_delphi-missions-aysle")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(resolver.resolve())
                .containsExactlyInAnyOrder("core-rulebook", "delphi-missions-aysle");
    }

    @Test
    void nonRoleAuthorities_areExcluded() {
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                "user",
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_core-rulebook"),
                        new SimpleGrantedAuthority("SCOPE_read"))); // OAuth2 scope, no ROLE_ prefix
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(resolver.resolve()).containsExactly("core-rulebook");
    }
}
