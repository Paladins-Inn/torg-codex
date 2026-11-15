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

package de.paladinsinn.torg.codex.drmfixtures.valid;

import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.domain.model.Article;
import de.paladinsinn.torg.codex.domain.model.Publication;
import de.paladinsinn.torg.codex.domain.model.Tag;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Valid: the three public reference-data areas bound to unrestricted queries. */
@TestConfiguration(proxyBeanMethods = false)
public class PublicQueryConfiguration {

    @Bean
    CatalogQuery<Article> articles() {
        return new EmptyQuery<>();
    }

    @Bean
    CatalogQuery<Publication> publications() {
        return new EmptyQuery<>();
    }

    @Bean
    CatalogQuery<Tag> tags() {
        return new EmptyQuery<>();
    }

    /**
     * An empty query.
     *
     * @param <T> the entry type
     */
    public static final class EmptyQuery<T> implements CatalogQuery<T> {

        @Override
        public List<T> findAll() {
            return List.of();
        }

        @Override
        public Optional<T> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public List<T> findByCosm(String cosm) {
            return List.of();
        }
    }
}
