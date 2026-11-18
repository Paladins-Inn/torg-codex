# Reader UI Contract: Publication Content Tiles

This adds to the [existing reader routes](../../006-codex-web-ui/contracts/web-ui-routes.md). It does not change the public REST contract.

## `GET /publications/{identifier}`

- Existing detail routes, including slug and UUID lookup, render the same publication details plus a section headed “Content in this publication” when at least one category has a positive count. When all counts are zero, omit the section and keep the existing detail content. A missing publication remains `404` and has no grid.
- The detail header shows the publication thumbnail with the homepage's markup contract: primary JPEG `src`, PNG `data-fallback-src`, and the placeholder when neither is available or both fail. It is decorative (`alt=""`), unlinked, and identical for every reader. `/js/publication-thumbnails.js` is loaded on publication details only; other detail types render no thumbnail.
- Labels, icons, presentation, and order follow the homepage's product-bound categories: Races, Perks, Perk Groups, Spells, Spell Lists, Miracles, Miracle Lists, Psionics, Power Lists, Items & Gear, Vehicles, Cosms, Eternity Shards, Threats. Publications, Articles, and Tags are not included.
- Each displayed tile shows a distinct count of granted entries assigned to the publication. Zero-count categories have no tile, regardless of sign-in state. A signed-in tile has one useful, named, focus-visible link to the canonical list path, e.g. `/spells?publication=<publication-uuid>`. Signed-out tiles have no `href`, link role, `tabindex`, or activation handler.

## `GET /{category}?publication=<uuid>`

The parameter applies only to product-bound categories. Canonical and existing alias routes preserve normal redirect behavior and query state. A single UUID uses the standard hyphenated representation (case-insensitive input, lowercase in generated links).

| Request state | Existing list route response |
|---|---|
| Parameter absent, or category is a public reference area | Existing unfiltered-by-publication behavior. |
| Known publication UUID | `200`, `Publication: <name>` and only granted, assigned entries matching the remaining filters. Show the normal empty state with the name still visible when nothing matches. |
| Unknown UUID; repeated, blank, or malformed parameter | `200`, `Publication unavailable` and the normal empty state. Offer the same clear action. Do not echo malformed input or silently broaden the results. |

- Regular GETs return the full page. `HX-Request: true` targeting `catalog-results` continues to return only the existing `#catalog-results` fragment, including the publication status and clear action. Both use `private, no-store` and retain the existing `Vary: HX-Request`. Browsing history restores a complete page for this filtered URL on a cache miss. Exclude new publication grids and filtered results from htmx local history snapshots.
- Preserve the selection across `q`, `cosm`, supported grouping parameters, `sort`, `dir`, and reload. The filter form and sort links carry the same parameter; forward a valid UUID (including an unknown UUID), or `publication=` for invalid syntax. Scope any result count denominator to the selection before other filters. A failed partial response must not apply a different publication selection to the existing results and URL.
- **Clear publication filter** uses a full GET without `publication` and preserves the latest applied search, facet, and sort state. Existing **Reset filters** actions return to the bare category route and use full GET navigation when clearing an active publication, so the form also loses the selection.
- Direct visits are allowed as under the existing list routes. The reader's current grants determine visible entries; the UUID and the availability of tile navigation are not access credentials. For unchanged data and grants, an unmodified list reached by a tile has exactly the originating tile count.
