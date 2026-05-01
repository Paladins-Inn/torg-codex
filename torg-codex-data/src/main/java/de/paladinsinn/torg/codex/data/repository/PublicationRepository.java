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

package de.paladinsinn.torg.codex.data.repository;

import de.paladinsinn.torg.codex.data.model.Publication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Publication} entities.
 */
public interface PublicationRepository extends JpaRepository<Publication, UUID> {

    /** Find by the stable codex identifier (e.g. {@code "core-rulebook"}). */
    Optional<Publication> findByCodexId(String codexId);

    /** Find by name (case-insensitive exact match). */
    Optional<Publication> findByNameIgnoreCase(String name);

    /** Find all publications whose product-id list contains the given product id. */
    @Query("SELECT p FROM Publication p WHERE :productId MEMBER OF p.productIds")
    List<Publication> findByProductId(@Param("productId") int productId);

    /** Find by the canonical (primary) product id. */
    Optional<Publication> findByPrimaryProductId(int primaryProductId);

    /** Find all third-party publications by author name. */
    List<Publication> findByThirdParty(String thirdParty);

    /** Find all first-party publications (no third-party author). */
    List<Publication> findByThirdPartyIsNull();
}

