# REST Compatibility Contract

## Purpose

This is a **non-evolution contract** for the architecture migration. It documents the
current public catalog REST surface so implementation characterization tests can reject
any externally visible change. It does not authorize a new endpoint, version, field,
header, or schema.

## Global invariants

- Preserve every current path, HTTP method, path/query parameter, status code, JSON
  shape, field name, null/omission behavior, collection/map ordering, response header,
  and content negotiation result.
- Preserve the IANA PEN 33132 media-type version negotiation:
  `application/vnd.1.3.6.1.4.1.33132.1.v<version>+<format>`. The recorded baseline is
  authoritative for the versioned media type and headers emitted for each request.
- `GET /api/{resource}` returns the existing resource-specific summary DTO list;
  `GET /api/{resource}/{id}` returns the existing detail DTO with `200`, or the existing
  empty `404` response if no record exists. Invalid UUID, parameter, authentication, and
  authorization behavior must be captured and remain unchanged.
- No request body is added to these GET resources. No `POST`, `PUT`, `PATCH`, or
  `DELETE` catalog operation is added by this migration.
- For `Player`, `GM`, `Third Party Systems`, `Orga`, `Judge`, and `Admin`, fixtures
  must preserve RBAC/ABAC/UMA outcomes and every DRM-censored/omitted field. Markup
  output is compared after the existing rendering pipeline.
- `/login` browser handlers are not catalog REST endpoints, but their route and behavior
  are out of scope for alteration and must remain untouched.

## Endpoint inventory

All endpoints below expose both list and detail forms unless stated otherwise.

| Resource | List contract | Detail contract | DTO contract |
|---|---|---|---|
| Articles | `GET /api/articles` | `GET /api/articles/{id}` | `ArticleSummaryDto` / `ArticleDetailDto` |
| Cosms | `GET /api/cosms` | `GET /api/cosms/{id}` | `CosmSummaryDto` / `CosmDetailDto` |
| Items | `GET /api/items[?cosm={cosm}]` | `GET /api/items/{id}` | `ItemSummaryDto` / `ItemDetailDto` |
| Miracles | `GET /api/miracles` | `GET /api/miracles/{id}` | `MiracleSummaryDto` / `MiracleDetailDto` |
| Miracle lists | `GET /api/miracle-lists[?cosm={cosm}]` | `GET /api/miracle-lists/{id}` | `MiracleListSummaryDto` / `MiracleListDetailDto` |
| Perks | `GET /api/perks[?cosm={cosm}]` | `GET /api/perks/{id}` | `PerkSummaryDto` / `PerkDetailDto` |
| Perk groups | `GET /api/perk-groups` | `GET /api/perk-groups/{id}` | `PerkGroupSummaryDto` / `PerkGroupDetailDto` |
| Powers | `GET /api/powers` | `GET /api/powers/{id}` | `PowerSummaryDto` / `PowerDetailDto` |
| Power lists | `GET /api/power-lists[?cosm={cosm}]` | `GET /api/power-lists/{id}` | `PowerListSummaryDto` / `PowerListDetailDto` |
| Publications | `GET /api/publications` | `GET /api/publications/{id}` | `PublicationSummaryDto` / `PublicationDetailDto` |
| Races | `GET /api/races` | `GET /api/races/{id}` | `RaceSummaryDto` / `RaceDetailDto` |
| Shards | `GET /api/shards[?cosm={cosm}]` | `GET /api/shards/{id}` | `ShardSummaryDto` / `ShardDetailDto` |
| Spells | `GET /api/spells` | `GET /api/spells/{id}` | `SpellSummaryDto` / `SpellDetailDto` |
| Spell lists | `GET /api/spell-lists[?cosm={cosm}]` | `GET /api/spell-lists/{id}` | `SpellListSummaryDto` / `SpellListDetailDto` |
| Tags | `GET /api/tags` | `GET /api/tags/{id}` | `TagSummaryDto` / `TagDetailDto` |
| Threats | `GET /api/threats[?cosm={cosm}]` | `GET /api/threats/{id}` | `ThreatSummaryDto` / `ThreatDetailDto` |
| Vehicles | `GET /api/vehicles[?cosm={cosm}]` | `GET /api/vehicles/{id}` | `VehicleSummaryDto` / `VehicleDetailDto` |

`cosm` is optional only for the listed resource families. Its filtering result, including
ordering and empty-result behavior, is part of the contract.

## Schema authority and characterization

The existing DTO records in `torg-codex/src/main/java/.../api/dto` are the current wire
schema authority until the repository's OpenAPI single source is materialized. The
migration preserves them byte-for-byte at the HTTP boundary; internal domain models and
ports are not public schemas.

For each table row, capture at least:

1. anonymous/unauthorized and each defined-role request as applicable;
2. one accessible resource, one censored resource, one not-found UUID, and each
   supported `cosm` filter;
3. status, all compatibility-relevant headers, negotiated media type, and canonical JSON
   body (including array/map order); and
4. database before/after snapshot or equivalent persisted-record comparison for the
   affected family.

Any difference blocks the migration task unless a separately approved and documented
compatibility exception exists. This feature defines no such exception.
