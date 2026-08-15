# Architecture Freeze List

Version-controlled register of temporary, rationale-backed exceptions to the hexagonal
architecture rules enforced by the ArchUnit tests introduced in this migration
(`torg-codex/src/test/java/de/paladinsinn/torg/codex/architecture/`). Every entry MUST be
resolved (migrated away) or explicitly re-classified as a permanent, accepted deviation
before the migration (Phase 6 / T126–T127) is considered complete.

Status header: **Migration complete** (zero `open` entries; FL-007 remains as a
permanent, spec-level `accepted deviation` — see rationale in its row above)

| id | module | violating class/dependency | violated rule | rationale | baseline task | planned removal phase | status |
|----|--------|------------------------------|----------------|-----------|----------------|------------------------|--------|
| FL-007 (G1 deviation, not a violation) | spec-level (FR-010) | AMQP publishers/listeners (no such class exists) | FR-010 names AMQP publishers/listeners as outbound-adapter candidates | The repository has no AMQP infrastructure today (no broker client, no publisher/listener, no configuration anywhere in the codebase), and this migration introduces no new business capabilities. Building a new AMQP outbound adapter from scratch is explicitly out of scope for this structural refactor. See the FR-010 deviation note in `spec.md` and task T005a. | T005a | N/A — revisit only if/when AMQP is first introduced to the system | accepted deviation |

## Format contract (enforced by `FreezeListFormatTest`, T123)

Every row MUST have a non-empty value for all eight columns: id, module, violating
class/dependency, violated rule, rationale, baseline task, planned removal phase, status.
`status` MUST be one of: `open`, `removed`, `accepted deviation`.
