package de.paladinsinn.torg.codex.data.repository;

import de.paladinsinn.torg.codex.data.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {

    Optional<Item> findByNameIgnoreCase(String name);

    List<Item> findByNameContainingIgnoreCase(String namePart);

    List<Item> findByType(String type);

    List<Item> findByCosm(String cosm);

    List<Item> findByClearanceLevel(String clearanceLevel);

    List<Item> findByTypeAndCosm(String type, String cosm);

    @Query("SELECT i FROM Item i WHERE :product MEMBER OF i.products")
    List<Item> findByProduct(@Param("product") String product);

    @Query("SELECT i FROM Item i WHERE i.type = :type AND :product MEMBER OF i.products")
    List<Item> findByTypeAndProduct(@Param("type") String type,
                                    @Param("product") String product);
}
