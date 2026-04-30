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
