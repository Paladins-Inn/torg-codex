# TORG-CODEX Copilot Instructions

## Build and test

- Use JDK 25 and the Maven wrapper from the repository root.
- Build the complete reactor (including integration tests): `./mvnw clean verify`
- Run the unit-test phase: `./mvnw test`
- Run one data-module unit test: `./mvnw -pl torg-codex-data -Dtest=TorgMarkupServiceTest test`
- Run one application-module unit test (building its data-module dependency): `./mvnw -pl torg-codex -am -Dtest=SecuredMarkupServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Run the Testcontainers-backed Liquibase integration test: `./mvnw -pl torg-codex-data -Dit.test=LiquibaseImportIT verify` (requires a running Docker daemon).
- There is no repository lint command. Maven compilation includes Lombok, MapStruct, JTE generation, and Hibernate enhancement.

## Architecture

- This is a two-module Maven reactor:
  - `torg-codex-data` is the reusable data layer. It owns JPA entities/repositories, Liquibase schema and data loading, the DriveThruRPG HTTP client and API-key security support, and the Torg markup pipeline.
  - `torg-codex` is the Spring Boot application. It enables the data, DriveThruRPG, and security modules; exposes REST controllers and JTE pages; and maps entities to DTOs through MapStruct.
- The data module is enabled through `@EnableTorgData`; do not rely on application component scanning to discover its entities or repositories.
- Requests may optionally authenticate with `Authorization: ApiKey <key>`. The application security chain permits all routes and uses product ownership authorities (`ROLE_<codex-id>`) to control rendered content rather than route access.
- Markup rendering is ordered and must remain so: conditional product blocks, entity references, raw HTML, game tokens, then CommonMark. `TorgMarkupService` accepts product IDs; `SecuredMarkupService` derives them from `ROLE_` authorities.
- Repository results receive a user-specific `Censor` through `CensorInjectionAspect`. Entity getters that render text depend on this injection; do not manually bypass it by constructing or exposing uncensored entities.

## Project conventions

- Keep web adapters thin: controllers use Spring Data repositories and MapStruct mappers, returning summary DTOs for lists and detail DTOs for item endpoints. Add matching DTO and mapper changes when extending an entity API.
- Use MapStruct mappers as Spring components (`componentModel = "spring"`) and share conversion rules through `TorgMappingSupport`.
- The data module uses Java records/value types where appropriate plus Lombok for entities. JPA entities extend `TorgEntity`, use generated UUID IDs, and keep product collections on concrete entities for JPA collection-table naming.
- Database changes use Liquibase under `torg-codex-data/src/main/resources/db`. Never alter an applied changeset. For breaking changes, use separate expand, migrate/backfill, and contract releases; the contract step only follows after old application instances have drained.
- Most game-data CSV and load files are intentionally Git-ignored proprietary content. Do not add, regenerate, or assume the presence of those files; retain the public free-tier load data and test against the available fixture set.
- Tests use JUnit 5 and AssertJ; isolate collaborators with Mockito for unit tests. `*IT` tests run under Failsafe during `verify` and use real PostgreSQL through Testcontainers.

## Documentation and contributions

- Keep architecture changes aligned with `docs/modules/arc42`, especially the data/markup and database-migration concepts.
- Preserve public API names and behavior. Contributions require git sign-off under the repository CLA process described in `CONTRIBUTING.md`.
