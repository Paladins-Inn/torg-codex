# Quickstart Validation Guide: Enforce Data-Persistence-Only Module Boundary
*(updated for consolidated Markup-to-Domain relocation — feature 002 integrated)*

**Branch**: `003-data-persistence-boundary` | **Date**: 2026-08-16 | **Revised**: 2026-08-16

This guide describes how to verify that the feature has been implemented correctly. Validation
is split into Phase A (markup relocation) and Phase B (architecture enforcement), with a final
full-build verification covering both phases.

---

## Prerequisites

1. **JDK 25** installed and active (Eclipse Temurin 25 recommended).
   ```bash
   java -version
   # Expected: version "25.x.x"
   ```
2. **Maven wrapper** available in the repository root.
   ```bash
   ls ./mvnw
   # Expected: file found
   ```
3. **Docker** available and running (required for Testcontainers in the full build only;
   not required for Phase A or B architecture-test-only paths).
4. **Current branch**: `003-data-persistence-boundary`.
   ```bash
   git branch --show-current
   # Expected: 003-data-persistence-boundary
   ```

---

## Phase A Validation — Markup Domain Service Relocation

### A-Step 1 — Domain module compiles with markup and commonmark

```bash
./mvnw clean compile -pl torg-codex-domain
```

**Expected**: Compiles successfully. The `de.paladinsinn.torg.codex.domain.markup` package
with 9 relocated classes is present and compiles against `commonmark`; `GameTokenRegistry`
remains a static utility and requires no Spring bean.

### A-Step 2 — DomainPurityArchitectureTest passes after markup move

```bash
./mvnw test -pl torg-codex -Dtest="DomainPurityArchitectureTest"
```

**Expected outcome**: GREEN. No Spring (`org.springframework.*`), JPA (`jakarta.persistence.*`),
or Hibernate (`org.hibernate.*`) imports in `de.paladinsinn.torg.codex.domain.*` — including
the new `domain.markup` package. If any `@Service` or `@Component` annotation was left in a
relocated class, this test would fail and identify the offending class.

### A-Step 3 — Data module compiles without markup package

```bash
./mvnw compile -pl torg-codex-data
```

**Expected**: Compiles successfully. The `de.paladinsinn.torg.codex.data.markup` package no
longer exists. `TorgEntity` imports `Censor` from `domain.markup` (valid since data depends on
domain).

### A-Step 4 — torg-codex module compiles with updated import paths

```bash
./mvnw compile -pl torg-codex
```

**Expected**: Compiles successfully. All 33 updated import paths (32 web-module
files plus `TorgEntity`) resolve against
`de.paladinsinn.torg.codex.domain.markup.*`.

### A-Step 5 — Markup unit tests run in domain module (no Spring context)

```bash
./mvnw test -pl torg-codex-domain
```

**Expected outcome**: 6 markup unit tests GREEN in `de.paladinsinn.torg.codex.domain.markup`:
- `ConditionalBlockProcessorTest`
- `EntityReferenceProcessorTest`
- `GameTokenProcessorTest`
- `MarkdownProcessorTest`
- `RawHtmlProcessorTest`
- `TorgMarkupServiceTest`

None of these tests should require or start a Spring application context. Failure here
indicates a test-to-production Spring coupling that must be resolved before proceeding.

### A-Step 6 — ApplicationPurityArchitectureTest unaffected

```bash
./mvnw test -pl torg-codex -Dtest="ApplicationPurityArchitectureTest"
```

**Expected**: GREEN. `MarkupConfiguration` (in `de.paladinsinn.torg.codex.markup.spring.*`)
is outside the checked `de.paladinsinn.torg.codex.application.*` scope; no violation.

### A-Step 7 — Censoring single-mechanism tests unaffected

```bash
./mvnw test -pl torg-codex -Dtest="CensoringSingleMechanismArchitectureTest"
```

**Expected**: All 4 tests GREEN. The `CurrentUserCensorFactory` → `ProductOwnershipResolver`
censoring chain is import-path-updated only; no behavioral change.

### A-Step 8 — Phase A gate: all pre-existing architecture tests pass

```bash
./mvnw test -pl torg-codex -Dtest="DomainPurityArchitectureTest,ApplicationPurityArchitectureTest,ModuleBoundaryArchitectureTest,CensoringSingleMechanismArchitectureTest,AdapterConventionArchitectureTest,FreezeListFormatTest,FreezeListEnforcementArchitectureTest"
```

**Expected**: All GREEN. This is the gate check before starting Phase B.

---

## Phase B Validation — Architecture Test Enforcement

### B-Step 1 — New data boundary test passes (SC-002, SC-003, SC-004 of spec 003)

```bash
./mvnw test -pl torg-codex -Dtest="DataPersistenceBoundaryArchitectureTest"
```

**Expected outcome**:

| Test method | Expected result |
|---|---|
| `dataMustNotReferenceSecurityIntegrationClasses` | GREEN |
| `dataMustNotReferenceDriveThruRpgIntegrationClasses` | GREEN |
| `dataMustNotImportSpringSecurityFrameworkClasses` | GREEN |

All three tests pass with zero violations from day one because:
1. Phase A removed the `data.markup` package (the only potential source of complexity)
2. `torg-codex-data` already contained no Security/DriveThruRPG imports (research Decision 2)

### B-Step 2 — Corrected adapter location tests pass (SC-004 of spec 003)

```bash
./mvnw test -pl torg-codex -Dtest="AdapterConventionArchitectureTest"
```

**Expected**: All 5 test methods GREEN (0 failures):

| Test method | Status | Notes |
|---|---|---|
| `controllersRemainInInboundAdapterPackage` | GREEN | Unchanged |
| `outboundAdaptersRemainInDataAdapterOutPackages` | GREEN | Unchanged |
| `driveThruRpgOutboundAdapterLivesUnderDriveThruAdapterOutHttp` | **GREEN** | Corrected from stale |
| `domainEventBridgeOutboundAdapterLivesUnderDriveThruAdapterOutEvent` | **GREEN** | Corrected from stale |
| `everyCatalogAreaControllerIntroducedInPhase4dExistsUnderApiController` | GREEN | Unchanged |

---

## Regression Test — SC-003 (Deliberate Violation Detection)

Verifies that the data-boundary rules are **genuinely enforced**, not trivially passing.

### Step 1 — Add a deliberate violation

In any production class under `torg-codex-data/src/main/java/`, add:

```java
// Example: in de.paladinsinn.torg.codex.data.model.Article (or any data class)
import de.paladinsinn.security.DriveThruUserDetails;
```

### Step 2 — Run data boundary test

```bash
./mvnw test -pl torg-codex -Dtest="DataPersistenceBoundaryArchitectureTest"
```

**Expected**: **BUILD FAILURE** with message:
```
torg-codex-data must not reference Security integration classes
- Class <de.paladinsinn.torg.codex.data.model.Article> imports class <de.paladinsinn.security.DriveThruUserDetails>
```

### Step 3 — Revert and confirm passing

```bash
git checkout torg-codex-data/src/main/java/de/paladinsinn/torg/codex/data/model/Article.java
./mvnw test -pl torg-codex -Dtest="DataPersistenceBoundaryArchitectureTest"
```

**Expected**: BUILD SUCCESS.

---

## Full Build Validation (requires Docker)

```bash
./mvnw clean verify
```

**Expected outcomes**:
- All four reactor modules compile cleanly
- All markup unit tests GREEN in `torg-codex-domain` (no Spring context)
- All architecture tests GREEN in `torg-codex` (including both new and corrected tests)
- `CensoringDifferentialTest` and characterization replay tests pass (zero REST API behavior change)
- All integration tests (`*IT`) pass under Failsafe with Testcontainers PostgreSQL
- Zero FreezeList violations for any new data-purity rule

---

## POM Boundary Verification (SC-005 of spec 003)

Verify `torg-codex-data` effective compile-scope dependency tree contains no Spring Security:

```bash
./mvnw dependency:tree -pl torg-codex-data | grep -i security
# Expected: no output
```

Verify `commonmark` has moved to domain, not data:

```bash
./mvnw dependency:tree -pl torg-codex-data | grep commonmark
# Expected: no output

./mvnw dependency:tree -pl torg-codex-domain | grep commonmark
# Expected: one line showing org.commonmark:commonmark:0.24.0
```

---

## Architecture Test Count Checkpoint

After implementing this feature, the following test count is expected:

| Class | Test method count | Change from pre-feature baseline |
|---|---|---|
| `DataPersistenceBoundaryArchitectureTest` | 3 | **NEW** (net +3) |
| `AdapterConventionArchitectureTest` | 5 | 0 net (2 replaced, 3 unchanged) |
| `DomainPurityArchitectureTest` | 1 | 0 (unchanged; now also guards `domain.markup`) |
| `ModuleBoundaryArchitectureTest` | 5 | 0 (unchanged) |
| `ApplicationPurityArchitectureTest` | 2 | 0 (unchanged) |
| `TransactionBoundaryArchitectureTest` | 3 | 0 (unchanged) |
| `CensoringSingleMechanismArchitectureTest` | 4 | 0 (unchanged) |
| `FreezeListFormatTest` | 1 | 0 (unchanged) |
| `FreezeListEnforcementArchitectureTest` | 4 | 0 (unchanged) |

**Total architecture test methods**: 28 (was 24 before this feature, net +4 — the original
plan counted +3; the revised scope adds one more corrected stale test that had `everyCatalogArea...`
as a 5th unchanged method in `AdapterConventionArchitectureTest`).

**Markup unit test counts (torg-codex-domain)**: 6 tests moved from `torg-codex-data` to
`torg-codex-domain`; net change: 0 markup tests added/removed, location changed.

---

## References

- Architecture boundary contract (updated): [`contracts/data-persistence-boundary-rule.md`](./contracts/data-persistence-boundary-rule.md)
- Package boundary model (updated): [`data-model.md`](./data-model.md)
- Research findings (extended): [`research.md`](./research.md)
- Feature 003 spec: [`spec.md`](./spec.md)
- Feature 002 spec (consolidated reference): [`../002-markup-to-domain/spec.md`](../002-markup-to-domain/spec.md)
- ADR-016: `docs/modules/arc42/pages/09_architecture_decisions/016_application-integration-boundaries.adoc`
- ADR-011: `docs/modules/arc42/pages/09_architecture_decisions/011_markdown.adoc`
- Freeze list: `specs/architecture-migration/freeze-list.md`
