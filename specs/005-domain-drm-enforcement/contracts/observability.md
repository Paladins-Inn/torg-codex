# Contract: Observability of Product-Less Entries

**Feature**: `005-domain-drm-enforcement` | **Applies to**: FR-016, FR-017, SC-008

## Trigger

Each call to `CatalogAccessObserver.entryWithoutProducts(area, id, name)`. This happens once per
product-less entry encountered in:
- listings and cosm-filtered listings;
- single-entry requests;
- embedded-reference filtering;
- cosm cross-reference resolution.

There is no deduplication across requests.

## Log event

| Aspect | Value |
|---|---|
| Logger | `de.paladinsinn.torg.codex.api.observability.CatalogAccessObservabilityAdapter` (via `@Slf4j`) |
| Level | `WARN` |
| Message | `Catalog entry without product assignment hidden by DRM: area={}, id={}, name={}` |
| Structured fields (SLF4J 2 fluent API `log.atWarn().addKeyValue(...)`; present in emitted Boot native ECS JSON) | `catalogArea` (`EntityType.tag()`), `entryId` (UUID), `entryName` |
| Forbidden content | caller identity, principal, authorities, owned products, API keys |

### Production output (ADR-017, constitution v3.0.0)

Use Spring Boot native structured logging with `logging.structured.format.console=ecs`;
configure `logging.structured.format.file=ecs` if file logging is enabled. No Logstash-specific
encoder dependency or custom encoder configuration is permitted. Remove any such dependency
or configuration found during implementation; the inspected POMs currently declare none.
Keep Lombok `@Slf4j` and the SLF4J fluent key-value API.

## Metric

| Aspect | Value |
|---|---|
| Micrometer name | `torg.codex.catalog.entries.without.products` |
| Prometheus name | `torg_codex_catalog_entries_without_products_total` |
| Type | Counter, incremented by 1 per trigger |
| Tags | `area` = `EntityType.tag()`. This is bounded to the 14 values, which keeps cardinality low. |
| Forbidden tags | entry id, entry name, caller data |
| Endpoint | `/actuator/prometheus` (existing) |

## Example alert rule (documented in `docs/modules/arc42/pages/08_concepts/observability.adoc`)

```yaml
groups:
  - name: torg-codex-drm
    rules:
      - alert: CatalogEntriesWithoutProducts
        expr: sum by (area) (increase(torg_codex_catalog_entries_without_products_total[15m])) > 0
        for: 15m
        labels:
          severity: warning
        annotations:
          summary: "Catalog entries without product assignment in area {{ $labels.area }}"
          description: "Entries are hidden from every caller (fail-closed DRM). Fix the catalog data."
```

## Verification

`CatalogAccessObservabilityAdapterTest` uses a `SimpleMeterRegistry` and a capturing appender to
check that one call produces exactly one `WARN` event and increases the counter by 1 for the given
`area`. It also checks that the event contains none of the forbidden content.

`StructuredLoggingTest` must additionally capture actual output using the production logging
configuration, parse emitted records as JSON, and assert ECS severity/message fields, the
three structured warning fields, and absence of caller/credential/ownership data. Include
sentinel caller values in the test context so exclusion assertions are meaningful. A
`ListAppender` assertion alone is not sufficient. Verify console output and file output when
enabled, without overriding the production format solely for the test.
