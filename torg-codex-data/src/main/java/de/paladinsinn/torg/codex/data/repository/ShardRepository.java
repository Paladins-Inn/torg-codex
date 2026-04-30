package de.paladinsinn.torg.codex.data.repository;

import de.paladinsinn.torg.codex.data.model.Shard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShardRepository extends JpaRepository<Shard, UUID> {

    Optional<Shard> findByNameIgnoreCase(String name);

    List<Shard> findByNameContainingIgnoreCase(String namePart);

    List<Shard> findByCosm(String cosm);

    List<Shard> findByClearanceLevel(String clearanceLevel);

    @Query("SELECT s FROM Shard s WHERE :product MEMBER OF s.products")
    List<Shard> findByProduct(@Param("product") String product);
}
