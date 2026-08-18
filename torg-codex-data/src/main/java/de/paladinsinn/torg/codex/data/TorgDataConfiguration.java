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

package de.paladinsinn.torg.codex.data;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
/**
 * Spring configuration that activates all Torg Codex data-layer components:
 * JPA entities, repositories, markup services, and model classes.
 * Import via {@link EnableTorgData}.
 */
@Configuration
@ComponentScan({
    "de.paladinsinn.torg.codex.data"
})
@EntityScan({
    "de.paladinsinn.torg.codex.data.model",
    "de.kaiserpfalz.liquibase"
})
@EnableJpaRepositories({
    "de.paladinsinn.torg.codex.data.repository",
    "de.kaiserpfalz.liquibase"
})
public class TorgDataConfiguration {}
