/*
 * Copyright (c) 2026.  Roland T. Lichti <rlichti@kaiserpfalz-edv.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * ERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * You may contact me via email rlichti@kaiserpfalz-edv.de or via mail
 *
 * Kaiserpfalz EDV-Service
 * Roland T. Lichti
 * Darmstädter Str. 12
 * 64625 Bensheim
 * GERMANY
 */

package de.paladinsinn.torg.codex.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import de.paladinsinn.torg.codex.application.port.in.CatalogQuery;
import de.paladinsinn.torg.codex.application.port.in.RestrictedCatalogQuery;
import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.application.port.out.CatalogReferencePersistencePort;
import de.paladinsinn.torg.codex.application.service.CatalogQueryService;
import de.paladinsinn.torg.codex.application.service.CatalogReferenceQueryService;
import de.paladinsinn.torg.codex.application.service.ReferencedEntryResolver;
import de.paladinsinn.torg.codex.application.service.RestrictedCatalogQueryService;
import de.paladinsinn.torg.codex.api.configuration.CatalogQueryConfiguration;
import de.paladinsinn.torg.codex.domain.markup.EntityType;
import de.paladinsinn.torg.codex.domain.model.Article;
import de.paladinsinn.torg.codex.domain.model.ProductBoundEntry;
import de.paladinsinn.torg.codex.domain.model.Publication;
import de.paladinsinn.torg.codex.domain.model.Tag;
import de.paladinsinn.torg.codex.drmfixtures.valid.PublicQueryConfiguration;
import de.paladinsinn.torg.codex.drmfixtures.valid.PublicReferenceReader;
import de.paladinsinn.torg.codex.drmfixtures.valid.RestrictedSpellAdapter;
import de.paladinsinn.torg.codex.drmfixtures.violating.HelperBackedSpellExport;
import de.paladinsinn.torg.codex.drmfixtures.violating.NewCatalogRestController;
import de.paladinsinn.torg.codex.drmfixtures.violating.RawCatalogQueryAdapter;
import de.paladinsinn.torg.codex.drmfixtures.violating.UnrestrictedSpellQueryConfiguration;
import de.paladinsinn.torg.codex.drmfixtures.violating.messaging.SpellImportConsumer;
import de.paladinsinn.torg.codex.drmfixtures.violating.page.SpellCatalogPage;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.ResolvableType;
import org.springframework.data.repository.Repository;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Production-wide architecture guards of the domain-level product-ownership DRM (feature 005,
 * ADR-019, invariants A1-A6 of {@code contracts/catalog-access-ports.md}).
 *
 * <p>The rules apply to every production class of all four modules, not just to the current
 * controllers. Each rule is also exercised against deliberately violating fixtures (a new REST
 * controller, a page/export and a message consumer outside {@code api..}, a helper-mediated
 * bypass, a raw query and unrestricted product-bound query beans) and valid fixtures in
 * {@code de.paladinsinn.torg.codex.drmfixtures}, which are never part of the production import.
 * The existing single-mechanism rules (A4) live unchanged in
 * {@link CensoringSingleMechanismArchitectureTest}.
 */
class CatalogDrmArchitectureTest {

    /** Production classes plus the DRM fixtures (and no other test class). */
    private static final JavaClasses WITH_FIXTURES = new ClassFileImporter()
            .withImportOption(location -> ImportOption.Predefined.DO_NOT_INCLUDE_TESTS.includes(location)
                    || location.contains("/de/paladinsinn/torg/codex/drmfixtures/"))
            .importPackages("de.paladinsinn.torg.codex", "de.paladinsinn.drivethru", "de.paladinsinn.security",
                    "de.kaiserpfalz.liquibase");

    private static final Set<Class<?>> PUBLIC_REFERENCE_DATA = Set.of(Article.class, Publication.class, Tag.class);

    /** Exact approved consumers of catalog persistence outside the persistence module. */
    private static final Set<String> APPROVED_PERSISTENCE_CONSUMERS = Set.of(
            CatalogQueryService.class.getName(),
            RestrictedCatalogQueryService.class.getName(),
            ReferencedEntryResolver.class.getName(),
            CatalogReferenceQueryService.class.getName(),
            CatalogPersistencePort.class.getName(),
            CatalogReferencePersistencePort.class.getName(),
            CatalogQueryConfiguration.class.getName());

    private static final DescribedPredicate<JavaClass> CATALOG_PERSISTENCE = DescribedPredicate.describe(
            "catalog persistence (JPA entities, repositories, persistence adapters, entity mappers or persistence ports)",
            CatalogDrmArchitectureTest::isCatalogPersistence);

    private static final DescribedPredicate<JavaClass> PRODUCTION_OUTSIDE_PERSISTENCE_EXCEPTIONS = DescribedPredicate.describe(
            "production classes other than persistence internals and the approved consumers",
            clazz -> !isFixture(clazz) && !isPersistenceInternal(clazz) && !isApprovedConsumer(clazz));

    // ----------------------------------------------------------------------------------------
    // A1: persistence boundary for every inbound adapter, direct or indirect
    // ----------------------------------------------------------------------------------------

    @Test
    void a1_noProductionClassReachesCatalogPersistenceOutsideTheApprovedConsumers() {
        a1(PRODUCTION_OUTSIDE_PERSISTENCE_EXCEPTIONS).check(ArchitectureTestSupport.IMPORTED_CLASSES);
    }

    @ParameterizedTest
    @ValueSource(classes = {NewCatalogRestController.class, SpellCatalogPage.class, SpellImportConsumer.class,
        HelperBackedSpellExport.class, UnrestrictedSpellQueryConfiguration.class})
    void a1_rejectsDirectAndHelperMediatedPersistenceReadsOfNewAdapters(Class<?> fixture) {
        assertThat(a1(exactly(fixture)).evaluate(WITH_FIXTURES).hasViolation())
                .as("A1 must reject %s", fixture.getSimpleName())
                .isTrue();
    }

    @ParameterizedTest
    @ValueSource(classes = {RestrictedSpellAdapter.class, PublicReferenceReader.class, PublicQueryConfiguration.class})
    void a1_acceptsAdaptersUsingTheDrivingPorts(Class<?> fixture) {
        assertThat(a1(exactly(fixture)).evaluate(WITH_FIXTURES).hasViolation()).isFalse();
    }

    private static ArchRule a1(DescribedPredicate<JavaClass> scope) {
        return noClasses().that(scope)
                .should().transitivelyDependOnClassesThat(CATALOG_PERSISTENCE)
                .as("A1: only persistence internals and the approved catalog services/composition root may use catalog "
                        + "persistence; inbound adapters in any package must use the driving ports")
                .allowEmptyShould(true);
    }

    // ----------------------------------------------------------------------------------------
    // A2: product-bound reads only through the restricted driving port
    // ----------------------------------------------------------------------------------------

    @Test
    void a2_productBoundAreasAreNeverReadThroughUnrestrictedQueries() {
        a2(DescribedPredicate.describe("production classes", clazz -> !isFixture(clazz)))
                .check(ArchitectureTestSupport.IMPORTED_CLASSES);
    }

    @ParameterizedTest
    @ValueSource(classes = {UnrestrictedSpellQueryConfiguration.class, RawCatalogQueryAdapter.class})
    void a2_rejectsUnrestrictedRawAndConstructedQueries(Class<?> fixture) {
        assertThat(a2(exactly(fixture)).evaluate(WITH_FIXTURES).hasViolation())
                .as("A2 must reject %s", fixture.getSimpleName())
                .isTrue();
    }

    @ParameterizedTest
    @ValueSource(classes = {RestrictedSpellAdapter.class, PublicReferenceReader.class, PublicQueryConfiguration.class})
    void a2_acceptsRestrictedAndPublicReferenceDataQueries(Class<?> fixture) {
        assertThat(a2(exactly(fixture)).evaluate(WITH_FIXTURES).hasViolation()).isFalse();
    }

    private static ArchRule a2(DescribedPredicate<JavaClass> scope) {
        return classes().that(scope)
                .and().doNotBelongToAnyOf(CatalogQuery.class, CatalogQueryService.class)
                .should(useOnlyRestrictedProductBoundQueries())
                .as("A2: product-bound entries are read only through RestrictedCatalogQuery; CatalogQuery is limited to "
                        + "Article, Publication and Tag; raw or unresolved query types and CatalogQueryService outside "
                        + "the composition root are forbidden")
                .allowEmptyShould(true);
    }

    private static ArchCondition<JavaClass> useOnlyRestrictedProductBoundQueries() {
        return new ArchCondition<>("use only restricted product-bound queries") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                List<String> problems = new ArrayList<>();
                for (JavaField field : clazz.getFields()) {
                    checkType(field.getType(), "field " + field.getName(), problems);
                }
                for (JavaCodeUnit codeUnit : clazz.getCodeUnits()) {
                    codeUnit.getParameterTypes().forEach(type -> checkType(type, "parameter of " + codeUnit.getFullName(), problems));
                    checkType(codeUnit.getReturnType(), "return type of " + codeUnit.getFullName(), problems);
                }
                boolean composition = clazz.getName().equals(CatalogQueryConfiguration.class.getName());
                clazz.getConstructorCallsFromSelf().stream()
                        .filter(call -> call.getTargetOwner().isEquivalentTo(CatalogQueryService.class))
                        .filter(call -> !composition)
                        .forEach(call -> problems.add("constructs CatalogQueryService outside the composition root: "
                                + call.getDescription()));
                problems.forEach(problem -> events.add(SimpleConditionEvent.violated(clazz, clazz.getName() + ": " + problem)));
            }
        };
    }

    private static void checkType(JavaType type, String location, List<String> problems) {
        JavaClass erasure = type.toErasure();
        boolean query = erasure.isEquivalentTo(CatalogQuery.class) || erasure.isEquivalentTo(CatalogQueryService.class);
        if (query) {
            if (!(type instanceof JavaParameterizedType parameterized)) {
                problems.add(location + " uses a raw " + erasure.getSimpleName());
            } else {
                JavaType argument = parameterized.getActualTypeArguments().getFirst();
                if (!(argument instanceof JavaClass entry)) {
                    problems.add(location + " uses an unresolved " + erasure.getSimpleName() + "<" + argument.getName() + ">");
                } else if (PUBLIC_REFERENCE_DATA.stream().noneMatch(entry::isEquivalentTo)) {
                    problems.add(location + " uses an unrestricted " + erasure.getSimpleName() + "<" + entry.getSimpleName() + ">");
                }
            }
        }
        if (type instanceof JavaParameterizedType parameterized) {
            parameterized.getActualTypeArguments().forEach(argument -> checkType(argument, location, problems));
        }
    }

    // ----------------------------------------------------------------------------------------
    // A3: every catalog-area record is product-bound
    // ----------------------------------------------------------------------------------------

    @Test
    void a3_everyCatalogAreaRecordIsProductBound() {
        for (EntityType area : EntityType.values()) {
            String name = "de.paladinsinn.torg.codex.domain.model." + recordName(area);

            assertThat(ArchitectureTestSupport.IMPORTED_CLASSES.contain(name)).as("record of %s exists", area).isTrue();
            assertThat(ArchitectureTestSupport.IMPORTED_CLASSES.get(name).isAssignableTo(ProductBoundEntry.class))
                    .as("%s implements ProductBoundEntry", name)
                    .isTrue();
        }
    }

    private static String recordName(EntityType area) {
        if (area == EntityType.GROUP) {
            return "PerkGroup";
        }
        StringBuilder name = new StringBuilder();
        for (String part : area.name().split("_")) {
            name.append(part.charAt(0)).append(part.substring(1).toLowerCase(java.util.Locale.ROOT));
        }
        return name.toString();
    }

    // ----------------------------------------------------------------------------------------
    // A5: the core stays free of Micrometer
    // ----------------------------------------------------------------------------------------

    @Test
    void a5_domainAndApplicationDoNotDependOnMicrometer() {
        noClasses().that().resideInAnyPackage("de.paladinsinn.torg.codex.application..", "de.paladinsinn.torg.codex.domain..")
                .should().dependOnClassesThat().resideInAnyPackage("io.micrometer..")
                .as("A5: torg-codex-application and torg-codex-domain must not depend on Micrometer")
                .check(ArchitectureTestSupport.IMPORTED_CLASSES);
    }

    // ----------------------------------------------------------------------------------------
    // A6: actual query bean bindings
    // ----------------------------------------------------------------------------------------

    @Test
    void a6_rejectsExposedAndDisguisedUnrestrictedProductBoundBeans() {
        try (var context = new AnnotationConfigApplicationContext(UnrestrictedSpellQueryConfiguration.class)) {
            List<String> violations = bindingViolations(context.getBeanFactory(), false);

            assertThat(violations).anyMatch(violation -> violation.contains("unrestrictedSpells"));
            assertThat(violations).anyMatch(violation -> violation.contains("disguisedSpells"));
        }
    }

    @Test
    void a6_acceptsThePublicReferenceDataBindings() {
        try (var context = new AnnotationConfigApplicationContext(PublicQueryConfiguration.class)) {
            assertThat(bindingViolations(context.getBeanFactory(), false)).isEmpty();
        }
    }

    /** A6 against the real application context. */
    @Nested
    @Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
    @SpringBootTest
    class ApplicationContextBindings {

        @Autowired
        private ConfigurableListableBeanFactory beanFactory;

        @Test
        void a6_exactlyTheProductBoundAreasUseRestrictedQueriesAndOnlyPublicDataIsUnrestricted() {
            assertThat(bindingViolations(beanFactory, true)).isEmpty();
        }
    }

    /**
     * Verifies the resolved generic bindings of all catalog query beans and the persistence ports
     * actually wrapped by their implementations.
     */
    static List<String> bindingViolations(ConfigurableListableBeanFactory factory, boolean requireAllProductBoundAreas) {
        List<String> violations = new ArrayList<>();
        for (String name : factory.getBeanNamesForType(CatalogQuery.class)) {
            Class<?> entry = factory.getMergedBeanDefinition(name).getResolvableType().as(CatalogQuery.class).resolveGeneric(0);
            if (entry == null || !PUBLIC_REFERENCE_DATA.contains(entry)) {
                violations.add(name + ": unrestricted CatalogQuery<" + entry + "> is limited to Article, Publication and Tag");
            }
            Object bean = factory.getBean(name);
            if (bean instanceof CatalogQueryService<?>) {
                Class<?> wrapped = persistedType(bean);
                if (wrapped == null || !PUBLIC_REFERENCE_DATA.contains(wrapped)) {
                    violations.add(name + ": CatalogQueryService wraps the persistence of " + wrapped);
                }
            }
        }
        Set<Class<?>> restrictedAreas = new HashSet<>();
        for (String name : factory.getBeanNamesForType(RestrictedCatalogQuery.class)) {
            Class<?> entry = factory.getMergedBeanDefinition(name).getResolvableType()
                    .as(RestrictedCatalogQuery.class).resolveGeneric(0);
            Object bean = factory.getBean(name);
            if (entry == null || !ProductBoundEntry.class.isAssignableFrom(entry)) {
                violations.add(name + ": unresolved RestrictedCatalogQuery<" + entry + ">");
            } else if (!(bean instanceof RestrictedCatalogQueryService<?>)) {
                violations.add(name + ": " + bean.getClass().getName() + " is not the approved RestrictedCatalogQueryService");
            } else if (!entry.equals(persistedType(bean))) {
                violations.add(name + ": restricted " + entry.getSimpleName() + " query wraps the persistence of "
                        + persistedType(bean));
            } else {
                restrictedAreas.add(entry);
            }
        }
        if (requireAllProductBoundAreas) {
            Set<Class<?>> expected = productBoundRecords();
            if (!restrictedAreas.equals(expected)) {
                violations.add("restricted query beans " + restrictedAreas + " differ from the product-bound areas " + expected);
            }
        }
        return violations;
    }

    private static Class<?> persistedType(Object queryService) {
        try {
            Field field = queryService.getClass().getDeclaredField("persistence");
            field.setAccessible(true);
            Object persistence = field.get(queryService);
            return ResolvableType.forClass(persistence.getClass()).as(CatalogPersistencePort.class).resolveGeneric(0);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Set<Class<?>> productBoundRecords() {
        Set<Class<?>> records = new HashSet<>();
        for (EntityType area : EntityType.values()) {
            try {
                records.add(Class.forName("de.paladinsinn.torg.codex.domain.model." + recordName(area)));
            } catch (ClassNotFoundException exception) {
                throw new IllegalStateException(exception);
            }
        }
        return records;
    }

    // ----------------------------------------------------------------------------------------
    // predicates
    // ----------------------------------------------------------------------------------------

    private static boolean isCatalogPersistence(JavaClass clazz) {
        if (clazz.isEquivalentTo(CatalogPersistencePort.class) || clazz.isEquivalentTo(CatalogReferencePersistencePort.class)) {
            return true;
        }
        String packageName = clazz.getPackageName();
        if (!packageName.startsWith("de.paladinsinn.torg.codex.data")) {
            return false;
        }
        return clazz.isAnnotatedWith(Entity.class)
                || clazz.isAnnotatedWith(MappedSuperclass.class)
                || clazz.isAssignableTo(Repository.class)
                || packageName.startsWith("de.paladinsinn.torg.codex.data.adapter.out.persistence")
                || packageName.startsWith("de.paladinsinn.torg.codex.data.mapper");
    }

    private static boolean isPersistenceInternal(JavaClass clazz) {
        return clazz.getPackageName().startsWith("de.paladinsinn.torg.codex.data")
                || clazz.getPackageName().startsWith("de.kaiserpfalz.liquibase");
    }

    private static boolean isApprovedConsumer(JavaClass clazz) {
        return Stream.iterate(clazz, current -> current.getEnclosingClass().orElse(null))
                .takeWhile(current -> current != null)
                .anyMatch(current -> APPROVED_PERSISTENCE_CONSUMERS.contains(current.getName()));
    }

    private static boolean isFixture(JavaClass clazz) {
        return clazz.getPackageName().startsWith("de.paladinsinn.torg.codex.drmfixtures");
    }

    private static DescribedPredicate<JavaClass> exactly(Class<?> type) {
        return DescribedPredicate.describe(type.getSimpleName(), clazz -> clazz.isEquivalentTo(type)
                || clazz.getEnclosingClass().map(enclosing -> enclosing.isEquivalentTo(type)).orElse(false));
    }
}
