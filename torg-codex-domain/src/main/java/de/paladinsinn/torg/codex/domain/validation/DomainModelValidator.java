package de.paladinsinn.torg.codex.domain.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * Post-construction validator for domain models.
 *
 * <p>The constitution forbids evaluating Bean Validation constraints inside domain
 * constructors and forbids any Jakarta Bean Validation <em>provider</em> (e.g.
 * Hibernate Validator) on the domain classpath. This helper therefore only depends on
 * the {@code jakarta.validation-api} {@link Validator} abstraction; a concrete provider
 * is supplied by an outer (application/adapter) layer and passed in explicitly, so that
 * domain models are validated <em>after</em> construction via a dedicated method rather
 * than implicitly during construction.
 */
@RequiredArgsConstructor
public final class DomainModelValidator {

    private final Validator validator;

    /**
     * Validates a fully-constructed domain model.
     *
     * @param model the domain model to validate
     * @param <T>   the domain model type
     * @return the same {@code model} instance when valid (fluent use)
     * @throws ConstraintViolationException when any field constraint is violated
     */
    public <T> T validate(T model) {
        final Set<ConstraintViolation<T>> violations = validator.validate(model);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return model;
    }
}
