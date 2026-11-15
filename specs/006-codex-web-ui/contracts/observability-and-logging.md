# Contract: Usage Metrics and Operational Logging Policy

**Feature**: `006-codex-web-ui` | **Applies to**: FR-017, FR-018, SC-008 | **ADRs**: ADR-017, ADR-019, ADR-022

## 1. Micrometer / Prometheus Usage Counters (`CatalogUsageObservabilityAdapter`)

Implemented in `de.paladinsinn.torg.codex.api.observability.CatalogUsageObservabilityAdapter` and exposed at `/actuator/prometheus`. All counters are channel-agnostic (shared across Web UI and `/api/*` REST reads) and carry **zero** caller identity, credential, or `ROLE_<codex-id>` tags.

### 1.1 Granular Usage Counters (4 Meter Names)

| Family | Micrometer Name | Prometheus Name | Tags | Cardinality Bound |
|---|---|---|---|---|
| Category | `torg.codex.catalog.usage.category` | `torg_codex_catalog_usage_category_total` | `category` = `CatalogArea.slug()` | Exactly 17 values |
| Cosm | `torg.codex.catalog.usage.cosm` | `torg_codex_catalog_usage_cosm_total` | `cosm` = validated cosm slug | Known catalog cosms (~10–20) |
| Grouping | `torg.codex.catalog.usage.grouping` | `torg_codex_catalog_usage_grouping_total` | `dimension` = `GroupingDimension.tag()`, `group` = validated key | Known catalog groups/lists/tags/types/clearances/races |
| Detail | `torg.codex.catalog.usage.detail` | `torg_codex_catalog_usage_detail_total` | `category` = `CatalogArea.slug()`, `entry` = canonical slug or `"_not_found_"`, `outcome` = `drm-granted` \| `public-reference` \| `teaser` \| `not-found` | Known catalog entries × applicable outcomes + 17 `not-found` series |

### 1.2 Aggregate Roll-Up Counters — Set 1: Outcome Totals (4 Meters, Untagged)

Summed across all categories, groups, and entries:

| Outcome | Micrometer Name | Prometheus Name | Incremented When |
|---|---|---|---|
| DRM-Granted | `torg.codex.catalog.usage.outcome.drm.granted` | `torg_codex_catalog_usage_outcome_drm_granted_total` | `detailAccessed(..., DRM_GRANTED)` |
| Public Reference | `torg.codex.catalog.usage.outcome.public.reference` | `torg_codex_catalog_usage_outcome_public_reference_total` | `detailAccessed(..., PUBLIC_REFERENCE)` |
| Teaser | `torg.codex.catalog.usage.outcome.teaser` | `torg_codex_catalog_usage_outcome_teaser_total` | `detailAccessed(..., TEASER)` |
| Not Found | `torg.codex.catalog.usage.outcome.not.found` | `torg_codex_catalog_usage_outcome_not_found_total` | `detailNotFound(area)` (missing or unassigned entry) |

### 1.3 Aggregate Roll-Up Counters — Set 2: Metric-Family Totals (4 Meters, Untagged)

Summed across all items within each metric family:

| Metric Family | Micrometer Name | Prometheus Name | Incremented When |
|---|---|---|---|
| All Categories | `torg.codex.catalog.usage.family.category` | `torg_codex_catalog_usage_family_category_total` | Every `categoryAccessed(area)` (+1) |
| All Cosms | `torg.codex.catalog.usage.family.cosm` | `torg_codex_catalog_usage_family_cosm_total` | Every `cosmAccessed(cosmSlug)` (+1) |
| All Groupings | `torg.codex.catalog.usage.family.grouping` | `torg_codex_catalog_usage_family_grouping_total` | Every `groupingAccessed(dimension, groupKey)` (+1) |
| All Details | `torg.codex.catalog.usage.family.detail` | `torg_codex_catalog_usage_family_detail_total` | Every `detailAccessed(...)` or `detailNotFound(...)` (+1) |

**Invariant**: For any workload starting from zero:
- `family.detail == outcome.drm.granted + outcome.public.reference + outcome.teaser + outcome.not.found`
- `family.category == sum(usage.category{category=*})`
- `family.cosm == sum(usage.cosm{cosm=*})`
- `family.grouping == sum(usage.grouping{dimension=*,group=*})`

---

## 2. Operational Logging Contract (`ADR-017`, `ADR-022`)

- **Format**: Spring Boot native structured ECS JSON (`logging.structured.format.console=ecs`).
- **Default Production Level (`application.yaml`)**: `root: info` (no `debug` or `trace` package overrides in the default production profile).
- **Mandatory Event Severity Mapping**:

| Event | Required Level | Structured Fields (`addKeyValue`) | Forbidden Content |
|---|---|---|---|
| Unhandled controller/adapter runtime exception | `ERROR` | `requestPath`, `exceptionClass` | API keys, tokens, session IDs, `ROLE_<codex-id>` |
| Product-less catalog entry encountered (`ADR-019`) | `WARN` | `catalogArea`, `entryId`, `entryName` | Caller identity, authorities, owned products |
| Upstream DriveThruRPG HTTP `5xx` / timeout | `WARN` | `upstream`, `statusCode` | API key, bearer token |
| Successful API-key sign-in / sign-out (`POST /login`, `POST /logout`) | `INFO` | `event.action` (`user-login` / `user-logout`) | Raw `apiKey`, `ROLE_<codex-id>` list |
| Invalid API-key sign-in (`POST /login`) | `INFO` *(never `WARN`/`ERROR`)* | `event.action` (`user-login-rejected`), `reason` (`invalid-credentials`) | Submitted `apiKey` |
| Purchase teaser served (`403` on unowned entry) | `INFO` or `DEBUG` *(never `WARN`/`ERROR`)* | `catalogArea`, `entrySlug`, `outcome` (`teaser`) | Caller identity, owned products |
| Unknown category or entry identifier (`404`) | `INFO` or `DEBUG` *(never `WARN`/`ERROR`)* | `outcome` (`not-found`) | Caller identity, owned products |
| Browse/filter execution entry & applied criteria | `DEBUG` | `catalogArea`, `sortKey` | Caller identity, owned products |
| Step-by-step descriptor cell/section mapping | `TRACE` | `catalogArea`, `entrySlug` | Caller identity, owned products |
