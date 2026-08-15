package de.paladinsinn.torg.codex.data.equivalence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Persistence-equivalence test harness (G2 prerequisite for T081).
 *
 * <p>For a given catalog area it compares two round-trips against the <em>same</em>
 * Testcontainers-backed database:
 * <ol>
 *   <li>the pre-migration raw-JPA-entity path (the entity as read straight from the
 *       repository, i.e. exactly as persisted), and</li>
 *   <li>the post-migration path (entity &rarr; domain model via the MapStruct mapper
 *       &rarr; entity via the inverse mapper).</li>
 * </ol>
 * It then asserts field-by-field equivalence between the two, failing with a recursive
 * diff on any mismatch. The transient {@code censor} field is ignored (it is never part of
 * the persisted state) and collection order is ignored (mapping copies collections verbatim,
 * but Hibernate collection wrappers must not cause spurious ordering differences).
 */
public final class PersistenceEquivalenceHarness {

    /**
     * Verifies mapper round-trip persistence equivalence for up to {@code maxRows} rows of a
     * catalog area. Areas without any loaded fixture rows are skipped via a JUnit assumption
     * so the harness never fails on legitimately empty (e.g. proprietary-only) areas.
     *
     * @param area       human-readable area name for assertion messages
     * @param repository the JPA repository providing the raw-persisted entities
     * @param toDomain   entity &rarr; domain-model mapper function
     * @param toEntity   domain-model &rarr; entity mapper function
     * @param maxRows    maximum number of rows to sample per area
     * @param <E>        the JPA entity type
     * @param <D>        the domain-model type
     */
    public <E, D> void assertRoundTripEquivalent(
            final String area,
            final JpaRepository<E, UUID> repository,
            final Function<E, D> toDomain,
            final Function<D, E> toEntity,
            final int maxRows) {

        final List<E> rows = repository.findAll();
        assumeThat(rows)
                .as("%s: no fixture rows loaded, skipping equivalence check", area)
                .isNotEmpty();

        rows.stream().limit(maxRows).forEach(original -> {
            final D domain = toDomain.apply(original);
            final E roundTripped = toEntity.apply(domain);

            assertThat(roundTripped)
                    .as("%s: entity round-tripped through the domain model + MapStruct mappers "
                            + "must equal the raw-persisted entity", area)
                    .usingRecursiveComparison()
                    .ignoringFields("censor")
                    .ignoringCollectionOrder()
                    .isEqualTo(original);
        });
    }
}
