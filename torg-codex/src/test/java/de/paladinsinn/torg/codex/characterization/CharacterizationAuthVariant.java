package de.paladinsinn.torg.codex.characterization;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

enum CharacterizationAuthVariant {
    ANONYMOUS(null),
    SOURCEBOOK_AYSLE_OWNER(user("fixture-owner").authorities(List.of(
            new SimpleGrantedAuthority("ROLE_core-rulebook"),
            new SimpleGrantedAuthority("ROLE_sourcebook-aysle"))));

    private final RequestPostProcessor requestPostProcessor;

    CharacterizationAuthVariant(RequestPostProcessor requestPostProcessor) {
        this.requestPostProcessor = requestPostProcessor;
    }

    RequestPostProcessor requestPostProcessor() {
        return requestPostProcessor;
    }
}
