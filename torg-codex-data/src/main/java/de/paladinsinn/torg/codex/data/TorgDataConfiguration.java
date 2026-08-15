package de.paladinsinn.torg.codex.data;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
/**
 * Spring configuration that activates all Torg Codex data-layer components:
 * JPA entities, repositories, markup services, and model classes.
 * Import via {@link EnableTorgData}.
 */
@Configuration
@ComponentScan({
    "de.paladinsinn.torg.codex.data"
})
@EntityScan({
    "de.paladinsinn.torg.codex.data.model",
    "de.kaiserpfalz.liquibase"
})
@EnableJpaRepositories({
    "de.paladinsinn.torg.codex.data.repository",
    "de.kaiserpfalz.liquibase"
})
public class TorgDataConfiguration {}
