package de.paladinsinn.torg.codex.api.security;

import de.paladinsinn.security.DriveThruUserService;
import de.paladinsinn.security.NotLoggedInUserDetails;
import de.paladinsinn.torg.codex.data.markup.Censor;
import de.paladinsinn.torg.codex.data.markup.TorgMarkupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Primary-adapter component that creates a censor for the authenticated request.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserCensorFactory {

    private final DriveThruUserService userService;
    private final TorgMarkupService markupService;

    public Censor create() {
        final Set<String> owned = userService.getCurrentUser()
                .map(user -> new HashSet<>(user.getOwnedCodexIds()))
                .map(Set::<String>copyOf)
                .orElse(Set.of(NotLoggedInUserDetails.FREE_PRODUCT_ID));
        return Censor.of(markupService, owned);
    }
}
