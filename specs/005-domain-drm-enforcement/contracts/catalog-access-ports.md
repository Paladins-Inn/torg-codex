# Contract: Catalog Access Ports and Architecture Invariants

**Feature**: `005-domain-drm-enforcement` | **Applies to**: FR-001, FR-002, FR-009 to FR-011, FR-014

## Ports (`torg-codex-application`, framework-free)

```java
// port.in — the only way inbound adapters read product-bound catalog entries
public interface RestrictedCatalogQuery<T extends ProductBoundEntry> {
    List<T> findAll();                 // GRANTED only; embedded references filtered
    List<T> findByCosm(String slug);   // same rules as findAll
    EntryAccess<T> findById(UUID id);  // Granted | Teaser | Absent
}

// port.out — effective ownership of the current caller (always includes the free tier)
public interface CallerProductsPort {
    OwnedProducts current();
}

// port.out — reported once per encounter of a product-less entry (no deduplication)
public interface CatalogAccessObserver {
    void entryWithoutProducts(EntityType area, UUID id, String name);
}
```

### Behavioural guarantees

| # | Guarantee |
|---|---|
| G1 | `CallerProductsPort.current()` is called at most once per use-case invocation. |
| G2 | Every `UNASSIGNED` decision, for a top-level or a referenced entry, results in exactly one `entryWithoutProducts` call during that invocation. |
| G3 | `findAll` / `findByCosm` never return an entry whose decision is not `GRANTED`. |
| G4 | `findById` returns `Teaser` only for `TEASER` decisions, and `Absent` for both not-found and `UNASSIGNED`. |
| G5 | Embedded reference lists of returned entries contain only ids whose decision is `GRANTED`, in their original order. The referenced area is loaded with **one** `findAllById` call per invocation. |
| G6 | The services carry no Spring, JPA, Hibernate or Micrometer dependency (existing `ApplicationPurityArchitectureTest`). |

## Wiring (`torg-codex`, `api.configuration.CatalogQueryConfiguration`)

| Bean type | Areas |
|---|---|
| `RestrictedCatalogQuery<T>` | Cosm, Item, Miracle, MiracleList, Perk, PerkGroup, Power, PowerList, Race, Shard, Spell, SpellList, Threat, Vehicle |
| `CatalogQuery<T>` | Article, Publication, Tag **only** |
| `CallerProductsPort` | provided by the existing `ProductOwnershipResolver` (`implements CallerProductsPort`) |
| `CatalogAccessObserver` | `CatalogAccessObservabilityAdapter` |

## Architecture invariants (ArchUnit, `torg-codex/src/test/.../architecture`)

| ID | Rule |
|---|---|
| A1 | Import all production classes from all four modules, not just `api..`. Catalog persistence ports may be used only by the approved catalog application services, persistence adapters, and explicitly named composition roots. Catalog JPA repositories/entities and concrete persistence adapters may not be used by inbound adapters in any package. The existing MapStruct-only cross-layer value conversions are not an inbound read exception. |
| A2 | Every inbound catalog adapter (REST/controller, server-rendered page, message consumer, export, or future adapter, regardless of package/name) must read product-bound entries through `RestrictedCatalogQuery` or the restricted `CatalogReferenceQuery` projection. Detect unrestricted `CatalogQuery<ProductBoundEntry subtype>` use in fields, constructors, method signatures and calls; reject raw or unresolved catalog-query types rather than treating erasure as safe. Direct use or construction of `CatalogQueryService` outside approved composition is forbidden. `CatalogQuery` is allowed only for Article, Publication and Tag. |
| A3 | Every type in `de.paladinsinn.torg.codex.domain.model` whose simple name corresponds to an `EntityType` implements `ProductBoundEntry`. |
| A4 | The existing `CensoringSingleMechanismArchitectureTest` passes **unchanged**: only `api.security` reads `GrantedAuthority`, and only `CurrentUserCensorFactory` depends on `ProductOwnershipResolver`. Configuration wires the resolver through the `CallerProductsPort` interface type. |
| A5 | No class in `de.paladinsinn.torg.codex.application..` or `de.paladinsinn.torg.codex.domain..` depends on `io.micrometer..`. |
| A6 | Inspect actual query bean definitions and resolved generic bindings in the Spring context: exactly the 14 product-bound areas use `RestrictedCatalogQuery`; unrestricted `CatalogQuery`/`CatalogQueryService` bindings are limited to Article, Publication and Tag. Raw/unknown bindings fail verification. Check direct generic construction as well as exposed bean types, so a restricted bean cannot delegate to an unrestricted product-bound service unnoticed. |

### Guard scope and exceptions

These are production-wide dependency constraints, not just rules selected by the existing
14 controller names or HTTP annotations. Inbound adapters must not acquire catalog persistence
dependencies through helpers, inherited members, constructor calls, or another adapter.
The composition exception is the exact `CatalogQueryConfiguration` class, not the whole
`configuration..` package. Approved application persistence consumers are
`CatalogQueryService` (reference data only), `RestrictedCatalogQueryService`,
`ReferencedEntryResolver`, and `CatalogReferenceQueryService`. Persistence adapter/repository
internals remain permitted within `torg-codex-data`; that is not permission for inbound code
to depend on them.

`CatalogDrmArchitectureTest` must exercise each rule against deliberately violating test
fixtures. Include a new REST controller, a page/export outside `api..`, and a message consumer
outside `api..` that try direct persistence access; an indirect helper/adapter bypass; and
an unrestricted `CatalogQueryService<Spell>` bean. Each must produce a rule violation. Include
positive fixtures for a new adapter using the restricted port and for the three allowed public
query bindings. Keep violating fixtures out of normal production-class imports.

ArchUnit enforces these declared boundaries; the test must not claim universal protection
based only on checking the current controller list. New catalog access capabilities must
extend the approved driving ports and the guard tests, not add broad package exemptions.

## Regression contract

If the filtering in `RestrictedCatalogQueryService` is replaced by a pass-through, at least
`RestrictedCatalogQueryServiceTest` and `CatalogDrmDifferentialTest` MUST fail (SC-005).
