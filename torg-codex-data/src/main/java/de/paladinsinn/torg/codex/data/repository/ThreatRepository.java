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

import de.paladinsinn.torg.codex.data.model.Threat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ThreatRepository extends JpaRepository<Threat, UUID> {

    Optional<Threat> findByNameIgnoreCase(String name);

    List<Threat> findByNameContainingIgnoreCase(String namePart);

    List<Threat> findByCosm(String cosm);

    List<Threat> findByUnique(boolean unique);

    List<Threat> findByClearanceLevel(String clearanceLevel);

    @Query("SELECT t FROM Threat t WHERE :product MEMBER OF t.products")
    List<Threat> findByProduct(@Param("product") String product);

    @Query("SELECT t FROM Threat t WHERE t.cosm = :cosm AND :product MEMBER OF t.products")
    List<Threat> findByCosmAndProduct(@Param("cosm") String cosm,
                                      @Param("product") String product);
}
