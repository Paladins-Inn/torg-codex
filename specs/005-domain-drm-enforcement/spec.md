# Feature Specification: Enforce Product-Ownership DRM at Domain Level

**Feature Branch**: `005-domain-drm-enforcement`

**Created**: 2026-11-14

**Status**: Draft

**Input**: User description: "The censoring drm is not working. Without user I get listed all included stuff. The access should be restricted. The drm has to work on all requests on domain level"

## Context

Today the only DRM in the codex is the censoring of product-gated passages *inside* an entry's text
(`<IF:product-id>` / `<IF:!product-id>` blocks, see feature `001-unify-censoring-authorization`).
The catalog entries themselves are not protected: every catalog entry belongs to one or more
products (sourcebooks), but listing, filtering by cosm, and fetching a single entry return every
entry to every caller, including anonymous visitors who only hold the free-tier product
(`core-rulebook`). As a result an anonymous visitor can enumerate and read the complete
proprietary catalog. Because this access decision is not part of the core catalog use cases,
every entry point would have to remember to apply it on its own — and none currently do.

## Clarifications

### Session 2026-11-14

- Q: When a caller requests a single entry they are not entitled to, how should the system respond? → A: Return a teaser containing only the entry's name and the product(s) that grant access; no protected content.
- Q: How should entries without any product assignment be treated? → A: Hidden from everyone (fail-closed).
- Q: What should happen operationally when an entry without any product assignment is encountered? → A: Log a warning so the data gap becomes visible to operators.
- Q: How often should the warning be logged for the same product-less entry? → A: Every time it is encountered (each request), without deduplication.
- Q: Should product-less entries also be counted in a monitoring metric? → A: Yes; expose a metric operators can alert on, in addition to the log warning.
- Q: (Planning) The 10 core cosms (Aysle, Core Earth, Cyberpapacy, Living Land, Nile Empire, Orrorsh, Pan-Pacifica, Tharkold, Universal, Unknown) have no product assigned and would be hidden from everyone. How should this be handled? → A: Correct the catalog data by assigning `core-rulebook` to these 10 cosms.
- Q: (Planning) All perk groups are assigned only to the non-existent product `groups`. How should perk groups be assigned? → A: Derive automatically: each perk group is assigned every product its perks are assigned to; the bogus `groups` assignment is removed.
- Review decisions: Require OpenAPI as the API source of truth; use Spring Boot native structured JSON logging without Logstash encoder dependencies (accepted ADR-017); keep every completed implementation task buildable; guard all inbound adapters against unrestricted catalog access; allow the effective-free-tier rendering change; reconcile perk-group products to the exact derived set, removing stale assignments.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Anonymous visitors only see free-tier entries (Priority: P1)

An anonymous visitor (no API key, no login) browses the codex. They MUST only be able to list,
filter, and open catalog entries that belong to the free-tier product. Entries that belong
exclusively to products they do not own MUST NOT appear in any listing or filtered listing; when
opened individually they MUST only yield a purchase teaser (name and the products that grant
access), never their protected content.

**Why this priority**: This is the reported defect and a direct intellectual-property leak:
proprietary sourcebook content is currently readable by anyone without owning it. Closing it is
the core value of this feature.

**Independent Test**: Issue anonymous requests against the list, cosm-filtered list, and
single-entry retrieval of each product-bound catalog area; verify that only free-tier entries are
returned and that a known sourcebook-only entry (e.g. one belonging only to `sourcebook-aysle`)
is not listed and, when opened individually, yields only its teaser.

**Acceptance Scenarios**:

1. **Given** the catalog contains entries belonging only to `core-rulebook` and entries belonging only to `sourcebook-aysle`, **When** an anonymous visitor lists the entries of any product-bound catalog area, **Then** only the `core-rulebook` entries are returned.
2. **Given** the same catalog, **When** an anonymous visitor lists entries filtered by a cosm, **Then** only entries of that cosm that belong to `core-rulebook` are returned.
3. **Given** an entry that belongs only to `sourcebook-aysle`, **When** an anonymous visitor requests it individually by its identifier, **Then** only a teaser is returned containing the entry's name and `sourcebook-aysle` as the product granting access, and none of the entry's protected content (texts, game statistics, embedded references).
4. **Given** an entry belonging to both `core-rulebook` and `sourcebook-aysle`, **When** an anonymous visitor lists or requests it, **Then** it is visible (ownership of any one of its products is sufficient).
5. **Given** an entry that has no product assigned, **When** any caller lists the catalog area or requests the entry individually, **Then** it is not listed and the single-entry request responds exactly as for a non-existent entry.

---

### User Story 2 - Product owners see exactly the entries of the products they own (Priority: P1)

A logged-in user (via any supported authentication method) who owns a set of products browses
the codex. They MUST see every entry that belongs to at least one product they own, plus all
free-tier entries, and nothing else.

**Why this priority**: Restricting access must not break paying customers. Owners must keep (and
reliably get) access to exactly what they paid for, which is the second half of a correct DRM.

**Independent Test**: Authenticate as an owner of `sourcebook-aysle` only and verify that both the
`sourcebook-aysle` entries and the free-tier entries are listed and retrievable, while entries of
a third, non-owned sourcebook are not.

**Acceptance Scenarios**:

1. **Given** a caller owning `sourcebook-aysle`, **When** they list a product-bound catalog area, **Then** the result contains all entries belonging to `sourcebook-aysle` or `core-rulebook` and no entries belonging exclusively to other products.
2. **Given** a caller owning `sourcebook-aysle`, **When** they request a `sourcebook-aysle` entry individually, **Then** the full entry is returned (with in-text product-gated passages censored as today).
3. **Given** an authenticated caller who owns no products at all, **When** they browse the catalog, **Then** they see the same entries an anonymous visitor sees (never fewer).
4. **Given** the same owned products, **When** ownership is established via a DriveThruRPG API key or via an OIDC token, **Then** the visible entries are identical.

---

### User Story 3 - DRM cannot be bypassed by any entry point (Priority: P2)

A maintainer adds a new way to read catalog data (a new endpoint, a page, a message consumer, an
export, a cross-reference lookup). The product-ownership restriction MUST apply automatically,
because it is enforced in the core catalog use cases themselves rather than in each entry point.

**Why this priority**: The current defect exists precisely because the restriction was left to
entry points. Enforcing it centrally prevents the same leak from reappearing with every new
feature. It ranks below P1 because the P1 stories already close the user-visible leak for the
existing entry points.

**Independent Test**: An automated architecture/regression check fails if any inbound entry point
can obtain catalog entries without the ownership restriction being applied, and a deliberate
regression (removing the restriction from the core use case) makes the test suite fail.

**Acceptance Scenarios**:

1. **Given** the implemented feature, **When** any existing entry point reads catalog entries (list, filtered list, single entry, cross-references), **Then** the result has passed through the single central ownership restriction.
2. **Given** a hypothetical regression that removes the central restriction, **When** the full automated test suite runs, **Then** it fails.
3. **Given** an entry the caller is allowed to see that refers to entries the caller is not allowed to see (e.g. a spell list containing spells from a non-owned sourcebook), **When** the entry is returned, **Then** the referenced non-accessible entries are not exposed (see FR-009).

---

### Edge Cases

- **Entry without any product assignment**: Hidden from everyone; a direct request behaves as for a non-existent entry (FR-008); a warning is logged and the encounter is counted in a metric (FR-016, FR-017).
- **Entry belonging to several products**: Visible if the caller owns at least one of them.
- **Stale or unknown product authorities** (caller holds a product id that no entry uses): Ignored; never grants access to anything else and never causes an error.
- **Authenticated caller with zero owned products**: Treated exactly like an anonymous visitor (free-tier access only); MUST NOT see fewer entries than anonymous.
- **Entry exists but caller is not entitled**: Only the teaser (identifier, name, granting products) is returned; existence is intentionally revealed as a purchase hint, protected content never is.
- **Teaser for an entry whose granting products include unknown/unpublished codex ids**: The teaser lists the codex ids as assigned; it MUST NOT fail because a product has no matching publication record.
- **Cross-references inside an accessible entry's text** pointing to a non-accessible entry: The reference MUST NOT expose the protected entry's content; it may lead at most to the teaser.
- **Teasers are never listed**: Listings never contain teasers for non-accessible entries; teasers appear only on direct single-entry requests.
- **Reference data without product binding** (Publications, Tags, Articles): Not restricted by entry-level ownership; Articles continue to have in-text gated passages censored as today.
- **Caching**: Any cached rendered or filtered result MUST NOT be served to a caller with a different set of owned products.
- **Ownership changes between requests** (e.g. a newly purchased book): Takes effect on the next request in which the caller's ownership is re-established, without requiring a restart.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST determine, for every catalog read, whether the caller may access an entry based solely on the overlap between the entry's assigned products and the products the caller owns.
- **FR-002**: An entry MUST be accessible to a caller if and only if the caller owns at least one of the entry's assigned products. The free-tier product (`core-rulebook`) MUST always count as owned by every caller, authenticated or not.
- **FR-003**: Listing a product-bound catalog area MUST return only entries accessible to the caller (FR-002); inaccessible entries MUST be omitted entirely, not merely redacted.
- **FR-004**: Filtered listings (e.g. by cosm) MUST apply the same restriction as unfiltered listings.
- **FR-005**: Retrieving a single entry by identifier MUST return the full entry only if it is accessible to the caller; otherwise FR-007 (teaser) or FR-008 (no product assigned) applies.
- **FR-006**: The restriction MUST be applied to all 14 product-bound catalog areas: Cosm, Item, Miracle, Miracle List, Perk, Perk Group, Power, Power List, Race, Shard, Spell, Spell List, Threat, Vehicle.
- **FR-007**: When a caller requests a single entry they may not access, the system MUST return a teaser containing only the entry's identifier, its name, and the product(s) that would grant access (so the caller can purchase them). The teaser MUST NOT contain any other entry content (texts, game statistics, clearance details, cosm assignments, embedded references) and MUST be clearly distinguishable from a full entry.
- **FR-008**: Entries that have no product assigned at all MUST be inaccessible to every caller (fail-closed): they MUST be omitted from all listings, and a single-entry request for them MUST respond exactly as for a non-existent entry (no teaser, since no product can grant access).
- **FR-009**: When an accessible entry references other entries (embedded entry lists such as spells in a spell list, or cross-references in its text), references to entries the caller cannot access MUST NOT expose those entries' protected content; embedded lists MUST omit them, and in-text references MAY lead at most to the referenced entry's teaser (FR-007).
- **FR-010**: The access restriction MUST be enforced centrally as a rule of the core catalog domain/use cases, so that every inbound entry point (REST endpoints, server-rendered pages, message consumers, cross-reference resolution, and any future entry point) receives already-restricted results and cannot obtain unrestricted entries.
- **FR-011**: The caller's owned products MUST be determined by the single existing product-ownership mechanism (`ROLE_<codex-id>` authorities, constitution Principle V); this feature MUST NOT introduce a second ownership-resolution mechanism.
- **FR-012**: The existing in-text censoring of product-gated passages (`<IF:…>` blocks) and the markup rendering order MUST remain unchanged for entries the caller may access, except that in-text censoring MUST use the same effective set of owned products as the access decision, i.e. it always includes the free tier (FR-002).
- **FR-013**: Reference data that is not bound to products (Publications, Tags, Articles) MUST remain listable and retrievable by all callers as today; Articles keep their in-text censoring.
- **FR-014**: Automated tests MUST demonstrate, for every product-bound catalog area, a genuine difference between an anonymous caller and an owner of a non-free product for both listing (entry omitted vs. present) and single-entry retrieval (teaser vs. full entry), MUST cover the fail-closed handling of entries without products (including the warning and metric of FR-016/FR-017), and MUST fail if the central restriction is removed or bypassed.
- **FR-015**: Existing response formats for listings and accessible entries MUST remain unchanged; the only changes are the set of returned entries and the new teaser response for non-accessible single entries (FR-007), which MUST be documented in the OpenAPI specification as the API source of truth. Markdown contracts are explanatory, not a replacement for OpenAPI.
- **FR-016**: Every time the access decision encounters an entry without any product assignment (FR-008) — in listings, filtered listings, single-entry requests, or reference resolution — the system MUST log one warning per encountered entry, without deduplication across requests. The warning MUST identify the catalog area, the entry's identifier, and the entry's name, and MUST NOT contain the caller's identity or owned products.
- **FR-017**: In addition to the warning (FR-016), every encounter of an entry without any product assignment MUST be counted in a monitoring metric, broken down by catalog area, so that operators can alert when it is greater than zero. The metric MUST NOT carry entry identifiers, caller identity, or owned products as dimensions.
- **FR-018**: The catalog data MUST be corrected before the restriction takes effect: the 10 core cosms without product assignment (Aysle, Core Earth, Cyberpapacy, Living Land, Nile Empire, Orrorsh, Pan-Pacifica, Tharkold, Universal, Unknown) MUST be assigned to `core-rulebook`, and every perk group MUST be assigned exactly the set of products its perks are assigned to, replacing the non-existent product `groups`. The correction MUST be a no-op where the affected entries do not exist (e.g. the public free-tier dataset) and MUST NOT overwrite cosms that already have products.

### Key Entities *(include if feature involves data)*

- **Catalog entry**: Any item of a product-bound catalog area (e.g. Cosm, Spell, Threat). Carries the set of products (codex ids) it is published in; this set is the resource attribute that drives access.
- **Product (codex id)**: A sourcebook or other publication identified by a codex id (e.g. `core-rulebook`, `sourcebook-aysle`). `core-rulebook` is the free tier.
- **Caller's owned products**: The set of codex ids the current caller is entitled to, derived from the single ownership mechanism, always including the free tier.
- **Access decision**: The domain rule "caller owns at least one of the entry's products" applied to every catalog read; entries without products are always denied.
- **Entry teaser**: The restricted view of a non-accessible entry: identifier, name, and the products that grant access. Contains no protected content.
- **Reference data**: Publications, Tags, Articles — not bound to products and not restricted at entry level.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Across all 14 product-bound catalog areas, an anonymous visitor finds 0 entries belonging exclusively to non-free products in listings or filtered listings, and receives 0 items of protected content (anything beyond identifier, name, and granting products) on single-entry retrieval of such entries.
- **SC-002**: 0 entries without product assignment are visible to any caller.
- **SC-003**: An anonymous visitor can still access 100% of free-tier entries.
- **SC-004**: A caller owning a given product can list and fully open 100% of that product's entries, finds 0 entries belonging exclusively to non-owned products in listings, and receives only teasers when opening such entries directly.
- **SC-005**: 100% of existing inbound entry points that read catalog entries are covered by automated tests proving the restriction, and a deliberate removal of the central restriction causes at least one automated test to fail.
- **SC-006**: Callers notice no slowdown: catalog listings and single-entry views remain as responsive as before the change (no more than 10% increase in typical response time).
- **SC-007**: Reference-data availability and markup rendering behavior remain unchanged, except that all callers implicitly own `core-rulebook` when rendering gated passages (including Articles), and catalog references follow FR-008/FR-009. For unchanged effective ownership and unaffected references, rendered output remains identical.
- **SC-008**: 100% of encounters with product-less entries produce exactly one warning per entry and request, and are reflected in the per-catalog-area monitoring count; when no such entries exist, the count stays at zero and no warning is emitted.

## Assumptions

- The free-tier product is `core-rulebook`, as currently configured for anonymous visitors; every caller (including authenticated callers who own nothing) implicitly owns it.
- Each product-bound catalog entry carries its product assignment in the catalog data, except for the gaps corrected by FR-018 (10 core cosms without products; perk groups assigned to the non-existent product `groups`).
- Privileged roles (Admin, Orga, Judge, etc.) receive no implicit DRM bypass in this feature; they see what their product ownership grants. Any bypass would require a separate decision.
- Owning any one of an entry's products is sufficient for full access to that entry; partial access per product is handled only by the existing in-text censoring.
- Publications must remain visible to everyone so visitors can discover which books to purchase; Tags and Articles are not product-bound reference data.
- Ownership is re-evaluated per request from the caller's current authentication; no persistent per-user access cache is introduced.
- No schema change is expected; the data correction (FR-018) is a pure data migration that follows the Liquibase changeset rules (ADR-011).
- Entries without product assignment are rare data errors; the per-request warning volume (FR-016) is therefore acceptable and is expected to drop to zero once the data is corrected.
