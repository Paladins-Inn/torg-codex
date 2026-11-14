# Quickstart: Validate Domain-Level DRM Enforcement

**Feature**: `005-domain-drm-enforcement`

## Prerequisites

- JDK 25 and the Maven wrapper (repository root).
- A running Docker daemon for the Testcontainers integration tests.
- Optional: the proprietary catalog CSVs in `torg-codex-data/src/main/resources/db/load/`, for
  manual checks against real data. The automated DRM tests seed their own entries and do not need
  them.

## 1. Fast feedback (unit and architecture tests)

```bash
./mvnw -pl torg-codex-domain -Dtest='CatalogAccessPolicyTest,OwnedProductsTest,ProductBoundEntryTest,EntryAccessTest,ReferencesEntriesTest' test
./mvnw -pl torg-codex-application -am -Dtest='RestrictedCatalogQueryServiceTest,CatalogReferenceQueryServiceTest,CatalogPersistencePortTest' -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -pl torg-codex -am -Dtest='ProductOwnershipResolverTest,CurrentUserCensorFactoryTest,CatalogAccessObservabilityAdapterTest,StructuredLoggingTest,*ArchitectureTest' -Dsurefire.failIfNoSpecifiedTests=false test
```

**Expected**: all green. The architecture tests include the new invariants A1–A6 from
[contracts/catalog-access-ports.md](./contracts/catalog-access-ports.md).
They scan production classes across all modules and reject negative controller/page/consumer,
indirect-helper and unrestricted-bean fixtures, including adapters outside `api..`. The actual
query bean generic bindings must also be verified; checking the current controller list alone
is insufficient.

`StructuredLoggingTest` must parse actual production-configured ECS JSON output and verify
the DRM fields and absence of sentinel caller secrets/ownership data. Check
`logging.structured.format.console=ecs` and the file equivalent when file logging is enabled.
Inspect `./mvnw dependency:tree` and custom logging configuration to ensure no Logstash-specific
encoder remains. Capturing a log event without serializing it is not sufficient.

## 2. End-to-end DRM behaviour (Testcontainers, Surefire — module convention)

```bash
./mvnw -pl torg-codex -am -Dtest='CatalogDrmDifferentialTest,CatalogOpenApiContractTest' -Dsurefire.failIfNoSpecifiedTests=false test
```

**Expected**, for every one of the 14 areas (see
[contracts/rest-access-contract.md](./contracts/rest-access-contract.md)):

| Request | Anonymous | `ROLE_sourcebook-aysle` |
|---|---|---|
| list | contains the `core-rulebook` entry only | contains the `core-rulebook` and `sourcebook-aysle` entries |
| list `?cosm=` (where supported) | `core-rulebook` entries of that cosm only | both |
| detail of the `sourcebook-aysle`-only entry | `403` + teaser (`requiredProducts` = `sourcebook-aysle`) | `200` full detail |
| detail of the product-less entry | `404` | `404` |
| list entry referencing an aysle-only entry | embedded list omits it | embedded list contains it |
| metric `torg_codex_catalog_entries_without_products_total{area=…}` | increased | increased |

`CatalogOpenApiContractTest` validates the syntax/references of
`contracts/catalog-api.openapi.yaml` and actual response shapes for all 14 product-bound
areas and the three public reference-data areas. The YAML is a required T015 deliverable,
not an artifact already present before implementation. Validate full existing DTO schemas,
content types and filters, exact teaser allowlists, and missing/unassigned response
equivalence. Do not substitute Markdown or file-existence checks for OpenAPI validation.

Also verify that an owner without `ROLE_core-rulebook` gets core-gated passages in accessible
catalog details and public Articles, as expressly allowed by SC-007. With unchanged effective
ownership, unrelated rendered output must remain identical.

## 3. Regression proof (SC-005)

Temporarily make `RestrictedCatalogQueryService.findAll` return the unfiltered persistence result
and run steps 1 and 2. `RestrictedCatalogQueryServiceTest` and `CatalogDrmDifferentialTest` must
fail. Revert the change afterwards.

Run the production-wide architecture checks with their negative fixtures too. They must
detect direct and helper-mediated persistence reads by a newly introduced adapter and
unrestricted product-bound query-bean wiring; restore only deliberately introduced mutations.

## 4. Data correction (FR-018)

```bash
./mvnw -pl torg-codex-data -Dit.test='LiquibaseImportIT,CatalogDataFixMigrationIT' verify
```

The automated seeded test must execute SQL twice and assert exact group-product equality,
removal of stale non-`groups` grants, an empty-derived-set group ending unassigned,
preservation of already-assigned core cosms, all ten unassigned core slugs corrected,
no duplicate rows, and a no-op when affected entries are absent.

Optionally, in a database migrated with the proprietary CSVs:

```sql
SELECT c.slug FROM torg_cosm c
 WHERE NOT EXISTS (SELECT 1 FROM torg_cosm_products p WHERE p.cosm_id = c.id);       -- expect 0 rows
SELECT count(*) FROM torg_perk_group_products WHERE product = 'groups';               -- expect 0
SELECT g.name FROM torg_perk_group g
 WHERE NOT EXISTS (SELECT 1 FROM torg_perk_group_products p WHERE p.group_id = g.id); -- expect 0 rows
WITH derived AS (
  SELECT DISTINCT g.id AS group_id, pp.product
  FROM torg_perk_group g
  JOIN torg_perk p ON p.perk_group = lower(replace(g.name, ' ', '-'))
  JOIN torg_perk_products pp ON pp.perk_id = p.id
)
SELECT 'unexpected' AS mismatch, actual.group_id, actual.product
FROM torg_perk_group_products actual
WHERE NOT EXISTS (
  SELECT 1 FROM derived d
  WHERE d.group_id = actual.group_id AND d.product = actual.product
)
UNION ALL
SELECT 'missing', d.group_id, d.product
FROM derived d
WHERE NOT EXISTS (
  SELECT 1 FROM torg_perk_group_products actual
  WHERE actual.group_id = d.group_id AND actual.product = d.product
); -- expect 0 rows: exact equality, not merely removal of 'groups'
```

The zero-unassigned-group expectation above describes the inspected full dataset, whose
groups all have product-bearing perks. It is not a fallback rule for arbitrary data:
groups with no derived products must remain unassigned and fail closed.

## 5. Characterization fixtures

```bash
./mvnw -pl torg-codex -am -Dcharacterization.capture=true -Dtest=CharacterizationFixtureCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
git --no-pager diff --stat torg-codex/src/test/resources/characterization
```

**Expected diff**:
- `anonymous-list.json` (and `anonymous-cosm-filter.json`) lose non-free entries;
- `anonymous-detail.json` may become a `403` teaser;
- owner lists also lose entries from unowned third products; owner details may change through
  legitimate access decisions, changed sample selection, or filtered embedded/cosm references;
- authenticated Article/catalog text may gain core-gated passages under SC-007; Article
  availability and all unchanged-effective-ownership rendering remain unchanged;
- public Publication/Tag data and response shapes remain unchanged.

Review against the explicit ownership and reference rules, not a blanket anonymous-only
filename allowlist. Inspect any changed sampled id before accepting its response difference.

Review the diff before committing. Then run the full build:

```bash
./mvnw clean verify
```

## 6. Manual smoke test (running application)

```bash
curl -s localhost:8080/api/spells | jq length                                     # free tier only
curl -s -H "Authorization: ApiKey <key-owning-aysle>" localhost:8080/api/spells | jq length   # larger
curl -si localhost:8080/api/spells/<aysle-only-spell-id> | head -1                # HTTP/1.1 403
curl -s localhost:8080/actuator/prometheus | grep entries_without_products        # 0 after data fix
```
