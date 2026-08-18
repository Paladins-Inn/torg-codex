/*
 * Copyright (c) 2026.  Roland T. Lichti <rlichti@kaiserpfalz-edv.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * ERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * You may contact me via email rlichti@kaiserpfalz-edv.de or via mail
 *
 * Kaiserpfalz EDV-Service
 * Roland T. Lichti
 * Darmstädter Str. 12
 * 64625 Bensheim
 * GERMANY
 */

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
