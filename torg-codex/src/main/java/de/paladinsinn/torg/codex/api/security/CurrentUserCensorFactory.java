package de.paladinsinn.torg.codex.api.security;

import de.paladinsinn.torg.codex.data.markup.Censor;
import de.paladinsinn.torg.codex.data.markup.TorgMarkupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Primary-adapter component that creates a {@link Censor} for the current request.
 *
 * <p>The set of owned products is resolved solely through {@link ProductOwnershipResolver} — the
 * single, principal-type-agnostic {@code ROLE_<codex-id>}-authority mechanism required by
 * constitution Principle V (feature {@code 001-unify-censoring-authorization}). This class no
 * longer derives ownership from any authentication-provider-specific principal.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserCensorFactory {

    private final ProductOwnershipResolver ownershipResolver;
    private final TorgMarkupService markupService;

    public Censor create() {
        return Censor.of(markupService, ownershipResolver.resolve());
    }
}
