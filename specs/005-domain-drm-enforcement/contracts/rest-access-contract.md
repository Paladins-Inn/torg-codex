# Contract: REST Access Behaviour for Product-Bound Catalog Areas

**Feature**: `005-domain-drm-enforcement` | **Applies to**: FR-003 to FR-008, FR-013, FR-015

## Authoritative specification

`catalog-api.openapi.yaml` is a required implementation deliverable (T015) and the catalog
REST API source of truth under constitution Principle III. This Markdown file explains the
design; it does not replace OpenAPI or imply that the YAML already exists.

The OpenAPI document must cover all 17 catalog collections, including the three public
reference-data collections, and describe:
- existing summary/detail and reference DTOs with their actual fields and required/null rules;
- supported cosm filters, UUID path parameters and existing content-negotiation media types;
- `200` list/detail responses, the closed `403` teaser schema (no extra entry properties),
  and existing `404` and invalid-UUID responses;
- `requiredProducts` ordering, nullable unresolved publication references, and the identical
  not-found behavior for nonexistent and product-less entries.

Do not replace existing DTO schemas with unconstrained objects. `CatalogOpenApiContractTest`
must validate representative anonymous/owner responses across all 14 product-bound areas,
the teaser field allowlist, and public reference-data responses against this document.
Validate the specification itself, not just the presence of the YAML file.

## Scope

The rules in this contract apply to these 14 collections:

| Area (`EntityType.tag`) | Collection path | Cosm filter |
|---|---|---|
| `cosm` | `/api/cosms` | — |
| `item` | `/api/items` | `?cosm=` |
| `miracle` | `/api/miracles` | — |
| `miracle-list` | `/api/miracle-lists` | `?cosm=` |
| `perk` | `/api/perks` | `?cosm=` |
| `group` | `/api/perk-groups` | — |
| `power` | `/api/powers` | — |
| `power-list` | `/api/power-lists` | `?cosm=` |
| `race` | `/api/races` | — |
| `shard` | `/api/shards` | `?cosm=` |
| `spell` | `/api/spells` | — |
| `spell-list` | `/api/spell-lists` | `?cosm=` |
| `threat` | `/api/threats` | `?cosm=` |
| `vehicle` | `/api/vehicles` | `?cosm=` |

The paths and the cosm-filter support stay exactly as they are today (verified against the
controllers on 2026-11-14).

`/api/articles`, `/api/publications` and `/api/tags` remain publicly available (FR-013).
Article gated rendering uses the same effective free-tier ownership as other rendered content
(FR-012, SC-007); otherwise the existing response formats remain unchanged.

"Effective owned products" always includes the free tier `core-rulebook`, plus the caller's
`ROLE_<codex-id>` authorities.

## `GET <collection>` and `GET <collection>?cosm=<slug>`

| Aspect | Contract |
|---|---|
| Status | `200` (unchanged) |
| Body | JSON array of the area's existing summary DTO (shape unchanged) |
| Content | only entries whose `products` intersect the effective owned products |
| Excluded | entries owned only through other products; entries without any product (fail-closed) |
| Teasers in lists | never |

## `GET <collection>/{id}`

| Situation | Status | Body |
|---|---|---|
| Entry accessible | `200` | existing detail DTO (shape unchanged). Embedded `spells` / `miracles` / `powers` contain only accessible ids. In-text `<IF:…>` passages are censored with the effective owned products. |
| Entry exists, assigned to products, none owned | `403` | `EntryTeaserDto` (below) |
| Entry exists but has no product | `404` | the same as a non-existent id (unchanged not-found response) |
| Entry does not exist | `404` | unchanged |
| Invalid UUID | unchanged | unchanged |

### `EntryTeaserDto` (403 body)

```json
{
  "id": "6f1c2d3e-0000-4000-8000-000000000001",
  "name": "Fireball",
  "type": "spell",
  "requiredProducts": [
    {
      "codexId": "sourcebook-aysle",
      "publication": { "id": "2b7e…", "title": "Aysle Sourcebook" }
    },
    {
      "codexId": "unpublished-thing",
      "publication": null
    }
  ]
}
```

| Field | Required | Notes |
|---|---|---|
| `id` | yes | the requested id |
| `name` | yes | the entry's display name |
| `type` | yes | `EntityType.tag()` |
| `requiredProducts` | yes, non-empty | the entry's assigned codex ids, sorted ascending. `publication` is `null` if no publication is known for that codex id. |

No other entry field is allowed in the teaser, including `text`, statistics, `clearanceLevel`,
`cosm`, `publications` and embedded references.

## Cross-references inside accessible responses

| Reference | Contract |
|---|---|
| `cosm` (`CosmRefDto`) | `{id, name}` if the cosm has products, whether or not the caller owns them. For a product-less cosm, it is `{id: null, name: <slug>}`, the same as an unknown slug. |
| `publications` (`PublicationRefDto[]`) | unchanged (reference data) |
| In-text entity links (`<spell:x>` → `<a href="/spells/x">`) | unchanged; the target endpoint applies this contract |

## Versioning

The response formats for `200` lists and details are unchanged. `EntryTeaserDto` is an additive
error body for `403`, so no media-type version bump is needed (ADR-008). It MUST be documented
in the authoritative OpenAPI specification, with this Markdown file as supporting documentation.
