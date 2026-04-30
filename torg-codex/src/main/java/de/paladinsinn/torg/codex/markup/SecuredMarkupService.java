package de.paladinsinn.torg.codex.markup;

import de.paladinsinn.torg.codex.data.markup.TorgMarkupService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring-Security-aware wrapper around {@link TorgMarkupService}.
 *
 * <p>The markup language uses {@code <IF:product-id>} / {@code <IF:!product-id>} blocks to
 * gate content behind product ownership.  Each product maps to a Spring Security authority
 * named {@code ROLE_<product-id>} (e.g. {@code ROLE_core-rulebook}).
 *
 * <ul>
 *   <li>{@code <IF:core-rulebook>…</IF>} – rendered only when the authenticated user holds
 *       {@code ROLE_core-rulebook}.</li>
 *   <li>{@code <IF:!core-rulebook>…</IF>} – rendered only when the authenticated user does
 *       <em>not</em> hold {@code ROLE_core-rulebook} (typically a "you need this book" notice).
 *       </li>
 * </ul>
 *
 * <p>Unauthenticated or anonymous users are treated as owning no products, so only
 * {@code <IF:!…>} blocks are visible to them.
 */
@Service
public class SecuredMarkupService {

    static final String ROLE_PREFIX = "ROLE_";

    private final TorgMarkupService markupService;

    public SecuredMarkupService(TorgMarkupService markupService) {
        this.markupService = markupService;
    }

    /**
     * Renders {@code rawText} for the currently authenticated user.
     *
     * <p>Product-gated blocks are resolved against the user's Spring Security authorities.
     * Each {@code ROLE_<id>} authority is mapped to the product-id {@code <id>}.
     */
    public String render(String rawText) {
        return markupService.render(rawText, ownedProducts());
    }

    /**
     * Renders {@code rawText} for a caller-supplied set of owned product-ids.
     *
     * <p>Use this overload when the caller already holds a resolved product-id set, for example
     * in batch processing or programmatic contexts where no HTTP request is in scope.
     *
     * @param rawText       the unprocessed markup
     * @param ownedProducts product-ids the user owns (without the {@code ROLE_} prefix)
     */
    public String render(String rawText, Set<String> ownedProducts) {
        return markupService.render(rawText, ownedProducts);
    }

    // ---------------------------------------------------------------------------
    // internals
    // ---------------------------------------------------------------------------

    private Set<String> ownedProducts() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Collections.emptySet();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith(ROLE_PREFIX))
                .map(a -> a.substring(ROLE_PREFIX.length()))
                .collect(Collectors.toUnmodifiableSet());
    }
}
