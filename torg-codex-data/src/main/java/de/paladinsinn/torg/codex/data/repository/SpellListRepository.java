package de.paladinsinn.torg.codex.data.repository;

import de.paladinsinn.torg.codex.data.model.SpellList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpellListRepository extends JpaRepository<SpellList, UUID> {

    Optional<SpellList> findByNameIgnoreCase(String name);

    List<SpellList> findByCosm(String cosm);

    List<SpellList> findByUnlockingPerk(String perkId);

    @Query("SELECT sl FROM SpellList sl WHERE :product MEMBER OF sl.products")
    List<SpellList> findByProduct(@Param("product") String product);

    /** Lists where the given product does NOT disable them. */
    List<SpellList> findByDisableIfIsNullOrDisableIfNot(String disableIfProduct);
}
