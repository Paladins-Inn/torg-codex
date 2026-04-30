package de.paladinsinn.torg.codex.data.repository;

import de.paladinsinn.torg.codex.data.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    Optional<Tag> findByNameIgnoreCase(String name);

    /** All direct children of the given parent tag id. */
    List<Tag> findByParent(UUID parentId);

    /** All root-level tags (no parent). */
    List<Tag> findByParentIsNull();
}
