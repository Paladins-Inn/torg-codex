package de.paladinsinn.torg.codex.data.repository;

import de.paladinsinn.torg.codex.data.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    Optional<Vehicle> findByNameIgnoreCase(String name);

    List<Vehicle> findByNameContainingIgnoreCase(String namePart);

    List<Vehicle> findByType(String type);

    List<Vehicle> findByCosm(String cosm);

    List<Vehicle> findBySize(String size);

    List<Vehicle> findByUnique(boolean unique);

    List<Vehicle> findByClearanceLevel(String clearanceLevel);

    @Query("SELECT v FROM Vehicle v WHERE :product MEMBER OF v.products")
    List<Vehicle> findByProduct(@Param("product") String product);
}
