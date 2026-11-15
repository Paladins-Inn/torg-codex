package de.paladinsinn.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.util.WebUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Reuses a completed API-key login only within its HTTP session.
 */
@RequiredArgsConstructor
public class ApiKeySessionAuthentication {

    private static final String KEY_FINGERPRINT = ApiKeySessionAuthentication.class.getName() + ".keyFingerprint";

    private final AuthenticationManager authenticationManager;
    private final HttpSessionSecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();
    private final ChangeSessionIdAuthenticationStrategy sessionStrategy = new ChangeSessionIdAuthenticationStrategy();

    /**
     * Authenticates a changed key or restores the current session's successful result.
     * Concurrent requests sharing a session wait for the same authentication attempt.
     *
     * @param apiKey submitted API key
     * @param request current request
     * @param response current response
     * @return the fully authenticated principal and publication authorities
     */
    public Authentication authenticate(
            final String apiKey, final HttpServletRequest request, final HttpServletResponse response) {
        String normalizedKey = apiKey.strip();
        byte[] fingerprint = fingerprint(normalizedKey);
        HttpSession session = request.getSession(true);
        synchronized (WebUtils.getSessionMutex(session)) {
            Object storedContext = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            if (session.getAttribute(KEY_FINGERPRINT) instanceof byte[] storedFingerprint
                    && MessageDigest.isEqual(fingerprint, storedFingerprint)
                    && storedContext instanceof SecurityContext context
                    && context.getAuthentication() instanceof ApiKeyAuthenticationToken authentication
                    && authentication.isAuthenticated()) {
                restore(authentication);
                return authentication;
            }

            Authentication authentication = authenticationManager.authenticate(
                    ApiKeyAuthenticationToken.unauthenticated(normalizedKey));
            sessionStrategy.onAuthentication(authentication, request, response);
            session.setAttribute(KEY_FINGERPRINT, fingerprint);
            contextRepository.saveContext(restore(authentication), request, response);
            return authentication;
        }
    }

    /**
     * Removes failed header authentication from both the request and its existing session.
     *
     * @param request current request
     * @param response current response
     */
    public void clear(final HttpServletRequest request, final HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            synchronized (WebUtils.getSessionMutex(session)) {
                session.removeAttribute(KEY_FINGERPRINT);
                contextRepository.saveContext(SecurityContextHolder.createEmptyContext(), request, response);
            }
        }
        SecurityContextHolder.clearContext();
    }

    private SecurityContext restore(final Authentication authentication) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        return context;
    }

    private static byte[] fingerprint(final String apiKey) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(apiKey.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK does not support SHA-256", exception);
        }
    }
}
