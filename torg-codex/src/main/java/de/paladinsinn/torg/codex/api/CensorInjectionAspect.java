package de.paladinsinn.torg.codex.api;
import de.paladinsinn.security.DriveThruUserService;
import de.paladinsinn.security.NotLoggedInUserDetails;
import de.paladinsinn.torg.codex.data.markup.Censor;
import de.paladinsinn.torg.codex.data.markup.TorgMarkupService;
import de.paladinsinn.torg.codex.data.model.TorgEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
/**
 * AOP aspect that automatically injects a {@link Censor} into every {@link TorgEntity}
 * returned by any Spring Data repository in the codex data layer.
 *
 * <p>This frees callers from having to invoke {@link TorgEntity#withCensor(Censor)}
 * manually before accessing rendered text getters.  The {@link Censor} is built from
 * the owned product IDs of the currently authenticated user (or the free-tier product
 * for anonymous requests).</p>
 *
 * <p>The pointcut covers all public methods of all interfaces in the
 * {@code de.paladinsinn.torg.codex.data.repository} package, which matches every
 * Spring Data JPA repository declared in {@code torg-codex-data}.</p>
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class CensorInjectionAspect {
    private final DriveThruUserService userService;
    private final TorgMarkupService markupService;
    // -------------------------------------------------------------------------
    // Advice
    // -------------------------------------------------------------------------
    /**
     * After any repository method returns, walk the result and call
     * {@link TorgEntity#withCensor(Censor)} on every entity found.
     *
     * <p>Supported return shapes: a single {@link TorgEntity}, an
     * {@link Optional} wrapping one, or any {@link Iterable} (covers
     * {@code List}, {@code Set}, {@code Page}, etc.).</p>
     */
    @AfterReturning(
            pointcut = "execution(* de.paladinsinn.torg.codex.data.repository.*Repository.*(..))",
            returning = "result")
    public void injectCensor(final Object result) {
        if (result == null) return;
        final Censor censor = buildCensor();
        apply(result, censor);
    }
    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private void apply(final Object result, final Censor censor) {
        if (result instanceof TorgEntity entity) {
            entity.withCensor(censor);
        } else if (result instanceof Optional<?> opt) {
            opt.ifPresent(item -> apply(item, censor));
        } else if (result instanceof Iterable<?> iterable) {
            iterable.forEach(item -> apply(item, censor));
        }
    }
    private Censor buildCensor() {
        final Set<String> owned = userService.getCurrentUser()
                .map(u -> new HashSet<>(u.getOwnedCodexIds()))
                .map(s -> (Set<String>) s)
                .orElse(Set.of(NotLoggedInUserDetails.FREE_PRODUCT_ID));
        return Censor.of(markupService, owned);
    }
}
