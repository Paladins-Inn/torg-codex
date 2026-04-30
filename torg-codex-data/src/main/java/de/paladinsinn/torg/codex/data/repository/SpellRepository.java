package de.paladinsinn.torg.codex.data.repository;

import de.paladinsinn.torg.codex.data.model.Spell;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpellRepository extends JpaRepository<Spell, UUID> {

    Optional<Spell> findByNameIgnoreCase(String name);

    List<Spell> findByNameContainingIgnoreCase(String namePart);

    List<Spell> findByClearanceLevel(String clearanceLevel);


    @Query("SELECT s FROM Spell s WHERE :product MEMBER OF s.products")
    List<Spell> findByProduct(@Param("product") String product);
}
