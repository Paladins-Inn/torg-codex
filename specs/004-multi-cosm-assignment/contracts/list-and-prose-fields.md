# Contract: unlocking-perk references and prose fields

**Feature**: 004-multi-cosm-assignment
**Companion to**: [`cosm-reference-api.md`](./cosm-reference-api.md)
**Status**: Design

This contract covers the second and third defect families of the feature: the `unlockingPerk`
reference on the three list types, and the prose/markup fields that currently hold encoded list
literals.

---

## 1. Affected endpoints

### 1.1 Unlocking-perk reference

| Endpoint | DTO | Field |
|----------|-----|-------|
| `GET /api/miracle-lists/{id}` | miracle list detail | `unlockingPerk` |
| `GET /api/power-lists/{id}` | power list detail | `unlockingPerk` |
| `GET /api/spell-lists/{id}` | spell list detail | `unlockingPerk` |

There is **no** `?unlockingPerk=` query filter today and this feature does not add one. The
repository method `findByUnlockingPerk` is not reachable from any port, service or controller
(research R9), so no request contract changes.

### 1.2 Prose fields

| Endpoint | DTO | Field |
|----------|-----|-------|
| `GET /api/items/{id}` | item detail | `additionalFeatures` |
| `GET /api/threats/{id}` | threat detail | `quote` |
| `GET /api/spell-lists/{id}` | spell list detail | `notes` |

---

## 2. Response contract — `unlockingPerk`

The field keeps its existing name, position and JSON type (`string` or `null`), per FR-018.

### 2.1 Value rules

| Assignments | Value |
|-------------|-------|
| none | `null` |
| one | that perk's **slug**, byte-identical to the value returned today |
| several | the slugs joined with `", "`, sorted alphabetically |

Values are **never** resolved to perk display names (FR-019). This differs deliberately from the
`cosm` field, which does resolve, because `unlockingPerk` has always exposed a slug and consumers
use it as an identifier.

### 2.2 Examples

Single assignment — unchanged from today:

```json
{
  "id": "…",
  "name": "Miracles of Light",
  "unlockingPerk": "miracles"
}
```

Several assignments:

```json
{
  "id": "…",
  "name": "Miracles of Light",
  "unlockingPerk": "exemplar-of-light, miracles"
}
```

No assignment:

```json
{
  "id": "…",
  "name": "Miracles of Light",
  "unlockingPerk": null
}
```

### 2.3 Prohibited output

```json
{
  "unlockingPerk": "['miracles', 'exemplar-of-light']"
}
```

---

## 3. Response contract — prose fields

**The JSON contract does not change.** These fields are and remain free-form
Markdown-bearing strings. Only the stored content is repaired, so the rendered
output changes.

### 3.1 Value rules

| Stored source | Value after repair |
|---------------|--------------------|
| encoded list with several statements | Markdown bullet list, `- ` per statement, `\n`-joined, source order preserved (FR-022, FR-025) |
| encoded list with exactly one statement | that statement as plain text, no bullet (FR-023) |
| empty encoded list | empty |
| anything not entirely an encoded list | byte-identical to today (FR-024) |

### 3.2 Example

Before:

```json
{
  "additionalFeatures": "[\"alters user's voice\", 'tests to fool target are Favored']"
}
```

After:

```json
{
  "additionalFeatures": "- alters user's voice\n- tests to fool target are Favored"
}
```

Rendered through the existing markup pipeline this yields a two-item HTML list.

### 3.3 Prohibited output

No prose field may begin with `[` and end with `]` while enclosing quoted statements.

---

## 4. Consumer guidance

| Consumer behaviour | Impact |
|--------------------|--------|
| Reads `unlockingPerk` as a single slug | Works unchanged for the single-assignment case, which is the overwhelming majority. Multi-assignment entries now yield a comma-separated slug list. |
| Compares `unlockingPerk` to a known slug for equality | Breaks only for multi-assignment entries, which previously returned an unusable encoded literal — so no working behaviour regresses. |
| Splits `unlockingPerk` on `", "` | Yields individual slugs. Safe: slugs never contain a comma or a space. |
| Renders prose fields as Markdown | Works, and multi-statement values now render as proper lists instead of raw Python literals. |
| Renders prose fields as plain text | Works. Bullet markers are visible but readable, unlike the previous literals. |

---

## 5. Unchanged behaviour

- HTTP status codes, headers, and error shapes.
- Authentication and authorization.
- Product-ownership censoring; prose repair runs before censoring and does not alter visibility.
- Field names, positions, and JSON types on every affected DTO.
- Every other field of every summary and detail DTO.
