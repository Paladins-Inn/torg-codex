package de.paladinsinn.torg.codex.data;
import org.springframework.context.annotation.Import;
import java.lang.annotation.*;
/**
 * Enables the Torg Eternity Codex data layer: JPA entities, Spring Data
 * repositories, markup services, and all supporting components.
 *
 * <p>Add this annotation to a {@code @Configuration} or {@code @SpringBootApplication}
 * class to activate the full {@code de.paladinsinn.torg.codex.data} stack.</p>
 *
 * <pre>{@code
 * @SpringBootApplication
 * @EnableTorgData
 * public class MyApplication { ... }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(TorgDataConfiguration.class)
public @interface EnableTorgData {}
