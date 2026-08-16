package de.paladinsinn.torg.codex.domain.validation;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;

class ValidationProviderAvailabilityTest {

    @Test
    void hibernateValidatorIsAvailableOnTheDomainTestClasspath() {
        assertThatNoException()
                .isThrownBy(() -> {
                    try (var factory = Validation.buildDefaultValidatorFactory()) {
                        factory.getValidator();
                    }
                });
    }
}
