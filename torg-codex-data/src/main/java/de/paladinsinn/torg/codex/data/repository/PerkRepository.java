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
