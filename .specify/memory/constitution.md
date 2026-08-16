<!--
Sync Impact Report
==================
Version change: 1.0.1 -> 1.1.0 (MINOR: new mandatory governance rule added; no principle removed/redefined)

Modified principles:
- V. Multi-Tier Security, DRM Censorship & Data Protection
  - Corrected stale/incorrect reference to a non-existent `CensorInjectionAspect`; the
    censoring-authority-derivation rule is now phrased as a MUST requirement.
  - Added: single-mechanism rule — product-ownership derivation for censoring MUST be based
    exclusively on `ROLE_<codex-id>` GrantedAuthority entries in the SecurityContext, regardless
    of authentication method/principal type. Parallel/duplicate ownership-resolution mechanisms
    are prohibited and must be consolidated.
  - Added: test-adequacy rule — censoring tests MUST assert an actual output difference between
    at least two ownership states for known product-gated content; byte-for-byte-equal fixtures
    across auth variants do not constitute proof of correct role-based censoring.

Rationale: A post-implementation `/speckit.analyze` investigation of the Hexagonal Architecture
Migration found that the live REST API's censoring path (`CurrentUserCensorFactory` / `Censor`)
derives product ownership only from a DriveThruRPG API-key principal type, never from
`ROLE_<codex-id>` authorities, while a separate, documented-but-unwired class
(`SecuredMarkupService`) implements the authority-based approach without being used by any
controller. All 17 catalog areas' characterization fixtures were found to be byte-identical
between an anonymous and an authenticated "owner" auth variant, including for content
(the "Aysle" cosm) known to contain `<IF:...>` product-gated markup — meaning role-based
censoring was never actually exercised or proven correct by the migration's test suite. This
gap pre-dates the migration (the deleted `CensoringCatalogQuery` used the same factory) but was
never previously codified as a violation. These rules are added to make this class of gap a
detectable constitution violation going forward and to require its remediation.

Added sections: none (rules folded into existing Principle V)
Removed sections: none

Follow-up TODOs (deferred, non-governance, tracked as Next Actions below):
- Consolidate product-ownership derivation for censoring onto a single ROLE_<codex-id>-based
  mechanism (retire or rewire `SecuredMarkupService` vs. `CurrentUserCensorFactory`/`Censor`).
- Add/repair characterization or dedicated tests that assert differing output between an
  anonymous and a real product-owning auth variant for known gated content.
-->

# TORG-CODEX Constitution

## Core Principles

### I. Hexagonal Architecture & Clean Ports
Business and domain logic MUST remain at the center of the system, decoupled from framework, database, and transport concerns (ADR-005, concept: hexagonal-architecture).
- Domain modules MUST NOT depend on the Spring Framework or JPA.
- Domain objects SHOULD use Lombok (including Lombok constructors like `@AllArgsConstructor`, `@Builder`, etc.), `@Slf4j` for logging, and `@EqualsAndHashCode`/`@ToString`/`@Getter` to minimize boilerplate (ADR-014). JPA entities MUST restrict `@EqualsAndHashCode` to the identity field only (`onlyExplicitlyIncluded = true`).
- Bean validation MUST be used outside of constructors (e.g., in domain services, use cases, or dedicated factory methods) rather than directly in the domain constructors.
- Separate models MUST be used for persistence (JPA entities) and the Domain core.
- Application services expose use cases through ports (interfaces).
- Primary/driving ports define incoming capabilities; secondary/driven ports define outgoing operations.
- Adapters translate between external protocols and domain ports. REST controllers are inbound adapters; Persistence implementations are outbound adapters.
- Dependencies MUST always point inward toward the domain/application core.
- Module boundaries MUST be enforceable through automated architecture tests.
- Mapping between layers (Domain <-> DTOs, Domain <-> JPA Entities) MUST be done using MapStruct (`componentModel = "spring"`), placed in the adapter package that performs the conversion, never inside domain modules (ADR-015).

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
- Content rendering MUST be censored based on product ownership authorities (`ROLE_<codex-id>`) present as Spring Security `GrantedAuthority` entries in the current `SecurityContext`. Repository queries, entities, controllers, and mappers MUST NOT bypass censoring.
- There MUST be exactly one authorization mechanism that derives product ownership for censoring/gated-content rendering across the entire application. This mechanism MUST resolve ownership from `ROLE_<codex-id>` `GrantedAuthority` entries in the current `SecurityContext`, independent of the authentication method or principal type (API key, OIDC, or any other) used to establish that context. Deriving ownership from a principal-type-specific lookup (e.g., a single authentication-provider's user-details object) instead of, or in addition to, the shared `ROLE_<codex-id>`-authority mechanism is prohibited. Parallel or duplicate censoring/ownership-resolution implementations (e.g., two unrelated services each deciding product ownership differently) MUST NOT coexist; any newly discovered duplicate MUST be consolidated into the single authorized mechanism as a priority fix, not left as parallel dead or partially-wired code.
- Automated tests verifying censored/gated content MUST assert on an actual difference in rendered output between at least two distinct ownership states (e.g., anonymous vs. an authenticated owner of the relevant product) for content that is known to contain product-gated markup. Tests that only assert byte-for-byte equality between such variants without ever exercising a case where the two states are expected to differ MUST NOT be treated as proof that role-based censoring functions correctly.
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

**Version**: 1.1.0 | **Ratified**: 2026-08-15 | **Last Amended**: 2026-08-16

