# Data Model: Enforce Data-Persistence-Only Module Boundary
*(updated to reflect consolidated Markup-to-Domain relocation — feature 002 integrated)*

**Branch**: `003-data-persistence-boundary` | **Date**: 2026-08-16 | **Revised**: 2026-08-16

This feature introduces no new JPA entities, repositories, Liquibase changesets, or domain
objects (FR-008 of spec 003 / FR-006 of spec 002). The "data model" for this feature is the
**package boundary model** — the precise set of package roots permitted or prohibited in each
Maven module's production code, updated to reflect the consolidated markup relocation.

---

## Package Boundary Model

### torg-codex-domain (framework-free domain module)

The domain module MUST contain no Spring Framework, JPA, or Hibernate imports in production
source (`DomainPurityArchitectureTest` enforces this).

**Added by this feature**:

| Package root | Contents | Dependency required |
|---|---|---|
| `de.paladinsinn.torg.codex.domain.markup.*` | Markup pipeline: `TorgMarkupService`, `Censor`, `ConditionalBlockProcessor`, `EntityReferenceProcessor`, `RawHtmlProcessor`, `GameTokenProcessor`, `GameTokenRegistry`, `EntityType`, `MarkdownProcessor` — **no Spring or CDI annotations** | `org.commonmark:commonmark:0.24.0` (added to domain POM) |

**Existing domain package roots** (unchanged):

| Package root | Contents |
|---|---|
| `de.paladinsinn.torg.codex.domain.model.*` | Domain model value objects and aggregates |
| `de.paladinsinn.torg.codex.domain.event.*` | Domain events and publisher interface |
| `de.paladinsinn.torg.codex.domain.validation.*` | Domain model validator |

---

### torg-codex-application (integration boundary)

**Added by this feature** (new package within this module):

| Package root | Contents | Note |
|---|---|---|
| `de.paladinsinn.torg.codex.markup.spring.*` | `MarkupConfiguration` — a Spring `@Configuration` class providing `@Bean` definitions for the five processor classes and `TorgMarkupService`; `GameTokenRegistry` remains a static utility, not a bean | Follows the same pattern as `de.paladinsinn.drivethru.*` and `de.paladinsinn.security.*`; outside `de.paladinsinn.torg.codex.application.*` to avoid `ApplicationPurityArchitectureTest` violation |

**Existing application package roots** (unchanged):

| Package root | Contents |
|---|---|
| `de.paladinsinn.torg.codex.application.port.in.*` | Driving port interfaces (`CatalogQuery`, `CatalogReferenceQuery`) |
| `de.paladinsinn.torg.codex.application.port.out.*` | Driven port interfaces (`CatalogPersistencePort`, `CatalogReferencePersistencePort`, `DriveThruRpgProductPort`) |
| `de.paladinsinn.torg.codex.application.service.*` | Framework-free use-case services |
| `de.paladinsinn.security.*` | Spring Security integration (API-key filter, auth provider, security config) |
| `de.paladinsinn.drivethru.*` | DriveThruRPG client, adapters, configuration |

---

### torg-codex-data (persistence-only boundary)

**Removed by this feature** (Phase A):

| Package root | Status | Replacement |
|---|---|---|
| `de.paladinsinn.torg.codex.data.markup.*` | **DELETED** — all 9 classes moved to `torg-codex-domain` | `de.paladinsinn.torg.codex.domain.markup.*` |

**Permitted** production package roots (after Phase A):

| Package root | Contents | Rationale |
|---|---|---|
| `de.paladinsinn.torg.codex.data.model.*` | JPA entity classes (`@Entity`, value types, converters); `TorgEntity` base class (with `@Transient Censor` field — Censor import now from `domain.markup`) | Core persistence concern |
| `de.paladinsinn.torg.codex.data.repository.*` | Spring Data JPA repository interfaces | Core persistence concern |
| `de.paladinsinn.torg.codex.data.adapter.out.persistence.*` | JPA persistence adapter implementations | Outbound persistence adapters |
| `de.paladinsinn.torg.codex.data.mapper.*` | MapStruct entity↔domain mappers | Persistence-layer mapping concern |
| `de.paladinsinn.torg.codex.data` (root) | `TorgDataConfiguration`, `EnableTorgData`, `TorgCodexDataApplication` | Module entry point/configuration |
| `de.kaiserpfalz.liquibase.*` | Liquibase entity/repository support | Liquibase schema management |

**Prohibited** production package roots (enforced by `DataPersistenceBoundaryArchitectureTest` after Phase B):

| Prohibited pattern | Reason | Enforcing rule |
|---|---|---|
| `de.paladinsinn.security.*` | Security integration — owned by `torg-codex-application` (ADR-016) | `dataMustNotReferenceSecurityIntegrationClasses` |
| `de.paladinsinn.drivethru.*` | DriveThruRPG integration — owned by `torg-codex-application` (ADR-016) | `dataMustNotReferenceDriveThruRpgIntegrationClasses` |
| `org.springframework.security.*` | Spring Security framework classes | `dataMustNotImportSpringSecurityFrameworkClasses` |

**Note**: The `markup` package is no longer permitted or present after Phase A. No carve-out
(such as the original FR-010 exemption) is needed in the data-purity architecture rules.

---

### Adapter Location Model (post-ADR-016, unchanged by this feature)

| Class | Module | Package | Enforcing test |
|---|---|---|---|
| `DriveThruRpgProductAdapter` | `torg-codex-application` | `de.paladinsinn.drivethru.adapter.out.http` | `driveThruRpgOutboundAdapterLivesUnderDriveThruAdapterOutHttp` |
| `SpringDomainEventPublisherAdapter` | `torg-codex-application` | `de.paladinsinn.drivethru.adapter.out.event` | `domainEventBridgeOutboundAdapterLivesUnderDriveThruAdapterOutEvent` |

---

### Module Dependency Graph (unchanged except `commonmark` moves)

```
torg-codex-domain
  depends on: commonmark [NEW in domain POM]
    ↑
torg-codex-application  (also depends on: torg-codex-domain)
    ↑
torg-codex-data         (also depends on: torg-codex-domain, torg-codex-application)
  no longer depends on: commonmark [REMOVED from data POM]
    ↑
torg-codex              (depends on: all four modules; hosts ArchUnit test suite)
```

Direction of arrows: `A → B` means "A depends on B" (inner-to-outer). This feature introduces
no new inter-module dependency: `torg-codex-data` already depends on `torg-codex-domain`.

---

### Entities Relevant to This Feature

No JPA entities are created, modified, or removed. `TorgEntity` requires one import path update:

| Entity / Class | Change | Notes |
|---|---|---|
| `TorgEntity` (`@MappedSuperclass`) | Import path update: `data.markup.Censor` → `domain.markup.Censor` | `@Transient Censor censor` field and `withCensor(Censor)` / `render(String)` methods are logically unchanged |
| All 17 catalog `@Entity` classes | No change | They inherit from `TorgEntity` — the import update propagates transitively through the base class |

---

### Validation Rules (Architecture)

All boundary rules enforced through ArchUnit tests (no bean-validation or runtime checks):

| Rule ID | Description | Test class | Test method | Phase |
|---|---|---|---|---|
| Domain-001 | `torg-codex-domain.*` must not import Spring/JPA/Hibernate | `DomainPurityArchitectureTest` | `domainContainsNoSpringOrJpaImports` | A (passes after markup move) |
| App-001 | `torg-codex-application.*` must stay framework-free | `ApplicationPurityArchitectureTest` | `applicationContainsNoSpringOrJpaInfrastructureImports` | A (unaffected — MarkupConfiguration is outside `application.*` package) |
| Censoring-001 | Only `api.security` may derive product ownership from `GrantedAuthority` | `CensoringSingleMechanismArchitectureTest` | `grantedAuthorityDerivationIsConfinedToApiSecurityPackage` | A (unaffected) |
| Censoring-002 | Only `CurrentUserCensorFactory` may depend on `ProductOwnershipResolver` | `CensoringSingleMechanismArchitectureTest` | `onlyCensorFactoryDependsOnProductOwnershipResolver` | A (unaffected) |
| FR-001 | `torg-codex-data.*` must not depend on `de.paladinsinn.security.*` | `DataPersistenceBoundaryArchitectureTest` | `dataMustNotReferenceSecurityIntegrationClasses` | B |
| FR-002 | `torg-codex-data.*` must not depend on `de.paladinsinn.drivethru.*` | `DataPersistenceBoundaryArchitectureTest` | `dataMustNotReferenceDriveThruRpgIntegrationClasses` | B |
| FR-002+ | `torg-codex-data.*` must not depend on `org.springframework.security.*` | `DataPersistenceBoundaryArchitectureTest` | `dataMustNotImportSpringSecurityFrameworkClasses` | B |
| FR-003a | `DriveThruRpgProductAdapter` at `de.paladinsinn.drivethru.adapter.out.http` | `AdapterConventionArchitectureTest` | `driveThruRpgOutboundAdapterLivesUnderDriveThruAdapterOutHttp` | B |
| FR-003b | `SpringDomainEventPublisherAdapter` at `de.paladinsinn.drivethru.adapter.out.event` | `AdapterConventionArchitectureTest` | `domainEventBridgeOutboundAdapterLivesUnderDriveThruAdapterOutEvent` | B |

All rules delegate to `ArchitectureTestSupport.assertNoUnfrozenViolations`. No FreezeList entry
is permitted for any rule introduced or corrected by this feature (FR-011 of spec 003).

---

### Markup Pipeline Public API Contract (preserved exactly)

| Class | Public API preserved | Notes |
|---|---|---|
| `TorgMarkupService` | `render(String rawText, Set<String> ownedProducts) → String` | Logic, pipeline order, and null-handling unchanged |
| `Censor` | `of(TorgMarkupService, Set<String>) → Censor`; `unauthenticated(TorgMarkupService) → Censor`; `apply(String) → String` | All static factories and instance method preserved |
| `ConditionalBlockProcessor` | `process(String, Set<String>) → String` | Logic unchanged |
| `EntityReferenceProcessor` | `process(String) → String` | Logic unchanged |
| `RawHtmlProcessor` | `process(String) → String` | Logic unchanged |
| `GameTokenProcessor` | `process(String) → String` | Logic unchanged |
| `MarkdownProcessor` | `process(String) → String` | Logic unchanged |

Fully qualified class names change from `de.paladinsinn.torg.codex.data.markup.*` to
`de.paladinsinn.torg.codex.domain.markup.*`. No class is renamed. No method signature changes.
Callers update import paths only.
