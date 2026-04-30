package de.paladinsinn.torg.codex.data.repository;

import de.paladinsinn.torg.codex.data.model.Cosm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CosmRepository extends JpaRepository<Cosm, UUID> {

    Optional<Cosm> findByNameIgnoreCase(String name);

    List<Cosm> findByNameContainingIgnoreCase(String namePart);

    @Query("SELECT c FROM Cosm c WHERE :product MEMBER OF c.products")
    List<Cosm> findByProduct(@Param("product") String product);
}
