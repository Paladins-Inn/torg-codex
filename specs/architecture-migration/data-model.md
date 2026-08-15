# Data Model: Hexagonal Architecture Migration

## Model separation rule

For every model below, create a framework-independent domain type in
`torg-codex-domain` and retain a distinct JPA entity in
`torg-codex-data`. JPA annotations, lazy/eager loading, repositories, encryption
converters, and censor injection remain persistence/adapter concerns. Data-side
MapStruct mappers perform entity ↔ domain conversion; web-side MapStruct mappers perform
domain ↔ existing DTO conversion. No table, column, join table, UUID, null convention,
or Liquibase changeset changes.

## Shared concepts

| Concept | Fields / rules | Relationships |
|---|---|---|
| Catalog entry | `id: UUID`, `name`, optional `clearanceLevel`, publication references; domain form is framework independent. | Base shape for catalog resources. |
| Clearance level | Value/enum corresponding to current product-gating clearance. `null` retains public accessibility. | Used to determine censorship, without carrying a Spring/JPA censor. |
| Publication reference | `id: UUID`, `title/name`. | Many catalog entries reference publications. |
| Cosm reference | `id: UUID`, `name`. | Used by cosm-associated catalog entries and API summaries/details. |
| Difficulty number | `level`, `text`. | Value object used by powers, spells, and miracles. |
| Vehicle weapon | `weaponId`, `ammo`, `amount`. | Value object collection owned by Vehicle. |

## Catalog domain models

| Model | Additional fields | Relationships and validation/preservation rules |
|---|---|---|
| Article | `text` | Publications; censored/rendered text outcome remains identical. |
| Cosm | axioms map, `text`, `worldLaws` | May be referenced by cosm-associated entries; preserve world-law censorship/rendering. |
| Item | `type`, technology/magic axioms, price, bonus, ammo, range, features, additional features, `text` | Cosm and publications; optional `cosm` query behavior remains unchanged. |
| Miracle | axiom, casting time, difficulty number, range, duration, required skills, `text` | Publications; preserve map ordering/null behavior. |
| MiracleList | `unlockingPerk`, miracle UUIDs, `text`, notes, disable condition | Cosm, publications, Miracle references. |
| Perk | contradiction flag, group, prerequisites, `text` | Cosm, publications, PerkGroup semantics. |
| PerkGroup | `text`, infos | Publications; name and detail behavior preserved. |
| Power | axiom, casting time, difficulty number, range, duration, required skills, `text`, enhancements, limitations | Publications. |
| PowerList | `unlockingPerk`, power UUIDs, `text`, notes, disable condition | Cosm, publications, Power references. |
| Publication | `id`, `codexId`, `name`, `primaryProductId`, optional `thirdParty`, product-ID set | Stable codex identity; cover URL is derived exactly as today from the primary product ID. |
| Race | major flag, attribute-limit map, abilities, `text`, perk text | Publications. |
| Shard | possibilities, tapping difficulty, purpose, `text`, powers, restrictions | Cosm and publications. |
| Spell | axiom, casting time, difficulty number, range, duration, required skills, `text` | Publications. |
| SpellList | `unlockingPerk`, spell UUIDs, `text`, notes, disable condition | Cosm, publications, Spell references. |
| Tag | optional `parentId` | Publications and self-reference; parent null behavior unchanged. |
| Threat | unique flag, subtitle, quote, `text`, attributes, skills, movement, toughness/shock/wounds, equipment, perks, possibilities, special abilities | Cosm and publications; preserve collection/map ordering. |
| Vehicle | type, technology axiom, unique flag, speed values/modifier, size, passengers, maneuver rating, wounds, toughness, price, weaponry, `text` | Cosm, publications, VehicleWeapon collection. |

## Ports and operational records

| Type | Fields / behavior | Relationship |
|---|---|---|
| Driving port | Domain-only command/query input and domain result types. | Implemented by an application service; invoked by REST or other inbound adapters. |
| Persistence driven port | Domain identity/query inputs and domain models/results, including existing cosm filter semantics. | Implemented by data JPA adapter through an entity-domain mapper. |
| External HTTP driven port | Domain request/result models only. | Implemented by DriveThruRPG adapter and verified with WireMock. |
| Domain event | Event name/type, domain payload, correlation/metadata only where currently observable. | Published through `DomainEventPublisher`; Spring bridge converts without altering listener-visible payload/delivery. |
| Architecture exception entry | Stable identifier, exact violating class/dependency/rule, rationale, baseline/introduced version, removal phase, status. | Consumed by ArchUnit freeze/suppression mechanism; no open entry is permitted at completion. |

## State transitions

This migration does not introduce new business lifecycle transitions. Persisted catalog
records retain their existing readable/writable behavior. The only tracked migration
state is architecture conformance:

```text
baseline violation recorded -> area migrated -> exception removed -> zero exceptions (complete)
```

An unrecorded violation is an immediate build failure; an open recorded violation is an
allowed interim condition but prevents migration completion.
