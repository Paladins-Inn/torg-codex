# TORG-CODEX Copilot Instructions

## Build and test

- Use JDK 25 and the Maven wrapper from the repository root.
- Build the complete reactor (including integration tests): `./mvnw clean verify`
- Run the unit-test phase: `./mvnw test`
- Run one data-module unit test: `./mvnw -pl torg-codex-data -Dtest=TorgMarkupServiceTest test`
- Run one application-module unit test (building its data-module dependency): `./mvnw -pl torg-codex -am -Dtest=ProductOwnershipResolverTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Run the Testcontainers-backed Liquibase integration test: `./mvnw -pl torg-codex-data -Dit.test=LiquibaseImportIT verify` (requires a running Docker daemon).
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
- **ADR-010 (Database Field Encryption):** Sensitive data fields are encrypted at rest application-side using JPA `AttributeConverter`.
- **ADR-011 (Markdown Rendering):** Author content is stored as Markdown and rendered server-side using `flexmark-java`, sanitized via OWASP Java HTML Sanitizer, cached, and safely injected into JTE templates unescaped.
- **ADR-012 (Zero-Downtime Database Migrations):** Breaking schema changes follow Parallel Change / Expand-and-Contract (Phase 1 Expand [nullable/defaults], Phase 2 Migrate/backfill, Phase 3 Contract [drop in separate release]). Applied via Liquibase changesets named `<ISO-date>-<sequence>-<phase>` and executed by `db-updater` with `ddl-auto: validate`.
- **ADR-013 (WireMock HTTP Testing):** Testing of external HTTP services (e.g. DriveThruRPG API) must use WireMock with both annotation-based (`@AutoConfigureWireMock`, `@WireMockTest`) and file-based (`src/test/resources/mappings` and `src/test/resources/__files`) configurations.
- **ADR-014 (Lombok for Boilerplate Reduction):** Use Lombok across all layers: constructor annotations (`@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`) instead of hand-written constructors; `@Slf4j` instead of manual logger fields; `@EqualsAndHashCode`/`@ToString` for value semantics (JPA entities restrict `@EqualsAndHashCode` to the identity field only); `@Getter`/`@Setter` instead of hand-written accessors; `@Builder` for objects with several optional fields.
- **ADR-015 (MapStruct for Layer Conversion):** Use MapStruct for all Domain <-> JPA entity and Domain <-> DTO conversions. Mappers are Spring components (`componentModel = "spring"`), placed in the adapter package that uses them (never inside domain modules), and share common rules via `TorgMappingSupport`.
- **ADR-016 (Application Integration Boundaries):** `de.paladinsinn.drivethru` and `de.paladinsinn.security`, including all subpackages, belong to `torg-codex-application`. Preserve their fully qualified names and public APIs. `torg-codex-data` owns persistence, Liquibase, markup processing, and data adapters, not the DriveThruRPG client or API-key security implementation.

## Cross-Cutting Concepts (arc42 08_concepts)

- **Self-Contained Systems (SCS):** Each service is an autonomous vertical slice (UI, logic, persistence). No shared databases or synchronous inter-SCS calls; inter-system communication is asynchronous event-driven, while UI is composed via Web Components or server-side includes.
- **EventBus & Spring Events:** In-process event handling uses Spring Events (`ApplicationEventPublisher`, `@EventListener`) for decoupling and event-driven internal workflows.
- **Asynchronous Data Handling:** Uses generic libraries (messaging, REST, UI), client libraries with SPIs for event consumption, and optional JPA-based store libraries (`dcis-*-store`).
- **Security & Access Control (RBAC & ABAC):** Roles include `Player`, `GM`, `Third Party Systems`, `Orga`, `Judge`, `Admin`. ABAC policies govern resource ownership and Torg Eternity DRM.
- **Data Protection & Privacy:** User identities map (Issuer, User) tuples to UUIDs. Account data is retained for 3 years post-closure; audit logs persist for account lifetime. API keys and credentials are strictly protected. User-created content transfers ownership to Torganized Play upon account deletion.
- **Observability:** Built on three pillars:
  - *Logging:* JSON-structured logs via Logback / LogstashEncoder.
  - *Monitoring:* Spring Boot Actuator with Micrometer exporting to Prometheus (`/actuator/prometheus`).
  - *Alerting:* Prometheus alerting rules and Alertmanager routing.

## Project Structure & Conventions

- **Four-Module Reactor:**
  - `torg-codex-domain`: Framework-independent domain models and events.
  - `torg-codex-application`: Driving/driven ports and use-case services plus the `de.paladinsinn.drivethru` client and `de.paladinsinn.security` API-key integration.
  - `torg-codex-data`: JPA entities/repositories, Liquibase migrations, persistence/event adapters, and the Torg markup pipeline. Enabled via `@EnableTorgData`.
  - `torg-codex`: Spring Boot application exposing REST controllers/JTE pages, DTO mappers, and role-based censoring.
- **Security & Censoring:** Requests may authenticate with `Authorization: ApiKey <key>` or OIDC tokens. `ROLE_<codex-id>` authorities control product ownership and content visibility. `ProductOwnershipResolver` and `CurrentUserCensorFactory` are the single censoring path; do not bypass censoring.
- **Markup Pipeline:** Rendering order must be strictly preserved: conditional product blocks -> entity references -> raw HTML -> game tokens -> CommonMark (`TorgMarkupService` / `Censor`).
- **Web & DTO Mapping:** Keep web adapters thin. Controllers return summary DTOs for lists and detail DTOs for item endpoints. MapStruct mappers must be Spring components (`componentModel = "spring"`) sharing rules via `TorgMappingSupport`.
- **Data Entities:** Value types/records where appropriate; JPA entities extend `TorgEntity` with generated UUID IDs and concrete product collections.
- **Database Migrations:** Liquibase changesets under `torg-codex-data/src/main/resources/db`. Never alter applied changesets. Follow the expand/migrate/contract phases for breaking changes.
- **Testing & Mocking:** JUnit 5, AssertJ, and Mockito for unit tests. Integration tests (`*IT`) use Testcontainers PostgreSQL under Failsafe (`./mvnw verify`). External HTTP integrations (e.g., DriveThruRPG API client) MUST use **WireMock** configured via both **annotation-based configuration** (`@AutoConfigureWireMock`, `@WireMockTest`) and **file-based configuration** (JSON stub mappings in `src/test/resources/mappings` and payload bodies in `src/test/resources/__files`).
- **Documentation & Contributions:** Keep architectural changes aligned with `docs/modules/arc42` (especially ADRs and concepts). Contributions require git sign-off under `CONTRIBUTING.md`.
