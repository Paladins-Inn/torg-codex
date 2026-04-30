package de.paladinsinn.torg.codex.data.repository;

import de.paladinsinn.torg.codex.data.model.Race;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RaceRepository extends JpaRepository<Race, UUID> {

    Optional<Race> findByNameIgnoreCase(String name);

    List<Race> findByNameContainingIgnoreCase(String namePart);

    List<Race> findByMajor(boolean major);

    List<Race> findByClearanceLevel(String clearanceLevel);

    @Query("SELECT r FROM Race r WHERE :product MEMBER OF r.products")
    List<Race> findByProduct(@Param("product") String product);
}
