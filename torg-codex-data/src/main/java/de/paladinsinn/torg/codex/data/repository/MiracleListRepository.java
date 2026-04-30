package de.paladinsinn.torg.codex.data.repository;

import de.paladinsinn.torg.codex.data.model.MiracleList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MiracleListRepository extends JpaRepository<MiracleList, UUID> {

    Optional<MiracleList> findByNameIgnoreCase(String name);

    List<MiracleList> findByCosm(String cosm);

    List<MiracleList> findByUnlockingPerk(String perkId);

    @Query("SELECT ml FROM MiracleList ml WHERE :product MEMBER OF ml.products")
    List<MiracleList> findByProduct(@Param("product") String product);

    List<MiracleList> findByDisableIfIsNullOrDisableIfNot(String disableIfProduct);
}
