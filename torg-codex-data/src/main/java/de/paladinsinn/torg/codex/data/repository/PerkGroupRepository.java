package de.paladinsinn.torg.codex.data.repository;

import de.paladinsinn.torg.codex.data.model.PerkGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PerkGroupRepository extends JpaRepository<PerkGroup, UUID> {

    Optional<PerkGroup> findByNameIgnoreCase(String name);
}
