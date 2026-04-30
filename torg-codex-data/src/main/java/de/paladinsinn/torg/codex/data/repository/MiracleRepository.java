package de.paladinsinn.torg.codex.data.repository;

import de.paladinsinn.torg.codex.data.model.Miracle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MiracleRepository extends JpaRepository<Miracle, UUID> {

    Optional<Miracle> findByNameIgnoreCase(String name);

    List<Miracle> findByNameContainingIgnoreCase(String namePart);

    List<Miracle> findByClearanceLevel(String clearanceLevel);


    @Query("SELECT m FROM Miracle m WHERE :product MEMBER OF m.products")
    List<Miracle> findByProduct(@Param("product") String product);
}
