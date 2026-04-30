package de.paladinsinn.torg.codex.markup;

import de.paladinsinn.torg.codex.data.markup.TorgMarkupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecuredMarkupServiceTest {

    @Mock
    TorgMarkupService markupService;

    @InjectMocks
    SecuredMarkupService service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // unauthenticated / anonymous
    // ------------------------------------------------------------------

    @Test
    void noAuthentication_passesEmptyProductSet() {
        SecurityContextHolder.clearContext();
        when(markupService.render(any(), any())).thenReturn("");

        service.render("text");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(markupService).render(eq("text"), captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void anonymousAuthentication_passesEmptyProductSet() {
        var anon = new AnonymousAuthenticationToken(
                "key", "anonymous",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(anon);
        when(markupService.render(any(), any())).thenReturn("");

        service.render("text");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(markupService).render(eq("text"), captor.capture());
        // ROLE_ANONYMOUS is stripped to "ANONYMOUS" — anonymous users DO appear to own nothing
        // of the actual products, but let us assert the stripped value is present so the
        // processor can decide (an <IF:ANONYMOUS> block would be visible to them).
        assertThat(captor.getValue()).containsExactly("ANONYMOUS");
    }

    // ------------------------------------------------------------------
    // authenticated user with product roles
    // ------------------------------------------------------------------

    @Test
    void authenticatedUser_productRolesStrippedOfPrefix() {
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                "user",
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_core-rulebook"),
                        new SimpleGrantedAuthority("ROLE_delphi-missions-aysle")
                )
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(markupService.render(any(), any())).thenReturn("rendered");

        service.render("raw");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(markupService).render(eq("raw"), captor.capture());
        assertThat(captor.getValue())
                .containsExactlyInAnyOrder("core-rulebook", "delphi-missions-aysle");
    }

    @Test
    void explicitProductIds_bypassSecurityContext() {
        // No authentication set — but explicit product-ids are provided directly.
        SecurityContextHolder.clearContext();
        when(markupService.render(any(), any())).thenReturn("rendered");

        service.render("raw", Set.of("core-rulebook", "delphi-missions-aysle"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(markupService).render(eq("raw"), captor.capture());
        assertThat(captor.getValue())
                .containsExactlyInAnyOrder("core-rulebook", "delphi-missions-aysle");
    }

    @Test
    void nonRoleAuthorities_notIncludedInProductSet() {
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                "user",
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_core-rulebook"),
                        new SimpleGrantedAuthority("SCOPE_read")   // OAuth2 scope, no ROLE_ prefix
                )
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(markupService.render(any(), any())).thenReturn("");

        service.render("raw");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(markupService).render(eq("raw"), captor.capture());
        assertThat(captor.getValue()).containsExactly("core-rulebook");
    }
}
