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

import de.paladinsinn.torg.codex.data.model.Perk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PerkRepository extends JpaRepository<Perk, UUID> {

    Optional<Perk> findByNameIgnoreCase(String name);

    List<Perk> findByNameContainingIgnoreCase(String namePart);

    List<Perk> findByCosm(String cosm);

    List<Perk> findByGroup(String group);

    List<Perk> findByContradiction(boolean contradiction);

    List<Perk> findByClearanceLevel(String clearanceLevel);

    @Query("SELECT p FROM Perk p WHERE :product MEMBER OF p.products")
    List<Perk> findByProduct(@Param("product") String product);

    @Query("SELECT p FROM Perk p WHERE p.cosm = :cosm AND :product MEMBER OF p.products")
    List<Perk> findByCosmAndProduct(@Param("cosm") String cosm,
                                    @Param("product") String product);
}
