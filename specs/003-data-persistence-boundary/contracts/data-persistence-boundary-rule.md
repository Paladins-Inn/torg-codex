# Architecture Boundary Rule: Data-Persistence-Only Module Invariant
*(updated to reflect consolidated Markup-to-Domain relocation — feature 002 integrated)*

**Feature**: `003-data-persistence-boundary` | **Date**: 2026-08-16 | **Revised**: 2026-08-16

This contract defines the invariants enforced by the architecture test classes introduced or
updated by this feature. It is the machine-verifiable expression of constitution Principle I's
module ownership rule and ADR-016's accepted module boundary, extended to cover the markup
relocation required by the feature 002 consolidation.

---

## Invariant 1 — torg-codex-data Security Integration Exclusion

**Rule name** (as declared in the ArchUnit `.as(...)` clause):
```
torg-codex-data must not reference Security integration classes
```

**Scope**: All production classes in `de.paladinsinn.torg.codex.data.*`
(scanned by `ArchitectureTestSupport.IMPORTED_CLASSES`; `ImportOption.DO_NOT_INCLUDE_TESTS`
applied — test classes excluded from enforcement).

**Prohibited dependency target**: Any class in `de.paladinsinn.security.*`

**Triggering examples**:
```java
import de.paladinsinn.security.ApiKeyAuthenticationFilter;
import de.paladinsinn.security.DriveThruAuthenticationProvider;
import de.paladinsinn.security.DriveThruSecurityConfig;
import de.paladinsinn.security.DriveThruUserDetails;
import de.paladinsinn.security.DriveThruUserService;
import de.paladinsinn.security.NotLoggedInUserDetails;
import de.paladinsinn.security.EnableDrivethruRPGSecurity;
```

**Test host**: `DataPersistenceBoundaryArchitectureTest`
**Test method**: `dataMustNotReferenceSecurityIntegrationClasses`
**FreezeList**: No entry permitted (FR-011). All violations fixed at source.
**Baseline**: Zero violations. Verified by research Decision 2 (pre-Phase A) and by Phase A
removing the markup package from `torg-codex-data` (no residual risk). No FR-010 carve-out
applies: the markup package no longer exists in `torg-codex-data` when this rule is introduced.

---

## Invariant 2 — torg-codex-data DriveThruRPG Integration Exclusion

**Rule name**:
```
torg-codex-data must not reference DriveThruRPG integration classes
```

**Scope**: All production classes in `de.paladinsinn.torg.codex.data.*`

**Prohibited dependency target**: Any class in `de.paladinsinn.drivethru.*`

**Triggering examples**:
```java
import de.paladinsinn.drivethru.DriveThruRPGService;
import de.paladinsinn.drivethru.adapter.out.http.DriveThruRpgProductAdapter;
import de.paladinsinn.drivethru.adapter.out.event.SpringDomainEventPublisherAdapter;
import de.paladinsinn.drivethru.client.DriveThruRPGClient;
import de.paladinsinn.drivethru.products.OwnedProduct;
```

**Test host**: `DataPersistenceBoundaryArchitectureTest`
**Test method**: `dataMustNotReferenceDriveThruRpgIntegrationClasses`
**FreezeList**: No entry permitted. All violations fixed at source.
**Baseline**: Zero violations (research Decision 2; no FR-010 carve-out needed).

---

## Invariant 3 — torg-codex-data Spring Security Framework Exclusion (complementary)

**Rule name**:
```
torg-codex-data must not import Spring Security framework classes
```

**Scope**: All production classes in `de.paladinsinn.torg.codex.data.*`

**Prohibited dependency target**: Any class in `org.springframework.security.*`

**Note**: Additive and defensive; guards against direct framework-level security imports not
caught by Invariants 1 and 2. The clean data POM (no Spring Security dependency) is the
structural guarantee; this rule is the class-level enforcement layer.

**Test host**: `DataPersistenceBoundaryArchitectureTest`
**Test method**: `dataMustNotImportSpringSecurityFrameworkClasses`
**FreezeList**: No entry permitted. All violations fixed at source.
**Baseline**: Zero violations (research Decision 2; no FR-010 carve-out needed).

---

## Invariant 4 — DriveThruRpgProductAdapter Location (corrected from stale)

**Rule name** (informational — enforced as a JUnit assertion in `AdapterConventionArchitectureTest`):
```
DriveThruRpgProductAdapter must exist under drivethru.adapter.out.http
as the outbound HTTP adapter for the DriveThruRPG product catalog
```

**Pre-ADR-016 (stale)**: `de.paladinsinn.torg.codex.data.adapter.out.http`
**Post-ADR-016 (correct)**: `de.paladinsinn.drivethru.adapter.out.http`

**Test host**: `AdapterConventionArchitectureTest`
**Test method**: `driveThruRpgOutboundAdapterLivesUnderDriveThruAdapterOutHttp`
(replaces `driveThruRpgOutboundAdapterLivesUnderDataAdapterOutHttp`)

---

## Invariant 5 — SpringDomainEventPublisherAdapter Location (corrected from stale)

**Rule name**:
```
SpringDomainEventPublisherAdapter must exist under drivethru.adapter.out.event
as the outbound bridge from torg-codex-domain events to Spring's ApplicationEventPublisher
```

**Pre-ADR-016 (stale)**: `de.paladinsinn.torg.codex.data.adapter.out.event`
**Post-ADR-016 (correct)**: `de.paladinsinn.drivethru.adapter.out.event`

**Test host**: `AdapterConventionArchitectureTest`
**Test method**: `domainEventBridgeOutboundAdapterLivesUnderDriveThruAdapterOutEvent`
(replaces `domainEventBridgeOutboundAdapterLivesUnderDataAdapterOutEvent`)

---

## Invariant 6 — torg-codex-domain markup package purity (Phase A)

**Rule name** (enforced by existing rule, no new test method needed):
```
torg-codex-domain must not import Spring, JPA, or Hibernate
(covering de.paladinsinn.torg.codex.domain.markup.* after relocation)
```

**Scope**: All production classes in `de.paladinsinn.torg.codex.domain.*`, which includes
`de.paladinsinn.torg.codex.domain.markup.*` after Phase A.

**Prohibited imports**: `org.springframework.*`, `jakarta.persistence.*`, `org.hibernate.*`

**Test host**: `DomainPurityArchitectureTest` (existing test — no new method required)
**Test method**: `domainContainsNoSpringOrJpaImports` (already covers `domain.markup.*`)
**FreezeList**: No entry permitted.
**Baseline**: Passes after Phase A removes Spring annotations from relocated markup classes.

---

## Invariant 7 — torg-codex-domain markup framework-binding adapter placement

**Rule name** (design invariant — enforced by package convention, not a standalone ArchUnit rule):
```
The Spring framework-binding configuration for domain markup classes MUST reside in
torg-codex-application at de.paladinsinn.torg.codex.markup.spring.*,
NOT in de.paladinsinn.torg.codex.application.* (which must stay framework-free).
```

**Scope**: `de.paladinsinn.torg.codex.markup.spring.MarkupConfiguration` in
`torg-codex-application` artifact.

**Rationale**: This placement follows the established pattern of `de.paladinsinn.drivethru.*`
and `de.paladinsinn.security.*` — both live in `torg-codex-application` jar but outside the
`de.paladinsinn.torg.codex.application.*` namespace that `ApplicationPurityArchitectureTest`
guards. No new ArchUnit rule is required for this invariant; the existing
`ApplicationPurityArchitectureTest` implicitly enforces it by passing cleanly (if
`MarkupConfiguration` were placed inside `application.*`, the test would fail).

---

## Governance Deliverables (non-ArchUnit invariants)

| Deliverable | Status in plan | Description |
|---|---|---|
| New ADR | Required implementation task (A8) | Records markup-to-domain decision; supersedes constitution v2.0.0 Principle I/VI markup assignment; specifies framework-binding adapter pattern |
| Constitution amendment (v2.0.0 → v2.1.0) | Required implementation task (A8) | Updates Principle I module ownership table and Principle VI markup pipeline owner |

---

## Non-Applicable Contracts

| Contract type | Reason |
|---|---|
| REST API contract | Zero externally visible API changes. All 17 catalog endpoints: byte-identical responses before and after. |
| Liquibase / schema contract | Zero schema or migration changes. |
| Port interface contract | All existing port interfaces in `de.paladinsinn.torg.codex.application.port.*` unchanged. |
| Domain event contract | No new domain events. `SpringDomainEventPublisherAdapter` unchanged. |

---

## Relationship to Other Architecture Rules

| Existing rule | Relationship to this feature |
|---|---|
| `DomainPurityArchitectureTest.domainContainsNoSpringOrJpaImports` | Now also guards `domain.markup.*` (Invariant 6). Passes after Phase A removes Spring annotations. |
| `ApplicationPurityArchitectureTest` | Unchanged. `MarkupConfiguration` lives outside its checked scope. |
| `ModuleBoundaryArchitectureTest.applicationDependsOnlyOnDomainAndJava` | Unchanged. `de.paladinsinn.torg.codex.markup.spring.*` is outside its checked scope. |
| `CensoringSingleMechanismArchitectureTest` | Unchanged. `CurrentUserCensorFactory` still sole censor-factory; import path updates only. |
| `AdapterConventionArchitectureTest.outboundAdaptersRemainInDataAdapterOutPackages` | Unchanged and passes: DriveThruRPG/Security adapters are not in `data.*` package tree. |
