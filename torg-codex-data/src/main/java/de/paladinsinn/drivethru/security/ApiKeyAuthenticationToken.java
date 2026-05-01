package de.paladinsinn.drivethru.security;
import org.springframework.security.authentication.AbstractAuthenticationToken;
/**
 * Spring Security token for DriveThruRPG API-key based authentication.
 * Unauthenticated: principal = raw API key (String).
 * Authenticated: principal = DriveThruUserDetails, credentials cleared.
 */
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {
    private final Object principal;
    private Object credentials;
    public static ApiKeyAuthenticationToken unauthenticated(final String apiKey) {
        return new ApiKeyAuthenticationToken(apiKey, apiKey);
    }
    public static ApiKeyAuthenticationToken authenticated(final DriveThruUserDetails userDetails) {
        final ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(userDetails, null);
        token.setAuthenticated(true);
        return token;
    }
    private ApiKeyAuthenticationToken(final Object principal, final Object credentials) {
        super(principal instanceof DriveThruUserDetails ud ? ud.getAuthorities() : null);
        this.principal   = principal;
        this.credentials = credentials;
    }
    @Override public Object getPrincipal()   { return principal; }
    @Override public Object getCredentials() { return credentials; }
    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        credentials = null;
    }
}
