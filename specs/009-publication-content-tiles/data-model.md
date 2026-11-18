# Data Model: Publication Content Tiles

No persisted fields or relationships change. The new projections live for one reader request.

| Entity or value | Fields and relationships | Rules |
|---|---|---|
| Publication (existing) | UUID `id`, display `name`, Codex assignment key, existing detail slug | The URL filter uses the UUID. Resolve the name and assignment key from the existing public catalog. A known publication without a usable assignment key is named and has zero matching entries. |
| Product-bound entry (existing) | UUID `id`, catalog area, set of assigned product Codex IDs | Many-to-many membership: any granted entry can contribute to each of its assigned publications. An unrelated textual reference is not membership. Unassigned entries remain inaccessible. |
| Publication content count | Area and nonnegative distinct entry count; scoped to the resolved publication detail | Exactly one value per product-bound area in homepage order, including zero. Deduplicate by entry UUID within an area. No count projections for other detail types. |
| Publication content tile | Area label, icon, positive count, optional destination | Presentation metadata comes from `CatalogArea`. Only positive count values become tiles. The destination exists only when signed in. No ownership state or entry data is added to the tile. |
| Publication thumbnail (existing, feature 008) | Primary JPEG URL and PNG fallback URL, both nullable | Derived from the publication's storefront product ID. Present on publication details for every reader; absent values render the placeholder. Not added to other detail types. |
| Requested publication filter | None, a UUID, or invalid input | Parse at the reader adapter only on product-bound routes. Omission is different from a blank, malformed, or repeated value. Preserve a safe token for future requests. |
| Applied publication selection | None, selected (UUID and name), or unavailable | Resolve UUID once on the current request. A deleted/unknown UUID and invalid syntax are unavailable, not unfiltered. Keep the assignment key internal to the application. |

## Read derivation and state transitions

- **Detail**: Resolve the publication by the existing detail path, then use its nonblank assignment key against each eligible area's granted entries. With no usable key, every count is zero; a missing publication stays not found. The web mapper combines positive counts with existing sign-in state to render tile destinations. If no tile remains, omit the grid and keep the existing detail content.
- **List**: No publication selection leaves the existing result unchanged. A selected publication intersects its membership with the category's granted entries and then with existing filters; the pre-other-filter total is the distinct granted count for that publication. An unavailable selection yields zero matching rows and an active clear action. Keep canonical item links and available facets based on the ordinary granted category data.
- **Navigation**: Sort, search, facets, and reload reapply the same publication selection from the GET URL or form token. Clear publication transitions to none while preserving the other applied criteria. Reset all clears every criterion. Direct access and session changes recalculate grants on the current request; no count or selection confers access.
