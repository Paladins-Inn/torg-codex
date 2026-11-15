# Contract: Catalog Browse Ports, Usage Observer Port, and Architecture Guards

**Feature**: `006-codex-web-ui` | **Applies to**: FR-009, FR-016, FR-017, FR-019 | **ADRs**: ADR-005, ADR-019, ADR-022

## 1. Driving Ports (`torg-codex-application.port.in`)

### 1.1 Extended Existing Ports (Backward-Compatible)

- **`RestrictedCatalogQuery<T extends ProductBoundEntry>`**:
  - `List<T> findAll()` *(existing; notifies `CatalogUsageObserver.categoryAccessed`)*
  - `List<T> findByCosm(String cosm)` *(existing; notifies `categoryAccessed` + `cosmAccessed` when `cosm` is a known cosm slug)*
  - `EntryAccess<T> findById(UUID id)` *(existing; notifies `detailAccessed` for `DRM_GRANTED` / `TEASER`, or `detailNotFound` for `Absent`)*
  - `EntryAccess<T> findByIdentifier(String slugOrId)` *(new: resolves UUID if valid UUID syntax, else matches canonical `CatalogSlugs` slug within the area; applies identical `CatalogAccessPolicy`, `CatalogAccessObserver`, `ReferencedEntryResolver`, and `CatalogUsageObserver` rules)*

- **`CatalogQuery<T>`** (restricted to `Article`, `Publication`, `Tag` per `ArchUnit A2` & `A6`):
  - `List<T> findAll()` *(existing; notifies `categoryAccessed`)*
  - `List<T> findByCosm(String cosm)` *(existing; notifies `categoryAccessed` + `cosmAccessed` if valid)*
  - `Optional<T> findById(UUID id)` *(existing; notifies `detailAccessed(..., PUBLIC_REFERENCE)` when present, or `detailNotFound` when empty)*
  - `Optional<T> findByIdentifier(String slugOrId)` *(new: resolves UUID or canonical slug/`codexId` and notifies `CatalogUsageObserver` identically)*

### 1.2 New Generic UI Driving Ports

- **`CatalogBrowseQuery`**:
  ```java
  public interface CatalogBrowseQuery {
      CatalogBrowseResult browse(CatalogArea area, CatalogFilterCriteria criteria);
      CatalogDetailResult findDetail(CatalogArea area, String slugOrId);
  }
  ```
  **Guarantees**:
  - **B1**: Delegates product-bound reads exclusively to the area's `RestrictedCatalogQuery<P>` and public reference reads exclusively to `CatalogQuery<Article>`, `CatalogQuery<Publication>`, or `CatalogQuery<Tag>`.
  - **B2**: `browse(...)` triggers exactly **one** `categoryAccessed(area)` event per invocation (via the underlying query), plus one `cosmAccessed(cosm)` per valid known cosm slug in `criteria.cosms()`, plus one `groupingAccessed(dimension, groupKey)` per valid known group value in `criteria.groupings()`. Unknown/untrusted query parameter values are ignored for counter increments and match zero entries. Free-text `criteria.query()` never increments a metric counter.
  - **B3**: Carries zero Spring, JPA, Hibernate, JTE, or Micrometer dependencies.

- **`CodexOverviewQuery`**:
  ```java
  public interface CodexOverviewQuery {
      CodexHomeOverview loadOverview();
  }
  ```
  **Guarantees**:
  - **O1**: Resolves caller ownership once via `CallerProductsPort.current()` and computes accessible entry counts per `CatalogArea` plus owned/missing `Publication` cards without inflating `usage.category` browse counters on landing-page load.

---

## 2. Driven Port (`torg-codex-application.port.out`)

### `CatalogUsageObserver`

```java
public interface CatalogUsageObserver {
    void categoryAccessed(CatalogArea area);
    void cosmAccessed(String cosmSlug);
    void groupingAccessed(GroupingDimension dimension, String groupKey);
    void detailAccessed(CatalogArea area, String entrySlug, DetailAccessOutcome outcome);
    void detailNotFound(CatalogArea area);
}
```

- **U1 (Purity)**: Lives in `torg-codex-application.port.out` and uses only domain types (`CatalogArea`, `GroupingDimension`, `DetailAccessOutcome`, `String`).
- **U2 (Privacy)**: Accepts **no** principal, session, API key, authority, or `OwnedProducts` argument.
- **U3 (Group & Cosm Detail Access Equivalence)**: Opening the detail view (`DRM_GRANTED` or `PUBLIC_REFERENCE`) of a `COSM` entry also invokes `cosmAccessed(entrySlug)`; opening the detail view of a `PERK_GROUP`, `SPELL_LIST`, `MIRACLE_LIST`, `POWER_LIST`, or `TAG` entry also invokes `groupingAccessed(dimension, entrySlug)`, fulfilling *"every access/filter to a cosm [or grouping] should add to a metric counter"*.

---

## 3. Architecture Test Invariants (`ArchUnit`)

Existing invariants **`A1`–`A6`** (`CatalogDrmArchitectureTest`), `DomainPurityArchitectureTest`, `ApplicationPurityArchitectureTest`, `TransactionBoundaryArchitectureTest`, and `CensoringSingleMechanismArchitectureTest` remain in full force. In addition:

- **W1 (Controller Package Separation)**:
  - `@RestController` classes MUST reside in `de.paladinsinn.torg.codex.api.controller..` (all 17 catalog REST controllers preserved).
  - `@Controller` classes MUST reside in `de.paladinsinn.torg.codex.web.codex..` or `de.paladinsinn.torg.codex.web.admin..`.
- **W2 (Reader vs. Admin Surface Isolation)**:
  - Classes in `de.paladinsinn.torg.codex.web.codex..` MUST NOT depend on `de.paladinsinn.torg.codex.web.admin..`, and vice versa.
- **W3 (Censoring & Ownership Single Mechanism Extended to `web..`)**:
  - No class in `de.paladinsinn.torg.codex.web..` may depend on `ProductOwnershipResolver` or `GrantedAuthority` directly; prose censoring in `web..` goes exclusively through `CurrentUserCensorFactory`.
- **W4 (SLF4J Facade Enforcement — `ADR-022`)**:
  - Production classes across all four modules MUST NOT reference `java.util.logging..`, `System.out`, `System.err`, or `ch.qos.logback..` directly.
