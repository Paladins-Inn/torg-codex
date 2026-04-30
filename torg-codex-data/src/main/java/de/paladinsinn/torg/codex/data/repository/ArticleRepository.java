package de.paladinsinn.torg.codex.data.repository;

import de.paladinsinn.torg.codex.data.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

    Optional<Article> findByNameIgnoreCase(String name);
}
