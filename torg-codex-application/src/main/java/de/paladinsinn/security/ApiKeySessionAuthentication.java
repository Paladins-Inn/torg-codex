package de.paladinsinn.security;

import de.paladinsinn.drivethru.ApiKeyFingerprint;
import de.paladinsinn.drivethru.DriveThruRPGService;
import de.paladinsinn.drivethru.token.DrivethruToken;
import de.paladinsinn.drivethru.token.NoValidTokenException;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.util.WebUtils;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Coordinates interactive token-first browser logins and session-aware header logins while storing
 * only non-reversible {@link ApiKeyFingerprint} digests and key-free {@link DrivethruToken#sessionCopy()}
 * tokens (FR-001, FR-002, FR-010, FR-013, FR-018, FR-019, ADR-023).
 */
@Slf4j
public class ApiKeySessionAuthentication {

    public static final String NOTIFICATION_CHANNEL_ATTR = "torg.codex.notificationChannelId";
    public static final String KEY_FINGERPRINT_ATTR =
            ApiKeySessionAuthentication.class.getName() + ".keyFingerprint";

    private final AuthenticationManager authenticationManager;
    private final GenerationAwareSecurityContextRepository contextRepository;
    private final SessionAuthenticationStrategy sessionStrategy;

    @Setter
    private DriveThruRPGService driveThruRPGService;

    @Setter
    private DriveThruLibrarySynchronizer synchronizer;

    @Setter
    private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

    /**
     * Creates a session authentication helper with default repositories.
     *
     * @param authenticationManager DriveThruRPG authentication manager
     */
    public ApiKeySessionAuthentication(final AuthenticationManager authenticationManager) {
        this(
                authenticationManager,
                new HttpSessionCsrfTokenRepository(),
                new GenerationAwareSecurityContextRepository());
    }

    /**
     * Creates a session authentication helper sharing the filter chain's {@link HttpSessionCsrfTokenRepository}.
     *
     * @param authenticationManager DriveThruRPG authentication manager
     * @param csrfTokenRepository   shared session CSRF token repository
     */
    public ApiKeySessionAuthentication(
            final AuthenticationManager authenticationManager,
            final HttpSessionCsrfTokenRepository csrfTokenRepository) {
        this(authenticationManager, csrfTokenRepository, new GenerationAwareSecurityContextRepository());
    }

    /**
     * Creates a session authentication helper sharing both CSRF and generation-aware context repositories.
     *
     * @param authenticationManager DriveThruRPG authentication manager
     * @param csrfTokenRepository   shared session CSRF token repository
     * @param contextRepository     shared generation-aware security context repository
     */
    public ApiKeySessionAuthentication(
            final AuthenticationManager authenticationManager,
            final HttpSessionCsrfTokenRepository csrfTokenRepository,
            final GenerationAwareSecurityContextRepository contextRepository) {
        this.authenticationManager = Objects.requireNonNull(authenticationManager, "authenticationManager");
        Objects.requireNonNull(csrfTokenRepository, "csrfTokenRepository");
        this.contextRepository = Objects.requireNonNull(contextRepository, "contextRepository");
        this.sessionStrategy = new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(),
                new CsrfAuthenticationStrategy(csrfTokenRepository)));
    }

    /**
     * Token-first interactive browser sign-in: synchronously validates the API key, rotates session
     * and CSRF identifiers, establishes initial free-tier access immediately, and dispatches
     * asynchronous background library streaming (FR-001, FR-002, SC-001).
     *
     * @param apiKey    submitted API key
     * @param channelId opaque session notification channel ID, or {@code null}
     * @param request   current HTTP request
     * @param response  current HTTP response
     * @return initial session authentication
     */
    public Authentication authenticateInteractive(
            final String apiKey,
            final String channelId,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        if (driveThruRPGService == null || synchronizer == null) {
            return authenticate(apiKey, request, response);
        }
        String normalizedKey = apiKey.strip();
        DrivethruToken validatedToken = validateTokenWithObservation(normalizedKey);
        ApiKeyFingerprint fingerprint = ApiKeyFingerprint.of(normalizedKey);
        DrivethruToken sessionToken = validatedToken.sessionCopy();

        Optional<DriveThruLibrarySynchronizer.CachedLibraryResolution> cachedOpt =
                synchronizer.resolveCachedLibrary(fingerprint);

        HttpSession session = request.getSession(true);
        UUID generationId = UUID.randomUUID();
        Authentication initialAuthentication;
        String effectiveChannelId;
        synchronized (WebUtils.getSessionMutex(session)) {
            if (cachedOpt.isPresent()) {
                DriveThruLibrarySynchronizer.CachedLibraryResolution cached = cachedOpt.get();
                if (sessionToken.getCustomerId() == null && cached.customerId() != null) {
                    sessionToken.setCustomerId(cached.customerId());
                }
                initialAuthentication = ApiKeyAuthenticationToken.authenticated(
                        new DriveThruUserDetails(sessionToken, cached.ownedCodexIds()));
                sessionStrategy.onAuthentication(initialAuthentication, request, response);
                effectiveChannelId = resolveOrCreateChannelId(session, channelId);
                DriveThruSessionSyncState idleState = DriveThruSessionSyncState.idle(generationId, Instant.now());
                contextRepository.saveExplicitAuthentication(
                        initialAuthentication, idleState, fingerprint, request, response);
            } else {
                List<String> initialOwned = existingCompletedOrPartialIdsForSameKey(session, fingerprint);
                initialAuthentication = ApiKeyAuthenticationToken.authenticated(
                        new DriveThruUserDetails(sessionToken, initialOwned));
                sessionStrategy.onAuthentication(initialAuthentication, request, response);
                effectiveChannelId = resolveOrCreateChannelId(session, channelId);
                DriveThruSessionSyncState syncState = DriveThruSessionSyncState.running(generationId, Instant.now());
                contextRepository.saveExplicitAuthentication(
                        initialAuthentication, syncState, fingerprint, request, response);
            }
        }

        if (cachedOpt.isPresent()) {
            synchronizer.notifyCachedReady(effectiveChannelId);
        } else {
            synchronizer.startInitialSync(session, effectiveChannelId, fingerprint, sessionToken, generationId);
        }
        return initialAuthentication;
    }

    /**
     * Authenticates a changed key or restores the current session's completed synchronous result.
     * Concurrent requests sharing a session wait for the same authentication attempt.
     *
     * @param apiKey   submitted API key
     * @param request  current request
     * @param response current response
     * @return the fully authenticated principal and publication authorities
     */
    public Authentication authenticate(
            final String apiKey, final HttpServletRequest request, final HttpServletResponse response) {
        String normalizedKey = apiKey.strip();
        ApiKeyFingerprint fingerprint = ApiKeyFingerprint.of(normalizedKey);
        HttpSession session = request.getSession(true);
        synchronized (WebUtils.getSessionMutex(session)) {
            Authentication reusable = completedSessionAuthentication(session, fingerprint);
            if (reusable != null) {
                restore(reusable);
                return reusable;
            }

            Authentication authenticated = sanitizeForSession(authenticationManager.authenticate(
                    ApiKeyAuthenticationToken.unauthenticated(normalizedKey)));
            sessionStrategy.onAuthentication(authenticated, request, response);
            contextRepository.saveExplicitAuthentication(authenticated, null, fingerprint, request, response);
            return authenticated;
        }
    }

    /**
     * Resolves a stateless {@code /api/**} header request that happens to carry an existing browser
     * session cookie: reuses the session result only if already complete for the same key, and
     * otherwise resolves full ownership for this request without clobbering an active or newer
     * browser session (T022).
     *
     * @param apiKey   submitted header API key
     * @param request  current HTTP request
     * @return full single-request authentication
     */
    public Authentication authenticateHeaderWithExistingSession(
            final String apiKey,
            final HttpServletRequest request) {
        String normalizedKey = apiKey.strip();
        ApiKeyFingerprint fingerprint = ApiKeyFingerprint.of(normalizedKey);
        HttpSession session = request.getSession(false);
        if (session != null) {
            synchronized (WebUtils.getSessionMutex(session)) {
                Authentication reusable = completedSessionAuthentication(session, fingerprint);
                if (reusable != null) {
                    restore(reusable);
                    return reusable;
                }
            }
        }
        Authentication resolved = sanitizeForSession(authenticationManager.authenticate(
                ApiKeyAuthenticationToken.unauthenticated(normalizedKey)));
        restore(resolved);
        return resolved;
    }

    /**
     * Reads the active session's stored API-key fingerprint when present.
     *
     * @param session HTTP session, or {@code null}
     * @return stored fingerprint if present
     */
    public Optional<ApiKeyFingerprint> currentFingerprint(final HttpSession session) {
        if (session == null) {
            return Optional.empty();
        }
        synchronized (WebUtils.getSessionMutex(session)) {
            if (session.getAttribute(KEY_FINGERPRINT_ATTR) instanceof ApiKeyFingerprint fingerprint) {
                return Optional.of(fingerprint);
            }
            return Optional.empty();
        }
    }

    /**
     * Removes failed header authentication from both the request and its existing session when no
     * newer interactive session owns it.
     *
     * @param request  current request
     * @param response current response
     */
    public void clear(final HttpServletRequest request, final HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            synchronized (WebUtils.getSessionMutex(session)) {
                if (session.getAttribute(DriveThruSessionSyncState.SYNC_STATE_KEY) == null) {
                    session.removeAttribute(KEY_FINGERPRINT_ATTR);
                    contextRepository.saveContext(SecurityContextHolder.createEmptyContext(), request, response);
                }
            }
        }
        SecurityContextHolder.clearContext();
    }

    public GenerationAwareSecurityContextRepository getContextRepository() {
        return contextRepository;
    }

    private DrivethruToken validateTokenWithObservation(final String normalizedKey) {
        ObservationRegistry registry = observationRegistry != null ? observationRegistry : ObservationRegistry.NOOP;
        Observation observation = Observation.createNotStarted("drivethru.authentication", registry)
                .contextualName("drivethru authenticate")
                .start();
        try (Observation.Scope ignored = observation.openScope()) {
            log.atInfo()
                    .addKeyValue("event.action", "drivethru-auth-started")
                    .log("DriveThruRPG authentication started.");
            DrivethruToken token = driveThruRPGService.getToken(normalizedKey);
            observation.lowCardinalityKeyValue("outcome", "success");
            return token;
        } catch (NoValidTokenException invalidKey) {
            observation.lowCardinalityKeyValue("outcome", "rejected");
            observation.error(new IllegalStateException("DriveThruRPG authentication rejected"));
            log.atInfo()
                    .addKeyValue("event.action", "drivethru-auth-rejected")
                    .addKeyValue("reason", "invalid-api-key")
                    .log("DriveThruRPG authentication failed: invalid API key.");
            throw new BadCredentialsException("Invalid DriveThruRPG API key.", invalidKey);
        } catch (RuntimeException failure) {
            String outcome = failure instanceof BadCredentialsException ? "rejected" : "error";
            observation.lowCardinalityKeyValue("outcome", outcome);
            observation.error(new IllegalStateException("DriveThruRPG authentication " + outcome));
            throw failure;
        } finally {
            observation.stop();
        }
    }

    private static Authentication completedSessionAuthentication(
            final HttpSession session,
            final ApiKeyFingerprint fingerprint) {
        Object storedContext = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        if (!(session.getAttribute(KEY_FINGERPRINT_ATTR) instanceof ApiKeyFingerprint storedFingerprint)
                || !fingerprint.equals(storedFingerprint)
                || !(storedContext instanceof SecurityContext context)
                || !(context.getAuthentication() instanceof ApiKeyAuthenticationToken authentication)
                || !authentication.isAuthenticated()) {
            return null;
        }
        Object rawSync = session.getAttribute(DriveThruSessionSyncState.SYNC_STATE_KEY);
        if (rawSync instanceof DriveThruSessionSyncState syncState
                && syncState.status() != SyncStatus.COMPLETED
                && syncState.status() != SyncStatus.IDLE) {
            return null;
        }
        return authentication;
    }

    private static List<String> existingCompletedOrPartialIdsForSameKey(
            final HttpSession session,
            final ApiKeyFingerprint fingerprint) {
        if (session.getAttribute(KEY_FINGERPRINT_ATTR) instanceof ApiKeyFingerprint storedFingerprint
                && fingerprint.equals(storedFingerprint)
                && session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)
                        instanceof SecurityContext context
                && context.getAuthentication() instanceof ApiKeyAuthenticationToken auth
                && auth.getPrincipal() instanceof DriveThruUserDetails details) {
            return details.ownedCodexIds();
        }
        return List.of();
    }

    static Authentication sanitizeForSession(final Authentication authentication) {
        if (authentication instanceof ApiKeyAuthenticationToken apiAuth
                && apiAuth.getPrincipal() instanceof DriveThruUserDetails details
                && details.token() != null) {
            DrivethruToken sessionToken = details.token().sessionCopy();
            if (sessionToken != null) {
                return ApiKeyAuthenticationToken.authenticated(
                        new DriveThruUserDetails(sessionToken, details.ownedCodexIds()));
            }
        }
        return authentication;
    }

    private static String resolveOrCreateChannelId(final HttpSession session, final String suppliedChannelId) {
        if (suppliedChannelId != null && !suppliedChannelId.isBlank()) {
            session.setAttribute(NOTIFICATION_CHANNEL_ATTR, suppliedChannelId);
            return suppliedChannelId;
        }
        if (session.getAttribute(NOTIFICATION_CHANNEL_ATTR) instanceof String existing && !existing.isBlank()) {
            return existing;
        }
        String generated = UUID.randomUUID().toString();
        session.setAttribute(NOTIFICATION_CHANNEL_ATTR, generated);
        return generated;
    }

    private SecurityContext restore(final Authentication authentication) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        return context;
    }
}
