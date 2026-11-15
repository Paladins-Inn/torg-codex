# TORG-CODEX Copilot Instructions

## Build and test

- Use JDK 25 and the Maven wrapper from the repository root.
- Build the complete reactor (including integration tests): `./mvnw clean verify`
- Run the unit-test phase: `./mvnw test`
- Run one data-module unit test: `./mvnw -pl torg-codex-data -Dtest=TorgMarkupServiceTest test`
- Run one application-module unit test (building its data-module dependency): `./mvnw -pl torg-codex -am -Dtest=ProductOwnershipResolverTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Run the Testcontainers-backed Liquibase integration test: `./mvnw -pl torg-codex-data -Dit.test=LiquibaseImportIT verify` (requires a running Docker daemon).
- Run the DRM behavior and contract tests: `./mvnw -pl torg-codex -am -Dtest='CatalogDrmDifferentialTest,CatalogOpenApiContractTest,CatalogDrmArchitectureTest*' -Dsurefire.failIfNoSpecifiedTests=false test` (requires Docker).
- There is no repository lint command. Maven compilation includes Lombok, MapStruct, JTE generation, and Hibernate enhancement.

## Architecture & Architectural Decision Records (ADRs)

- **ADR-001 (ADRs for Decisions):** Every architecture-level decision is documented via an Architecture Decision Record in `docs/modules/arc42/pages/09_architecture_decisions`. Implementing code must align with accepted ADRs.
- **ADR-002 (Spring Boot Ecosystem):** The core stack uses Java with Spring Boot, leveraging standard Spring ecosystem libraries.
- **ADR-003 (Asynchronous Messaging):** Asynchronous inter-system communication uses an AMQP broker (RabbitMQ). Inbound message receiving is decoupled from synchronous HTTP handling, with shared domain models and eventual consistency via the database.
- **ADR-004 (SQL Data Store):** Relational persistence uses PostgreSQL managed through Spring Data JPA / Hibernate.
- **ADR-005 (Hexagonal Architecture):** Follow Ports and Adapters. Domain logic resides at the core with inward-pointing dependencies, defining primary/driving and secondary/driven ports (interfaces), implemented by external adapters (controllers, repositories, message consumers).
- **ADR-006 (REST API with OpenAPI):** Expose APIs as classic REST endpoints with OpenAPI specifications as the single source of truth. Do not use GraphQL or full HATEOAS navigation.
- **ADR-007 (Access Management: Keycloak OIDC + UMA):** Identity and token management use Keycloak OIDC (JWT). Coarse-grained access uses RBAC (Spring Security authorities); fine-grained resource and DRM access uses ABAC / UMA 2.0 scope-based permissions per resource type.
- **ADR-008 (API Versioning Strategy):** APIs use HTTP media-type versioning: `Content-Type: application/vnd.1.3.6.1.4.1.33132.1.v<version>+<format>` using IANA PEN `33132` (Kaiserpfalz EDV-Service) and PEN subtype ID 1. URLs remain clean without `/v1/` prefixes.
- **ADR-009 (Database Field Encryption):** Sensitive data fields are encrypted at rest application-side using JPA `AttributeConverter`.
- **ADR-010 (Markdown Rendering):** Author content is stored as Markdown and rendered server-side using `flexmark-java`, sanitized via OWASP Java HTML Sanitizer, cached, and safely injected into JTE templates unescaped.
- **ADR-011 (Zero-Downtime Database Migrations):** Breaking schema changes follow Parallel Change / Expand-and-Contract (Phase 1 Expand [nullable/defaults], Phase 2 Migrate/backfill, Phase 3 Contract [drop in separate release]). Applied via Liquibase changesets named `<ISO-date>-<sequence>-<phase>` and executed by `db-updater` with `ddl-auto: validate`.
- **ADR-012 (WireMock HTTP Testing):** Testing of external HTTP services (e.g. DriveThruRPG API) must use WireMock with both annotation-based (`@AutoConfigureWireMock`, `@WireMockTest`) and file-based (`src/test/resources/mappings` and `src/test/resources/__files`) configurations.
- **ADR-013 (Lombok for Boilerplate Reduction):** Use Lombok across all layers: constructor annotations (`@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`) instead of hand-written constructors; `@Slf4j` instead of manual logger fields; `@EqualsAndHashCode`/`@ToString` for value semantics (JPA entities restrict `@EqualsAndHashCode` to the identity field only); `@Getter`/`@Setter` instead of hand-written accessors; `@Builder` for objects with several optional fields.
- **ADR-014 (MapStruct for Layer Conversion):** Use MapStruct for all Domain <-> JPA entity and Domain <-> DTO conversions. Mappers are Spring components (`componentModel = "spring"`), placed in the adapter package that uses them (never inside domain modules), and share common rules via `TorgMappingSupport`.
- **ADR-015 (Application Integration Boundaries):** `de.paladinsinn.drivethru` and `de.paladinsinn.security`, including all subpackages, belong to `torg-codex-application`. Preserve their fully qualified names and public APIs. `torg-codex-data` owns persistence, Liquibase, markup processing, and data adapters, not the DriveThruRPG client or API-key security implementation.
- **ADR-016 (Markup Domain Service):** The framework-free markup pipeline belongs to `torg-codex-domain`; its Spring bean wiring remains in `torg-codex-application`. This supersedes older markup-placement descriptions.
- **ADR-017 (Native Structured Logging):** Use Spring Boot native ECS structured JSON logging, preserving SLF4J key-value fields in emitted output. Do not add or retain Logstash-specific encoder dependencies or configuration.
- **ADR-019 (Domain-Level Product-Ownership DRM):** The access rule (`CatalogAccessPolicy`, `OwnedProducts`) lives in `torg-codex-domain`; the 14 product-bound catalog areas are read only through the restricted driving port `RestrictedCatalogQuery`. Lists contain granted entries only, unowned single entries return a `403` purchase teaser, and entries without products behave like missing ones (`404`). ADR-018 is reserved for multi-valued references.

## Cross-Cutting Concepts (arc42 08_concepts)

- **Self-Contained Systems (SCS):** Each service is an autonomous vertical slice (UI, logic, persistence). No shared databases or synchronous inter-SCS calls; inter-system communication is asynchronous event-driven, while UI is composed via Web Components or server-side includes.
- **EventBus & Spring Events:** In-process event handling uses Spring Events (`ApplicationEventPublisher`, `@EventListener`) for decoupling and event-driven internal workflows.
- **Asynchronous Data Handling:** Uses generic libraries (messaging, REST, UI), client libraries with SPIs for event consumption, and optional JPA-based store libraries (`dcis-*-store`).
- **Security & Access Control (RBAC & ABAC):** Roles include `Player`, `GM`, `Third Party Systems`, `Orga`, `Judge`, `Admin`. ABAC policies govern resource ownership and Torg Eternity DRM.
- **Data Protection & Privacy:** User identities map (Issuer, User) tuples to UUIDs. Account data is retained for 3 years post-closure; audit logs persist for account lifetime. API keys and credentials are strictly protected. User-created content transfers ownership to Torganized Play upon account deletion.
- **Observability:** Built on three pillars:
  - *Logging:* Spring Boot native structured ECS JSON logs (`logging.structured.format.console: ecs`), no Logstash encoder.
  - *Monitoring:* Spring Boot Actuator with Micrometer exporting to Prometheus (`/actuator/prometheus`).
  - *Alerting:* Prometheus alerting rules and Alertmanager routing.

## Project Structure & Conventions

- **Four-Module Reactor:**
  - `torg-codex-domain`: Framework-independent domain models and events.
  - `torg-codex-application`: Driving/driven ports and use-case services plus the `de.paladinsinn.drivethru` client and `de.paladinsinn.security` API-key integration.
  - `torg-codex-data`: JPA entities/repositories, Liquibase migrations, persistence/event adapters, and the Torg markup pipeline. Enabled via `@EnableTorgData`.
  - `torg-codex`: Spring Boot application exposing REST controllers/JTE pages, DTO mappers, and role-based censoring.
- **Security & Censoring:** Requests may authenticate with `Authorization: ApiKey <key>` or OIDC tokens. `ROLE_<codex-id>` authorities control product ownership and content visibility. `ProductOwnershipResolver` is the single ownership source; it implements `CallerProductsPort` (`current()` = owned products plus the free tier `OwnedProducts.FREE_TIER` = `core-rulebook`, for every caller). `CurrentUserCensorFactory` is the single censoring path and uses the same effective ownership; do not bypass censoring.
- **Domain-Level DRM (ADR-019):** Inbound adapters in any package read the 14 product-bound areas only through `RestrictedCatalogQuery<T extends ProductBoundEntry>` (`RestrictedCatalogQueryService`); the unrestricted `CatalogQuery` is limited to Article, Publication and Tag. Never read catalog repositories, entities, persistence adapters or persistence ports from inbound code; `CatalogDrmArchitectureTest` enforces this production-wide (A1-A6) including actual Spring bean bindings. Wire new restricted queries in `CatalogQueryConfiguration` through `CallerProductsPort`/`CatalogAccessObserver` interface types. Entries without products are reported through `CatalogAccessObserver` (WARN + counter `torg.codex.catalog.entries.without.products{area}`, no caller data).
- **REST Contract:** The catalog API source of truth is `specs/005-domain-drm-enforcement/contracts/catalog-api.openapi.yaml`; keep it in sync with DTOs and controllers (`CatalogOpenApiContractTest` validates the specification and live responses).
- **Markup Pipeline:** Rendering order must be strictly preserved: conditional product blocks -> entity references -> raw HTML -> game tokens -> CommonMark (`TorgMarkupService` / `Censor`).
- **Web & DTO Mapping:** Keep web adapters thin. Controllers return summary DTOs for lists and detail DTOs for item endpoints. MapStruct mappers must be Spring components (`componentModel = "spring"`) sharing rules via `TorgMappingSupport`.
- **Data Entities:** Value types/records where appropriate; JPA entities extend `TorgEntity` with generated UUID IDs and concrete product collections.
- **Database Migrations:** Liquibase changesets under `torg-codex-data/src/main/resources/db`. Never alter applied changesets. Follow the expand/migrate/contract phases for breaking changes.
- **Testing & Mocking:** JUnit 5, AssertJ, and Mockito for unit tests. Integration tests (`*IT`) use Testcontainers PostgreSQL under Failsafe (`./mvnw verify`). External HTTP integrations (e.g., DriveThruRPG API client) MUST use **WireMock** configured via both **annotation-based configuration** (`@AutoConfigureWireMock`, `@WireMockTest`) and **file-based configuration** (JSON stub mappings in `src/test/resources/mappings` and payload bodies in `src/test/resources/__files`).
- **Documentation & Contributions:** Keep architectural changes aligned with `docs/modules/arc42` (especially ADRs and concepts). Contributions require git sign-off under `CONTRIBUTING.md`.
