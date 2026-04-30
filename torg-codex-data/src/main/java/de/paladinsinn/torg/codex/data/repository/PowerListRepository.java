package de.paladinsinn.torg.codex.data.repository;

import de.paladinsinn.torg.codex.data.model.PowerList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PowerListRepository extends JpaRepository<PowerList, UUID> {

    Optional<PowerList> findByNameIgnoreCase(String name);

    List<PowerList> findByCosm(String cosm);

    List<PowerList> findByUnlockingPerk(String perkId);

    @Query("SELECT pl FROM PowerList pl WHERE :product MEMBER OF pl.products")
    List<PowerList> findByProduct(@Param("product") String product);

    List<PowerList> findByDisableIfIsNullOrDisableIfNot(String disableIfProduct);
}
