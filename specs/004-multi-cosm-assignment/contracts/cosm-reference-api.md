# API Contract: Cosm Reference on Catalog Endpoints

**Feature**: 004-multi-cosm-assignment
**Date**: 2026-09-12
**Applies to**: all catalog list and detail endpoints that expose a cosm
**Companion**: [`list-and-prose-fields.md`](./list-and-prose-fields.md) covers `unlockingPerk` and
the prose/markup fields.

This contract documents the externally visible behaviour that must hold after the change. It is a
**compatibility contract**: the field shape is unchanged, only its value semantics are extended.

---

## 1. Affected endpoints

| Resource | List | Detail |
|----------|------|--------|
| Items | `GET /api/items` | `GET /api/items/{id}` |
| Miracle lists | `GET /api/miracle-lists` | `GET /api/miracle-lists/{id}` |
| Perks | `GET /api/perks` | `GET /api/perks/{id}` |
| Power lists | `GET /api/power-lists` | `GET /api/power-lists/{id}` |
| Shards | `GET /api/shards` | `GET /api/shards/{id}` |
| Spell lists | `GET /api/spell-lists` | `GET /api/spell-lists/{id}` |
| Threats | `GET /api/threats` | `GET /api/threats/{id}` |
| Vehicles | `GET /api/vehicles` | `GET /api/vehicles/{id}` |

No endpoint is added, removed, renamed, or version-bumped. Per ADR-008 no new media type is
introduced, because the response schema is unchanged.

---

## 2. Request contract — cosm filter

```text
GET /api/{resource}?cosm={slug}
```

| Aspect | Contract |
|--------|----------|
| Parameter name | `cosm` — unchanged |
| Parameter value | A cosm **slug**, e.g. `core-earth`. Never a display name. |
| Cardinality | Single value. Filtering by several cosms at once is not supported. |
| Matching | An entry matches when `{slug}` is among its assigned cosms (exact match). |
| Omitted | Returns the unfiltered collection, unchanged. |
| Unknown slug | Returns an empty array with status `200`, not an error. |
| Display name passed | Returns an empty array; display names must not match. |

**Behavioural change**: entries assigned several cosms now match each of their cosms. Previously
they matched none. No entry that matched before stops matching (SC-003).

---

## 3. Response contract — the `cosm` field

The field keeps its existing JSON shape in both summary and detail representations:

```json
{
  "cosm": {
    "id": "<uuid|null>",
    "name": "<string>"
  }
}
```

### 3.1 Value rules

| Assigned cosms | `cosm` |
|----------------|--------|
| none | `null` |
| one, known | `{"id": "<uuid>", "name": "<display name>"}` |
| one, unknown slug | `{"id": null, "name": "<raw slug>"}` |
| several | `{"id": null, "name": "<display name>, <display name>, ..."}` |

### 3.2 Rules for the several-cosms case

- `id` is `null`. No single cosm identifies the entry, so no id is invented.
- `name` contains every assigned cosm, joined with `", "` (comma, then space).
- Entries are sorted alphabetically by their contributed display string, so repeated requests
  return a byte-identical value.
- An unresolvable slug contributes its raw slug text, so nothing is dropped.

### 3.3 Examples

Single cosm — unchanged from today:

```json
{
  "id": "a5516f46-6d84-425e-b042-75a7b3f9473a",
  "name": "Cessna 172",
  "cosm": { "id": "f56ffdc1-8637-4ebb-835f-4a39e24f21da", "name": "Core Earth" }
}
```

Several cosms — previously an unmatched encoded value:

```json
{
  "id": "959f00d3-1692-4d2f-8682-407dd5214b1a",
  "name": "Conviction",
  "cosm": {
    "id": null,
    "name": "Aysle, Core Earth, Cyberpapacy, Living Land, Nile Empire, Orrorsh"
  }
}
```

### 3.4 Prohibited output

A response MUST NEVER contain an encoded list representation, in any field:

```json
{
  "cosm": {
    "id": null,
    "name": "['core-earth', 'tharkold']"
  }
}
```

---

## 4. Consumer guidance

| Consumer behaviour | Impact |
|--------------------|--------|
| Reads `cosm.name` for display | Works. Multi-cosm entries now read as a readable enumeration. |
| Reads `cosm.id` and assumes non-null | Already unsafe today — unresolved slugs return `null`. Multi-cosm entries also return `null`. |
| Filters via `?cosm=<slug>` | Works, and now returns more complete results. |
| Splits `cosm.name` on `", "` | Yields the individual display names. Not a guaranteed contract, but stable: no known cosm display name contains a comma. |

---

## 5. Unchanged behaviour

- HTTP status codes, headers, and error shapes.
- Authentication and authorization (`Authorization: ApiKey <key>` and OIDC).
- Product-ownership censoring. The number of assigned cosms has no effect on visibility (FR-012).
- Every other field of every summary and detail DTO.
