# Architecture Freeze List

Version-controlled register of temporary, rationale-backed exceptions to the hexagonal
architecture rules enforced by the ArchUnit tests introduced in this migration
(`torg-codex/src/test/java/de/paladinsinn/torg/codex/architecture/`). Every entry MUST be
resolved (migrated away) or explicitly re-classified as a permanent, accepted deviation
before the migration (Phase 6 / T126–T127) is considered complete.

Status header: **Migration in progress**

| id | module | violating class/dependency | violated rule | rationale | baseline task | planned removal phase | status |
|----|--------|------------------------------|----------------|-----------|----------------|------------------------|--------|
| FL-001 | torg-codex-data | `de.paladinsinn.torg.codex.data.application.port.in.CatalogQuery` | Ports MUST live in `torg-codex-application` (FR-007), not `torg-codex-data` | Pre-migration baseline location, retained until Phase 4a relocates it | T011 | Phase 4a: Ports relocation | open |
| FL-002 | torg-codex-data | `de.paladinsinn.torg.codex.data.application.port.in.CatalogReferenceQuery` | Ports MUST live in `torg-codex-application` (FR-007), not `torg-codex-data` | Pre-migration baseline location, retained until Phase 4a relocates it | T011 | Phase 4a: Ports relocation | open |
| FL-003 | torg-codex-data | `de.paladinsinn.torg.codex.data.application.port.out.CatalogPersistencePort` | Ports MUST live in `torg-codex-application` (FR-008), not `torg-codex-data` | Pre-migration baseline location, retained until Phase 4a relocates it | T011 | Phase 4a: Ports relocation | open |
| FL-004 | torg-codex-data | `de.paladinsinn.torg.codex.data.application.port.out.CatalogReferencePersistencePort` | Ports MUST live in `torg-codex-application` (FR-008), not `torg-codex-data` | Pre-migration baseline location, retained until Phase 4a relocates it | T011 | Phase 4a: Ports relocation | open |
| FL-005 | torg-codex-data | `de.paladinsinn.torg.codex.data.application.service.CatalogQueryService` | Framework-free use-case services MUST live in `torg-codex-application` (FR-007), not `torg-codex-data` | Pre-migration baseline location, retained until Phase 4a relocates it | T011 | Phase 4a: Ports relocation | open |
| FL-006 | torg-codex-data | `de.paladinsinn.torg.codex.data.application.service.CatalogReferenceQueryService` | Framework-free use-case services MUST live in `torg-codex-application` (FR-007), not `torg-codex-data` | Pre-migration baseline location, retained until Phase 4a relocates it | T011 | Phase 4a: Ports relocation | open |
| FL-007 (G1 deviation, not a violation) | spec-level (FR-010) | AMQP publishers/listeners (no such class exists) | FR-010 names AMQP publishers/listeners as outbound-adapter candidates | The repository has no AMQP infrastructure today (no broker client, no publisher/listener, no configuration anywhere in the codebase), and this migration introduces no new business capabilities. Building a new AMQP outbound adapter from scratch is explicitly out of scope for this structural refactor. See the FR-010 deviation note in `spec.md` and task T005a. | T005a | N/A — revisit only if/when AMQP is first introduced to the system | accepted deviation |

## Format contract (enforced by `FreezeListFormatTest`, T123)

Every row MUST have a non-empty value for all eight columns: id, module, violating
class/dependency, violated rule, rationale, baseline task, planned removal phase, status.
`status` MUST be one of: `open`, `removed`, `accepted deviation`.
