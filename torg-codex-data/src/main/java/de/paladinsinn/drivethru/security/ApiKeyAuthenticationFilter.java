package de.paladinsinn.drivethru.security;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
/**
 * Servlet filter that extracts a DriveThruRPG API key from the
 * {@code Authorization: ApiKey <key>} header an authenticates the request.
 *
 * <p>If no {@code ApiKey} header is present the filter passes the request through
 * unchanged (other security rules may still block anonymous access).</p>
 *
 * <p>On authentication failure a {@code 401 Unauthorized} response is returned
 * immediately.</p>
 */
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    /** Authorization scheme prefix expected in the {@code Authorization} header. */
    public static final String API_KEY_PREFIX = "ApiKey ";
    private final AuthenticationManager authenticationManager;
    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {
        final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(API_KEY_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }
        final String apiKey = header.substring(API_KEY_PREFIX.length()).strip();
        log.debug("Received API key authentication request.");
        try {
            final Authentication auth = authenticationManager.authenticate(
                    ApiKeyAuthenticationToken.unauthenticated(apiKey));
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("API key authentication successful. user={}", auth.getName());
            filterChain.doFilter(request, response);
        } catch (final AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            log.warn("API key authentication failed: {}", ex.getMessage());
            response.sendError(HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        }
    }
}
