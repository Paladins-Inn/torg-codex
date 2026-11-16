package de.paladinsinn.torg.codex.security;

import de.paladinsinn.drivethru.ApiKeyFingerprint;
import de.paladinsinn.drivethru.token.DrivethruToken;
import de.paladinsinn.drivethru.DriveThruRPGService;
import de.paladinsinn.security.ApiKeyAuthenticationToken;
import de.paladinsinn.security.ApiKeySessionAuthentication;
import de.paladinsinn.security.DriveThruUserDetails;
import de.paladinsinn.torg.codex.web.codex.CodexAuthController;
import de.paladinsinn.torg.codex.web.codex.mapper.CodexViewMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies session-local API-key reuse through the production security chain and login controller.
 */
@SpringJUnitWebConfig(ApiKeySessionAuthenticationTest.TestConfiguration.class)
class ApiKeySessionAuthenticationTest {

    private static final String API_KEY = "session-test-api-key";
    private static final String OTHER_KEY = "another-session-test-api-key";
    private static final String PROBE = "/session-probe";
    private static final int TIMEOUT_SECONDS = 5;

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private AuthenticationManager authenticationManager;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(authenticationManager);
        when(authenticationManager.authenticate(any())).thenReturn(authenticated("reader"));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void headerLoginIsReusedWithAndWithoutTheHeader() throws Exception {
        MvcResult login = mvc.perform(get(PROBE).header("Authorization", "ApiKey " + API_KEY))
                .andExpect(status().isOk()).andExpect(content().string("reader")).andReturn();
        MockHttpSession session = session(login);

        mvc.perform(get(PROBE).session(session).header("Authorization", "ApiKey " + API_KEY))
                .andExpect(content().string("reader"));
        mvc.perform(get(PROBE).session(session)).andExpect(content().string("reader"));
        mvc.perform(get("/session-access").session(session))
                .andExpect(status().isOk()).andExpect(content().string("unlocked"));

        verify(authenticationManager).authenticate(any());
    }

    @Test
    void formAndHeaderLoginShareTheSessionResult() throws Exception {
        MockHttpSession session = session(mvc.perform(post("/login").param("apiKey", API_KEY).with(csrf()))
                .andExpect(status().isFound()).andReturn());
        mvc.perform(post("/login").session(session).param("apiKey", " " + API_KEY + " ").with(csrf()))
                .andExpect(status().isFound());
        mvc.perform(get(PROBE).session(session).header("Authorization", "ApiKey " + API_KEY))
                .andExpect(content().string("reader"));

        verify(authenticationManager).authenticate(any());
    }

    @Test
    void headerLoginCanBeReusedByTheLoginForm() throws Exception {
        MockHttpSession session = session(mvc.perform(get(PROBE).header("Authorization", "ApiKey " + API_KEY))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(post("/login").session(session).param("apiKey", API_KEY).with(csrf()))
                .andExpect(status().isFound());

        verify(authenticationManager).authenticate(any());
    }

    @Test
    void theSameKeyInAnotherSessionIsAuthenticatedAgain() throws Exception {
        mvc.perform(get(PROBE).session(new MockHttpSession()).header("Authorization", "ApiKey " + API_KEY))
                .andExpect(content().string("reader"));
        mvc.perform(get(PROBE).session(new MockHttpSession()).header("Authorization", "ApiKey " + API_KEY))
                .andExpect(content().string("reader"));

        verify(authenticationManager, times(2)).authenticate(any());
    }

    @Test
    void changingTheKeyReplacesTheSessionIdentityAndRotatesTheSessionId() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenReturn(authenticated("reader"), authenticated("another-reader"));
        MockHttpSession session = new MockHttpSession();
        String anonymousId = session.getId();
        mvc.perform(get(PROBE).session(session).header("Authorization", "ApiKey " + API_KEY))
                .andExpect(content().string("reader"));
        assertThat(session.getId()).isNotEqualTo(anonymousId);
        String authenticatedId = session.getId();
        mvc.perform(get(PROBE).session(session).header("Authorization", "ApiKey " + OTHER_KEY))
                .andExpect(content().string("another-reader"));
        assertThat(session.getId()).isNotEqualTo(authenticatedId);
        mvc.perform(get(PROBE).session(session)).andExpect(content().string("another-reader"));

        verify(authenticationManager, times(2)).authenticate(any());
    }

    @Test
    void failuresAreNotCachedAndDoNotRestoreAnOldHeaderLogin() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get(PROBE).session(session).header("Authorization", "ApiKey " + API_KEY))
                .andExpect(content().string("reader"));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("rejected"));
        mvc.perform(get(PROBE).session(session).header("Authorization", "ApiKey " + OTHER_KEY))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(PROBE).session(session)).andExpect(content().string("anonymous"));

        doReturn(authenticated("reader")).when(authenticationManager).authenticate(any());
        mvc.perform(get(PROBE).session(session).header("Authorization", "ApiKey " + OTHER_KEY))
                .andExpect(content().string("reader"));
        verify(authenticationManager, times(3)).authenticate(any());
    }

    @Test
    void upstreamFailureCanBeRetriedInTheSameSession() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new AuthenticationServiceException("unavailable"))
                .thenReturn(authenticated("reader"));
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get(PROBE).session(session).header("Authorization", "ApiKey " + API_KEY))
                .andExpect(status().isServiceUnavailable());
        mvc.perform(get(PROBE).session(session).header("Authorization", "ApiKey " + API_KEY))
                .andExpect(content().string("reader"));
        verify(authenticationManager, times(2)).authenticate(any());
    }

    @Test
    void rejectedFormLoginIsRetriedRatherThanCached() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("rejected"))
                .thenReturn(authenticated("reader"));
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/login").session(session).param("apiKey", API_KEY).with(csrf()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/login").session(session).param("apiKey", API_KEY).with(csrf()))
                .andExpect(status().isFound());
        mvc.perform(get(PROBE).session(session)).andExpect(content().string("reader"));
        verify(authenticationManager, times(2)).authenticate(any());
    }

    @Test
    void anExpiredSessionCannotSupplyTheNextLoginResult() throws Exception {
        MockHttpSession session = session(mvc.perform(post("/login").param("apiKey", API_KEY).with(csrf()))
                .andExpect(status().isFound()).andReturn());
        session.invalidate();
        mvc.perform(get(PROBE)).andExpect(content().string("anonymous"));
        mvc.perform(post("/login").param("apiKey", API_KEY).with(csrf())).andExpect(status().isFound());
        verify(authenticationManager, times(2)).authenticate(any());
    }

    @Test
    void credentialAndOwnershipResultsHaveNoApplicationWideSpringCache() throws Exception {
        assertThat(DriveThruRPGService.class.getMethod("getToken", String.class).getAnnotation(Cacheable.class))
                .isNull();
        assertThat(DriveThruRPGService.class.getMethod("getIdsOfOwnedProducts", DrivethruToken.class)
                .getAnnotation(Cacheable.class)).isNull();
    }

    @Test
    void logoutDiscardsTheCachedResult() throws Exception {
        MockHttpSession session = session(mvc.perform(post("/login").param("apiKey", API_KEY).with(csrf()))
                .andExpect(status().isFound()).andReturn());
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(status().isFound());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get(PROBE)).andExpect(content().string("anonymous"));
        mvc.perform(post("/login").param("apiKey", API_KEY).with(csrf())).andExpect(status().isFound());

        verify(authenticationManager, times(2)).authenticate(any());
    }

    @Test
    void formAndHeaderSessionsStoreFingerprintAndKeyFreeTokensWithoutRetainingRawApiKey() throws Exception {
        MockHttpSession formSession = session(mvc.perform(post("/login").param("apiKey", API_KEY).with(csrf()))
                .andExpect(status().isFound()).andReturn());
        assertSessionStripsCleartextKey(formSession, API_KEY);

        MockHttpSession headerSession = session(mvc.perform(get(PROBE).header("Authorization", "ApiKey " + OTHER_KEY))
                .andExpect(status().isOk()).andReturn());
        assertSessionStripsCleartextKey(headerSession, OTHER_KEY);
    }

    private static void assertSessionStripsCleartextKey(final MockHttpSession session, final String submittedKey) {
        Object storedFingerprint = session.getAttribute(ApiKeySessionAuthentication.KEY_FINGERPRINT_ATTR);
        assertThat(storedFingerprint).isEqualTo(ApiKeyFingerprint.of(submittedKey));
        assertThat(String.valueOf(storedFingerprint)).doesNotContain(submittedKey);

        SecurityContext storedContext = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
        assertThat(storedContext).isNotNull();
        DriveThruUserDetails principal = (DriveThruUserDetails) storedContext.getAuthentication().getPrincipal();
        assertThat(principal.token().getApplicationKey()).isNull();
    }

    @Test
    void concurrentRequestsInOneSessionShareOneSuccessfulLogin() throws Exception {
        CountDownLatch requestsStarted = new CountDownLatch(2);
        CountDownLatch authenticationStarted = new CountDownLatch(1);
        CountDownLatch finishAuthentication = new CountDownLatch(1);
        when(authenticationManager.authenticate(any())).thenAnswer(invocation -> {
            authenticationStarted.countDown();
            assertThat(finishAuthentication.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
            return authenticated("reader");
        });
        MockHttpSession session = new MockHttpSession();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                requestsStarted.countDown();
                return mvc.perform(get(PROBE).session(session).header("Authorization", "ApiKey " + API_KEY))
                        .andExpect(content().string("reader")).andReturn();
            });
            var second = executor.submit(() -> {
                requestsStarted.countDown();
                return mvc.perform(get(PROBE).session(session).header("Authorization", "ApiKey " + API_KEY))
                        .andExpect(content().string("reader")).andReturn();
            });
            try {
                assertThat(requestsStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
                assertThat(authenticationStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
            } finally {
                finishAuthentication.countDown();
            }
            first.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            second.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        verify(authenticationManager).authenticate(any());
    }

    private MockHttpSession session(final MvcResult result) {
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }

    private static Authentication authenticated(final String username) {
        DrivethruToken token = new DrivethruToken();
        token.setCustomerId(username);
        token.setApplicationKey("raw-key-that-must-never-stay-in-session-" + API_KEY + "-" + OTHER_KEY);
        DriveThruUserDetails user = new DriveThruUserDetails(token, List.of("sourcebook-aysle"));
        return ApiKeyAuthenticationToken.authenticated(user);
    }

    /** Minimal web context without database or external HTTP services. */
    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @Import({TorgCodexSecurityConfig.class, CodexAuthController.class, ProbeController.class})
    static class TestConfiguration {
        @Bean
        AuthenticationManager authenticationManager() {
            return mock(AuthenticationManager.class);
        }

        @Bean
        CodexViewMapper viewMapper() {
            return mock(CodexViewMapper.class);
        }
    }

    /** Exposes the effective request identity to verify security-context persistence. */
    @RestController
    static class ProbeController {
        @GetMapping(PROBE)
        String identity(final Authentication authentication) {
            return authentication == null ? "anonymous" : authentication.getName();
        }

        @GetMapping("/session-access")
        @PreAuthorize("hasAuthority('ROLE_sourcebook-aysle')")
        String unlocked() {
            return "unlocked";
        }
    }
}
