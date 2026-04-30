package de.paladinsinn.torg.codex.data.repository;

import de.paladinsinn.torg.codex.data.model.Power;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PowerRepository extends JpaRepository<Power, UUID> {

    Optional<Power> findByNameIgnoreCase(String name);

    List<Power> findByNameContainingIgnoreCase(String namePart);

    List<Power> findByClearanceLevel(String clearanceLevel);


    @Query("SELECT p FROM Power p WHERE :product MEMBER OF p.products")
    List<Power> findByProduct(@Param("product") String product);
}
