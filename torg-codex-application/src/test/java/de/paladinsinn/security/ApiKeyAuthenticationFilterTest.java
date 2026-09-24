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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
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

package de.paladinsinn.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ApiKeyAuthenticationFilterTest {

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final ApiKeyAuthenticationFilter subject = new ApiKeyAuthenticationFilter(authenticationManager);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void setsNotLoggedInUserDetailsWhenApiKeyIsMissing() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = mock(FilterChain.class);

        subject.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isInstanceOfSatisfying(AnonymousAuthenticationToken.class, authentication -> {
                    assertThat(authentication.getPrincipal()).isSameAs(NotLoggedInUserDetails.INSTANCE);
                    assertThat(authentication.getAuthorities())
                            .containsExactlyElementsOf(NotLoggedInUserDetails.INSTANCE.getAuthorities());
                });
        verifyNoInteractions(authenticationManager);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void retainsExistingAuthenticationWhenApiKeyIsMissing() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = mock(FilterChain.class);
        final TestingAuthenticationToken existingAuthentication =
                new TestingAuthenticationToken("authenticated user", null);
        SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

        subject.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isSameAs(existingAuthentication);
        verifyNoInteractions(authenticationManager);
        verify(filterChain).doFilter(request, response);
    }
}
