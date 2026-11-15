/*
 * Copyright (c) 2026.  Roland T. Lichti <rlichti@kaiserpfalz-edv.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * ERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * You may contact me via email rlichti@kaiserpfalz-edv.de or via mail
 *
 * Kaiserpfalz EDV-Service
 * Roland T. Lichti
 * Darmstädter Str. 12
 * 64625 Bensheim
 * GERMANY
 */

package de.paladinsinn.torg.codex.api.security;

import de.paladinsinn.security.NotLoggedInUserDetails;
import de.paladinsinn.torg.codex.application.port.out.CallerProductsPort;
import de.paladinsinn.torg.codex.domain.access.OwnedProducts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.TestingAuthenticationToken;

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

    @Test
    void unauthenticatedAuthentication_resolvesToEmptySet() {
        // The unauthenticated constructor yields isAuthenticated() == false.
        var unauthenticated = new UsernamePasswordAuthenticationToken("user", "password");
        assertThat(unauthenticated.isAuthenticated()).isFalse();
        SecurityContextHolder.getContext().setAuthentication(unauthenticated);

        assertThat(resolver.resolve()).isEmpty();
    }

    @Test
    void unknownOrStaleProductRole_isIncludedAsIs() {
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                "user",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_this-product-does-not-exist")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Unknown/stale ids are inert (no matching <IF:...> block) but must not be filtered.
        assertThat(resolver.resolve()).containsExactly("this-product-does-not-exist");
    }

    @Test
    void resolve_isIdempotentWithinSameContext() {
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                "user",
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_core-rulebook"),
                        new SimpleGrantedAuthority("ROLE_sourcebook-aysle")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(resolver.resolve())
                .isEqualTo(resolver.resolve())
                .containsExactlyInAnyOrder("core-rulebook", "sourcebook-aysle");
    }

    @Test
    void resolvedSet_isUnmodifiable() {
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                "user",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_core-rulebook")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(resolver.resolve())
                .as("resolve() returns an unmodifiable set (no side effects, no leaking mutation)")
                .isUnmodifiable();
    }

    @Test
    void resolverIsTheCallerProductsPort() {
        assertThat(resolver).isInstanceOf(CallerProductsPort.class);
    }

    @Test
    void current_withoutAuthentication_isTheFreeTier() {
        SecurityContextHolder.clearContext();

        assertThat(resolver.current()).isEqualTo(OwnedProducts.anonymous());
    }

    @Test
    void current_forTheNotLoggedInPrincipal_isTheFreeTier() {
        var anonymous = new AnonymousAuthenticationToken(
                "key", NotLoggedInUserDetails.INSTANCE, NotLoggedInUserDetails.INSTANCE.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        assertThat(resolver.current().codexIds()).containsExactly(OwnedProducts.FREE_TIER);
    }

    @Test
    void current_withOnlyTheDriveThruUserRole_ownsNoProductBeyondTheFreeTier() {
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                "user", null, List.of(new SimpleGrantedAuthority("ROLE_DRIVETHRU_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        OwnedProducts owned = resolver.current();

        assertThat(owned.codexIds()).contains(OwnedProducts.FREE_TIER);
        assertThat(owned.ownsAnyOf(java.util.Set.of("sourcebook-aysle"))).isFalse();
    }

    @Test
    void current_forAnAysleOwnerWithoutCoreAuthority_includesTheFreeTier() {
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                "user", null, List.of(new SimpleGrantedAuthority("ROLE_sourcebook-aysle")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(resolver.current().codexIds())
                .containsExactlyInAnyOrder("sourcebook-aysle", OwnedProducts.FREE_TIER);
    }

    @Test
    void current_isPrincipalTypeIndependent() {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_sourcebook-aysle"));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("user", null, authorities));
        OwnedProducts fromUsernamePassword = resolver.current();

        var testing = new TestingAuthenticationToken(new Object(), null, authorities);
        testing.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(testing);
        OwnedProducts fromTestingToken = resolver.current();

        assertThat(fromTestingToken).isEqualTo(fromUsernamePassword);
    }

    @Test
    void freeProductIdOfTheAnonymousPrincipalIsTheDomainFreeTier() {
        assertThat(NotLoggedInUserDetails.FREE_PRODUCT_ID).isSameAs(OwnedProducts.FREE_TIER);
    }
}
