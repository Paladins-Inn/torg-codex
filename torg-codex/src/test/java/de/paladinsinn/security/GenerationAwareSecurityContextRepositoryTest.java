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

import de.paladinsinn.drivethru.ApiKeyFingerprint;
import de.paladinsinn.drivethru.token.DrivethruToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying {@link GenerationAwareSecurityContextRepository} protection against stale
 * request saves overwriting newer background batches, newer logins, or logged-out sessions.
 */
class GenerationAwareSecurityContextRepositoryTest {

    private static final ApiKeyFingerprint FIRST_KEY = ApiKeyFingerprint.of("first-reader-api-key");
    private static final ApiKeyFingerprint SECOND_KEY = ApiKeyFingerprint.of("second-reader-api-key");
    private static final Instant STARTED_AT = Instant.parse("2026-11-16T09:00:00Z");

    private final GenerationAwareSecurityContextRepository repository =
            new GenerationAwareSecurityContextRepository();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void staleRequestSaveNeverDowngradesNewerWorkerBatchesOrSupersedesNewerGeneration() {
        MockHttpSession session = new MockHttpSession();
        UUID gen1 = UUID.randomUUID();
        SecurityContext initialContext = contextWithBooks("424242", List.of());

        MockHttpServletRequest loginReq = new MockHttpServletRequest();
        loginReq.setSession(session);
        repository.saveExplicitAuthentication(
                initialContext.getAuthentication(),
                DriveThruSessionSyncState.running(gen1, STARTED_AT),
                FIRST_KEY,
                loginReq,
                new MockHttpServletResponse());

        MockHttpServletRequest slowGetReq = new MockHttpServletRequest();
        slowGetReq.setSession(session);
        MockHttpServletResponse slowGetRes = new MockHttpServletResponse();
        SecurityContext loadedAtStart = repository.loadDeferredContext(slowGetReq).get();
        assertThat(principal(loadedAtStart).ownedCodexIds()).isEmpty();

        // Worker applies Batch 1 while slowGetReq is still rendering
        SecurityContext batchOneContext = contextWithBooks("424242", List.of("sourcebook-aysle"));
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, batchOneContext);
        session.setAttribute(
                DriveThruSessionSyncState.SYNC_STATE_KEY,
                DriveThruSessionSyncState.running(gen1, STARTED_AT).withBatchApplied());

        // Slow request finishes and tries to save its stale free-tier context
        repository.saveContext(loadedAtStart, slowGetReq, slowGetRes);

        SecurityContext afterStaleSave = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(principal(afterStaleSave).ownedCodexIds()).containsExactly("sourcebook-aysle");

        // Newer login with a different key supersedes gen1
        UUID gen2 = UUID.randomUUID();
        MockHttpServletRequest secondLoginReq = new MockHttpServletRequest();
        secondLoginReq.setSession(session);
        repository.saveExplicitAuthentication(
                contextWithBooks("999999", List.of("sourcebook-living-land")).getAuthentication(),
                DriveThruSessionSyncState.running(gen2, STARTED_AT),
                SECOND_KEY,
                secondLoginReq,
                new MockHttpServletResponse());

        repository.saveContext(loadedAtStart, slowGetReq, slowGetRes);
        SecurityContext afterSecondLogin = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(principal(afterSecondLogin).getCustomerId()).isEqualTo("999999");
        assertThat(principal(afterSecondLogin).ownedCodexIds()).containsExactly("sourcebook-living-land");
    }

    @Test
    void loggedOutOrInvalidatedSessionsAreNeverResurrectedByInFlightRequests() {
        MockHttpSession session = new MockHttpSession();
        UUID gen = UUID.randomUUID();
        MockHttpServletRequest loginReq = new MockHttpServletRequest();
        loginReq.setSession(session);
        repository.saveExplicitAuthentication(
                contextWithBooks("424242", List.of("sourcebook-aysle")).getAuthentication(),
                DriveThruSessionSyncState.running(gen, STARTED_AT).withBatchApplied(),
                FIRST_KEY,
                loginReq,
                new MockHttpServletResponse());

        MockHttpServletRequest inFlightReq = new MockHttpServletRequest();
        inFlightReq.setSession(session);
        MockHttpServletResponse inFlightRes = new MockHttpServletResponse();
        SecurityContext loadedBeforeLogout = repository.loadDeferredContext(inFlightReq).get();

        // Logout marks the session detached and clears context before invalidation
        MockHttpServletRequest logoutReq = new MockHttpServletRequest();
        logoutReq.setSession(session);
        repository.markLoggedOut(logoutReq);
        session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);

        repository.saveContext(loadedBeforeLogout, inFlightReq, inFlightRes);
        assertThat(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNull();

        session.invalidate();
        repository.saveContext(loadedBeforeLogout, inFlightReq, inFlightRes);
        assertThat(inFlightReq.getSession(false)).isNull();
    }

    @SuppressWarnings("deprecation")
    @Test
    void anonymousRequestDoesNotAllocateHttpSessionWhenSavingEmptyContext() {
        MockHttpServletRequest anonReq = new MockHttpServletRequest();
        MockHttpServletResponse anonRes = new MockHttpServletResponse();
        SecurityContext loaded = repository.loadContext(new HttpRequestResponseHolder(anonReq, anonRes));
        repository.saveContext(loaded, anonReq, anonRes);
        assertThat(anonReq.getSession(false)).isNull();
    }

    private static SecurityContext contextWithBooks(final String customerId, final List<String> codexIds) {
        DrivethruToken token = new DrivethruToken();
        token.setCustomerId(customerId);
        DriveThruUserDetails details = new DriveThruUserDetails(token.sessionCopy(), codexIds);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(ApiKeyAuthenticationToken.authenticated(details));
        return context;
    }

    private static DriveThruUserDetails principal(final SecurityContext context) {
        return (DriveThruUserDetails) context.getAuthentication().getPrincipal();
    }
}
