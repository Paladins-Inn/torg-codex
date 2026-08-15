# TORG-CODEX Constitution

## Core Principles

### I. Hexagonal Architecture & Clean Ports
Business and domain logic MUST remain at the center of the system, decoupled from framework, database, and transport concerns (ADR-005, concept: hexagonal-architecture).
- Domain modules MUST NOT depend on the Spring Framework or JPA.
- Domain objects SHOULD use Lombok (including Lombok constructors like `@AllArgsConstructor`, `@Builder`, etc.) to minimize boilerplate.
- Bean validation MUST be used outside of constructors (e.g., in domain services, use cases, or dedicated factory methods) rather than directly in the domain constructors.
- Separate models MUST be used for persistence (JPA entities) and the Domain core.
- Application services expose use cases through ports (interfaces).
- Primary/driving ports define incoming capabilities; secondary/driven ports define outgoing operations.
- Adapters translate between external protocols and domain ports. REST controllers are inbound adapters; Persistence implementations are outbound adapters.
- Dependencies MUST always point inward toward the domain/application core.
- Module boundaries MUST be enforceable through automated architecture tests.
- Mapping between layers (Domain <-> DTOs, Domain <-> JPA Entities) MUST be done using MapStruct.

### II. Self-Contained Systems & Asynchronous Integration
Services operate as autonomous vertical slices (UI, backend logic, persistence) with no shared databases or synchronous inter-service runtime couplings (ADR-003, concepts: scs, asynchronus-data-handling).
- Inter-system communication MUST be asynchronous and event-driven via AMQP (RabbitMQ).
- In-process eventing MUST use Spring Events (`ApplicationEventPublisher`, `@EventListener`) for decoupled module workflows (concept: event-bus).
- UI integration is achieved via server-side composition (JTE/Thymeleaf) or Web Components.

### III. Standardized REST APIs & Strict OpenAPI Contracts
All synchronous APIs exposed by the application MUST be implemented as classic REST endpoints with OpenAPI specifications as the single source of truth (ADR-006).
- GraphQL and full HATEOAS navigation are prohibited on public endpoints.
- Web controllers MUST remain thin adapters: delegate to Spring Data repositories and MapStruct mappers (`componentModel = "spring"`), exposing summary DTOs for lists and detail DTOs for item endpoints.
- API versioning MUST follow media-type content negotiation in HTTP headers using IANA PEN `33132` (`application/vnd.1.3.6.1.4.1.33132.1.v<version>+<format>`); URLs remain clean (ADR-008).

### IV. Zero-Downtime Evolutionary Database Migrations
Database schema changes MUST follow the Parallel Change / Expand-and-Contract pattern to ensure non-disruptive rolling updates and canary deployments (ADR-004, ADR-012, concept: db-migrations).
- Breaking schema changes MUST be split across separate deployments: Phase 1 (Expand with nullable/default fields) -> Phase 2 (Migrate/backfill) -> Phase 3 (Contract/drop old structures).
- Migrations MUST be defined as Liquibase changesets under `torg-codex-data/src/main/resources/db` with naming format `<ISO-date>-<sequence>-<phase>` (e.g., `2024-06-01-001-expand`).
- Applied Liquibase changesets MUST NEVER be altered.
- Application runtime runs with `ddl-auto: validate`; schema modifications are executed exclusively by `db-updater` prior to application rollouts.

### V. Multi-Tier Security, DRM Censorship & Data Protection
Security combines centralized identity, role-based access, fine-grained attribute control, and DRM content censorship (ADR-007, ADR-010, concepts: rbac, abac, data-protection).
- Identity management uses Keycloak OIDC (JWT); coarse gating is handled via Spring Security RBAC roles (`Player`, `GM`, `Third Party Systems`, `Orga`, `Judge`, `Admin`).
- Fine-grained object permissions and DRM use ABAC / UMA 2.0 scope-based permissions per resource type.
- Content rendering is censored based on product ownership authorities (`ROLE_<codex-id>`) injected via `CensorInjectionAspect`. Repository queries and entities MUST NOT bypass censoring.
- Sensitive fields at rest MUST be encrypted application-side using JPA `AttributeConverter` (ADR-010).
- Data privacy rules MUST be strictly enforced: external IdP identity tuples map to UUIDs, 3-year account retention post-closure, and audit trails preserved for account lifetimes.

### VI. Deterministic Markdown Pipeline & Template Safety
Content is authored and stored as Markdown and rendered server-side with strict sanitization and order (ADR-011, concept: jte-with-markdown).
- Parsing and rendering MUST use `flexmark-java` followed by OWASP Java HTML Sanitizer before caching.
- Templates (JTE) receive pre-sanitized safe HTML attributes. Raw unsanitized HTML input from users MUST NEVER be rendered unescaped.
- Markup rendering order MUST be maintained: conditional product blocks -> entity references -> raw HTML -> game tokens -> CommonMark.

### VII. Production Observability & Quality Standards
Production systems MUST expose standard observability hooks across three pillars: Logging, Monitoring, and Alerting (concept: observability).
- Logging: Structured JSON logs using LogstashEncoder for automated ingestion.
- Monitoring: Spring Boot Actuator with Micrometer publishing metrics to `/actuator/prometheus`.
- Alerting: Metric-based thresholds via Prometheus alerting rules and Alertmanager.

### VIII. Test-First & Integration Verification (NON-NEGOTIABLE)
All new features, schema updates, external integrations, and bug fixes MUST include automated test coverage (JUnit 5, AssertJ, Mockito).
- Unit tests run with `./mvnw test`.
- Integration tests (`*IT`) MUST run against real PostgreSQL instances via Testcontainers under Failsafe (`./mvnw clean verify`).
- External HTTP service testing (such as DriveThruRPG client and remote APIs) MUST use **WireMock**, supporting both **annotation-based configuration** (`@AutoConfigureWireMock`, `@WireMockTest`) and **file-based configuration** (declarative stub mappings in `src/test/resources/mappings` and body fixtures in `src/test/resources/__files`) (ADR-013, concept: wiremock).
- Git sign-off is required for all contributions in accordance with `CONTRIBUTING.md`.

## Additional Constraints & Architecture Decisions

- **Architectural Decision Records (ADR-001):** All high-level architectural proposals and changes must be documented via an ADR in `docs/modules/arc42/pages/09_architecture_decisions` and accepted before implementation code is merged.
- **Two-Module Maven Reactor:**
  - `torg-codex-data`: Reusable data layer (JPA entities, Liquibase migrations, DriveThruRPG client, markup pipeline). Enabled via `@EnableTorgData`.
  - `torg-codex`: Spring Boot web application (REST controllers, JTE templates, DTO mappers).
- **Proprietary Fixtures:** Game data CSV files are largely proprietary and git-ignored. Code and tests must run against the public free-tier fixture set.

## Development Workflow & Quality Gates

1. **Specification & Plan Phase:** Ensure every feature spec and design plan checks against the constitution principles (Hexagonal boundaries, REST/OpenAPI contracts, Expand-Contract migrations, Censor/RBAC/ABAC security).
2. **Build Verification:** `./mvnw clean verify` must pass completely before merging.
3. **Database Migration Checks:** Validate that any schema modification adheres to the 3-phase naming conventions and backward compatibility rules.
4. **Architectural Migrations:** Architectural migrations MUST be incremental and leave the repository buildable after every task.
5. **Backward Compatibility:** Existing externally visible behavior MUST remain compatible unless explicitly specified otherwise.
6. **Documentation Alignment:** Keep `docs/modules/arc42` synchronized with implementation changes.

## Governance

- The TORG-CODEX Constitution supersedes all informal practices.
- Amendments require an approved Architecture Decision Record (ADR) and updates to `docs/modules/arc42`.
- All PRs, code reviews, and AI-generated plans must verify compliance with this constitution.

**Version**: 1.0.0 | **Ratified**: 2026-08-15 | **Last Amended**: 2026-08-15

