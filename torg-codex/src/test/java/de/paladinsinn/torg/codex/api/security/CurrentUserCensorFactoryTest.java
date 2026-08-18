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

package de.paladinsinn.torg.codex.api.security;

import de.paladinsinn.torg.codex.domain.markup.Censor;
import de.paladinsinn.torg.codex.domain.markup.TorgMarkupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Focused test for {@link CurrentUserCensorFactory} (feature
 * {@code 001-unify-censoring-authorization}, User Story 1, FR-001/SC-005).
 *
 * <p>Proves the factory delegates ownership resolution to {@link ProductOwnershipResolver} and
 * feeds exactly that resolved set into the {@link Censor}/{@link TorgMarkupService} pipeline, and
 * that it no longer depends on the DriveThruRPG-specific {@code DriveThruUserService}.
 */
@ExtendWith(MockitoExtension.class)
class CurrentUserCensorFactoryTest {

    @Mock
    ProductOwnershipResolver resolver;

    @Mock
    TorgMarkupService markupService;

    @InjectMocks
    CurrentUserCensorFactory factory;

    @Test
    void create_passesResolverOwnedProductsIntoCensor() {
        when(resolver.resolve()).thenReturn(Set.of("core-rulebook", "sourcebook-aysle"));
        when(markupService.render(any(), any())).thenReturn("rendered");

        Censor censor = factory.create();
        censor.apply("raw");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(markupService).render(eq("raw"), captor.capture());
        assertThat(captor.getValue())
                .containsExactlyInAnyOrder("core-rulebook", "sourcebook-aysle");
    }

    @Test
    void create_withEmptyOwnership_passesEmptySet() {
        when(resolver.resolve()).thenReturn(Set.of());
        when(markupService.render(any(), any())).thenReturn("rendered");

        factory.create().apply("raw");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(markupService).render(eq("raw"), captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void factory_hasNoDriveThruUserServiceDependency() {
        assertThat(Arrays.stream(CurrentUserCensorFactory.class.getDeclaredFields())
                .map(Field::getType)
                .map(Class::getName))
                .as("censoring path must not depend on the DriveThruRPG-specific user service")
                .noneMatch(name -> name.contains("DriveThruUserService"));
    }
}
