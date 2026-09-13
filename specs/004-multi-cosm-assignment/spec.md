# Feature Specification: Multi-Cosm Assignment for Catalog Entries

**Feature Branch**: `004-multi-cosm-assignment`

**Created**: 2026-09-12

**Status**: Draft

**Input**: User description: "die 78 zeilen mit python-listen müssen in jpa-technologie abgebildet werden. dazu muss es möglich sein, jedem eintrag mehrere cosms zuweisen zu können."

**Scope**: A full scan of the shipped catalog data found encoded list values in three distinct
families of fields. All three are in scope:

| Family | Affected content | Rows (public data) | Defect |
|--------|------------------|--------------------|--------|
| Cosm reference | Cosm of perks, miracle lists and power lists | 62 | Entry matches no cosm filter |
| Unlocking-perk reference | Unlocking perk of miracle lists | 5 | Entry matches no unlocking-perk lookup |
| Prose / markup | Additional features of items, quote of threats, notes of spell lists | 97 | Encoded value shown verbatim to readers |

The two reference families are treated identically: a real many-valued assignment replacing the
single-valued column. The prose family is a formatting defect, not a modelling defect, and is
converted to Markdown.

## Clarifications

### Session 2026-09-12

- Q: Welches der beiden „alten cosm-Felder" soll entfernt werden — die Datenbankspalte, das
  API-Feld, oder beide? → A: Nur die Datenbankspalte. Das API-Feld behält seine Form und bleibt
  komma-separiert wie in FR-004a bis FR-004c beschrieben.
- Q: Wie soll die Kollision mit Prinzip IV aufgelöst werden, das vorschreibt, dass das Löschen
  alter Strukturen in einem separaten Deployment erfolgen muss? → A: Als begründete Ausnahme im
  Complexity Tracking des Plans dokumentieren; Prinzip IV bleibt für künftige Features in Kraft.
- Q: Wie sollen die Python-Listen in den Prosa-Feldern in Markdown umgewandelt werden — durch einen
  Migrationsschritt beim Hochfahren, oder durch Korrektur der ausgelieferten CSV-Dateien? → A:
  Migrationsschritt nach dem Laden, analog zur cosm-Migration; erfasst auch proprietäre Daten.
- Q: Wenn eine Liste mehrere freischaltende Perks hat, soll das beibehaltene Feld die Perk-Slugs
  komma-separiert enthalten, oder die lesbaren Perk-Namen? → A: Komma-separierte Slugs; der
  Einzelwert-Fall bleibt exakt wie heute.
- Q: Was soll aus einem Prosa-Feld werden, das nur einen einzigen Listeneintrag enthält — eine
  einelementige Markdown-Liste oder schlichter Fließtext? → A: Ein Eintrag wird Fließtext, zwei oder
  mehr werden eine Markdown-Aufzählung.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Finding multi-cosm content through a cosm filter (Priority: P1)

A player or game master browses the codex and narrows the catalog down to a single cosm, for
example Core Earth. Today, entries that are valid in several cosms are stored with an encoded
list value in their single cosm field, so they match no cosm filter at all and are effectively
invisible. After this change, every entry that is valid in the selected cosm appears in the
filtered result, regardless of how many other cosms it also belongs to.

**Why this priority**: This is the actual user-visible defect. 62 catalog entries (58 perks,
3 miracle lists, 1 power list) are currently unreachable through any cosm filter. Restoring
their discoverability is the whole point of the feature and delivers value on its own.

**Independent Test**: Filter the perk catalog by `core-earth` and confirm that perks which are
valid in Core Earth plus other cosms are contained in the result. Repeat for a second cosm that
shares the same entry and confirm the entry appears there too.

**Acceptance Scenarios**:

1. **Given** a perk that is valid in Core Earth, Pan-Pacifica and Tharkold, **When** a user
   filters perks by Core Earth, **Then** that perk is part of the result set.
2. **Given** the same perk, **When** a user filters perks by Tharkold, **Then** that perk is
   part of that result set as well.
3. **Given** a perk that is valid only in Aysle, **When** a user filters perks by Core Earth,
   **Then** that perk is not part of the result set.
4. **Given** an entry with no cosm assigned at all, **When** a user filters by any cosm,
   **Then** that entry is not part of the result set.

---

### User Story 2 - Seeing every cosm an entry belongs to (Priority: P2)

A reader opens the detail view of a catalog entry and wants to know in which cosms the entry can
be used. Today a multi-cosm entry either shows a raw encoded value such as
`['core-earth', 'tharkold']` or nothing meaningful. After this change the entry names every cosm
it belongs to, using human-readable display names joined into one readable value.

**Why this priority**: Correct discovery (P1) is worthless if the presented information is still
unreadable, but the catalog remains usable while only the display is incomplete, so this ranks
below the filter fix.

**Independent Test**: Open the detail view of a known multi-cosm entry and verify that all of its
cosms are named with their display names and that no raw encoded value is shown anywhere.

**Acceptance Scenarios**:

1. **Given** an entry valid in Core Earth and Tharkold, **When** a user opens its detail view,
   **Then** its cosm reads "Core Earth, Tharkold", naming both display names in one value.
2. **Given** an entry valid in exactly one cosm, **When** a user opens its detail view,
   **Then** exactly that one cosm is shown, unchanged from today's presentation.
3. **Given** an entry whose stored cosm reference cannot be resolved to a known cosm, **When** a
   user opens its detail view, **Then** the unresolved reference is still shown as-is rather
   than being silently dropped.
4. **Given** any entry in the catalog, **When** a user views it in a list or detail view,
   **Then** no encoded list value such as `['core-earth', 'tharkold']` is ever displayed.
5. **Given** an entry valid in several cosms, **When** a consumer reads its cosm, **Then** the
   value carries no single cosm identity, signalling that the entry is not tied to one cosm.

---

### User Story 3 - Reading prose fields without encoded lists (Priority: P2)

A reader opens an item, a threat or a spell list and reads its descriptive text. Today 97 of those
records show a raw encoded value such as `['puts out fires', 'might be used for tricks']` —
brackets, quotes and commas included — in the middle of otherwise well-formatted prose. After this
change the same information reads as ordinary formatted text: a bullet list when there are several
statements, a plain sentence when there is only one.

**Why this priority**: This affects far more records than the reference defects and every one of
them is directly visible to readers, but the affected entries remain findable and usable, so it
ranks below the filter defect.

**Independent Test**: Open an item known to carry several additional features and confirm the
features render as a bullet list with no brackets or quotes. Open an item with exactly one feature
and confirm it renders as a plain sentence.

**Acceptance Scenarios**:

1. **Given** an item whose additional features hold two or more statements, **When** a user views
   the item, **Then** the statements are rendered as a bullet list, one bullet per statement.
2. **Given** an item whose additional features hold exactly one statement, **When** a user views the
   item, **Then** the statement is rendered as plain text without any bullet.
3. **Given** any affected prose field, **When** a user views it, **Then** no brackets, quotes or
   separators from the encoded form are visible.
4. **Given** a prose field whose text merely contains bracket characters without being an encoded
   list, **When** the conversion runs, **Then** the text is left untouched.
5. **Given** a statement containing formatting or an entity reference, **When** it is converted,
   **Then** that formatting and the reference still work afterwards.

---

### User Story 4 - Finding lists by their unlocking perk (Priority: P3)

A reader or an automated consumer looks up which miracle, power or spell lists a given perk unlocks.
Today, five miracle lists name several unlocking perks encoded as a list in a single-valued field,
so they are returned for none of those perks. After this change, a list is found through every perk
that unlocks it.

**Why this priority**: The defect is identical in kind to the cosm defect and deserves the same
treatment, but it affects far fewer records and a less prominent lookup path.

**Independent Test**: Look up the lists unlocked by a perk that is one of several unlocking perks of
a miracle list and confirm that list is returned. Repeat for a second perk of the same list.

**Acceptance Scenarios**:

1. **Given** a miracle list unlocked by the perks "miracles" and "exemplar-of-light", **When** a
   consumer looks up lists unlocked by "miracles", **Then** that list is returned.
2. **Given** the same list, **When** a consumer looks up lists unlocked by "exemplar-of-light",
   **Then** that list is returned as well.
3. **Given** a list with exactly one unlocking perk, **When** a consumer reads its unlocking perk,
   **Then** the value is unchanged from today.
4. **Given** a list with several unlocking perks, **When** a consumer reads its unlocking perk,
   **Then** the value names all of them as a comma-separated list of perk slugs.
5. **Given** a list with no unlocking perk, **When** a consumer reads its unlocking perk, **Then**
   the value stays empty.

---

### User Story 5 - Maintaining multi-cosm data without encoding tricks (Priority: P3)

A data maintainer imports or corrects catalog data and needs to express that an entry belongs to
several cosms. Today the only way is to squeeze a list into a single-value field, which no part
of the system understands. After this change, multiple cosms are a first-class property of an
entry, so the import produces correct, queryable data and existing single-cosm entries continue
to load unchanged.

**Why this priority**: This removes the root cause and prevents the defect from reappearing with
future data deliveries, but the two user-facing stories already deliver the visible value.

**Independent Test**: Load the full catalog data set from scratch and verify that no entry
retains an encoded list value, that the 62 previously broken entries carry the expected set of
cosms, and that the count of entries per cosm matches the source data.

**Acceptance Scenarios**:

1. **Given** the shipped catalog data set, **When** the data is loaded into an empty system,
   **Then** zero entries hold an encoded list value in their cosm assignment.
2. **Given** an entry that was previously assigned a single cosm, **When** the data is loaded,
   **Then** the entry is assigned exactly that one cosm.
3. **Given** an existing installation with already-loaded data, **When** the system is upgraded,
   **Then** the existing cosm assignments are preserved and the previously encoded entries are
   converted into their individual cosm assignments without manual intervention.
4. **Given** a source record that names the same cosm twice, **When** the data is loaded,
   **Then** the entry is assigned that cosm exactly once.

---

### Edge Cases

- **No cosm assigned**: Entries without any cosm remain valid and are excluded from every cosm
  filter. Their cosm field stays empty rather than showing an empty placeholder.
- **Unknown cosm reference**: A reference that does not match any known cosm is retained and
  displayed as the raw reference value, matching today's fallback behaviour, so data errors stay
  visible instead of disappearing.
- **Duplicate references**: The same cosm named more than once for one entry results in a single
  assignment.
- **Large assignment sets**: Entries assigned up to seven cosms exist in the current data set and
  must be handled without truncation, including in the combined display value.
- **Display names containing separators**: No known cosm display name contains a comma, so joining
  display names with a comma stays unambiguous. Should such a name ever be introduced, the
  combined value would become ambiguous for consumers that split it.
- **Filter on an unused cosm**: Filtering by a cosm that no entry references returns an empty
  result, not an error.
- **Case and spelling of references**: Cosm references are matched by their stable slug, never by
  the human-readable display name, so a data record naming a display name must not silently match.
- **Mixed-zone cosms**: A dedicated combined cosm (for example the Aysle/Tharkold mixed zone) is a
  cosm in its own right and must not be confused with an entry that is assigned both Aysle and
  Tharkold individually.
- **Partial encoded value in prose**: A prose text that merely contains bracket characters, or that
  starts with prose and embeds a list literal somewhere inside, is not an encoded list and stays
  untouched. Only a value whose entire content is a list literal is converted.
- **Prose statement containing an apostrophe**: Statements are quoted in the encoded form, so an
  apostrophe inside a statement is escaped or the statement is quoted differently. The conversion
  must recover the original text rather than truncating at the first apostrophe.
- **Prose statement containing formatting or references**: Statements may contain markup and entity
  references. Conversion only changes the surrounding structure, never the statement text, so all
  markup keeps working.
- **Empty prose list**: An encoded value with no statements becomes empty rather than an empty
  bullet.
- **Unlocking perk that matches no known perk**: The reference is retained and surfaced unchanged,
  matching the cosm behaviour, because it is a data-quality issue rather than a supported case.
- **Repeated unlocking perk**: The same perk named twice for one list results in a single
  assignment.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST allow every catalog entry type that currently carries a cosm
  reference to be assigned zero, one, or many cosms.
- **FR-002**: The system MUST match a cosm assignment by the cosm's stable slug for all filtering
  and comparison, and MUST present the cosm's display name to users.
- **FR-003**: Filtering a catalog by a cosm MUST return every entry that has that cosm among its
  assignments, and no entry that does not.
- **FR-004**: List and detail views MUST keep the existing single cosm field rather than replacing
  it with a collection, and MUST expose the complete set of cosms assigned to an entry through it.
- **FR-004a**: When an entry is assigned exactly one cosm, the cosm field MUST carry that cosm's
  identity and display name, unchanged from today's behaviour.
- **FR-004b**: When an entry is assigned several cosms, the cosm field MUST carry the display names
  of all assigned cosms as a single comma-separated value, and MUST carry no single cosm identity,
  because no one cosm represents the entry.
- **FR-004c**: When an entry is assigned no cosm, the cosm field MUST stay empty, unchanged from
  today's behaviour.
- **FR-005**: The system MUST NOT expose or display encoded multi-value representations such as
  `['core-earth', 'tharkold']` in any user-facing or machine-facing output.
- **FR-006**: On upgrade of an existing installation, the system MUST convert all existing cosm
  references, including the 62 encoded multi-value records, into individual cosm assignments
  without data loss and without manual operator action. The same MUST hold for unlocking-perk
  references and prose fields.
- **FR-007**: The conversion MUST be repeatable and MUST leave already-converted installations
  unchanged when applied again.
- **FR-008**: Entries that were assigned exactly one cosm before the change MUST end up with
  exactly that one cosm afterwards, and their filter and detail results MUST be unchanged.
- **FR-009**: The system MUST deduplicate repeated references to the same cosm within one entry.
- **FR-010**: The system MUST preserve an unresolvable cosm reference and surface it rather than
  discarding it.
- **FR-011**: The order in which an entry's cosms are returned MUST be deterministic and stable
  across repeated requests, so that automated comparisons of responses remain reliable.
- **FR-012**: Access control and content censoring behaviour for an entry MUST remain unchanged;
  the number of cosms assigned to an entry MUST NOT affect who may see it.
- **FR-013**: The cosm field MUST retain its existing shape so that current consumers keep working;
  the multi-value nature of a cosm assignment MUST be expressed within that field as described in
  FR-004a to FR-004c, and MUST NOT be introduced as an additional or replacement field.
- **FR-014**: The legacy single-value cosm storage MUST be removed in the same release that
  introduces the multi-cosm assignments, once every existing reference has been converted. No
  read or write path against the legacy storage may remain afterwards, and no fallback to it is
  required.
- **FR-015**: Because the legacy storage disappears in the same release, the system MUST NOT be
  expected to run the previous and the new version side by side during the upgrade. The upgrade
  MUST fail safely and leave the data untouched if the conversion required by FR-006 cannot be
  completed.

**Unlocking-perk references**

- **FR-016**: The system MUST allow every catalog entry type that carries an unlocking-perk
  reference today — miracle lists, power lists and spell lists — to be assigned zero, one, or many
  unlocking perks.
- **FR-017**: Looking up entries by an unlocking perk MUST return every entry that names that perk
  among its unlocking perks, and no entry that does not.
- **FR-018**: The unlocking-perk field MUST retain its existing shape. When one perk is assigned it
  MUST carry that perk's reference unchanged from today; when several are assigned it MUST carry all
  of their references as a single comma-separated value; when none is assigned it MUST stay empty.
- **FR-019**: Unlocking-perk references MUST be carried as the perk's stable reference value, never
  translated into a display name, so that consumers reading a single-valued field today keep
  receiving the identical value.
- **FR-020**: Unlocking-perk references MUST be deduplicated, unresolvable references MUST be
  preserved and surfaced, ordering MUST be deterministic, and the legacy single-value storage MUST
  be removed in the same release — matching FR-009, FR-010, FR-011 and FR-014 exactly.

**Prose and markup fields**

- **FR-021**: Every prose or markup field whose stored value consists entirely of an encoded list
  MUST be converted to equivalent Markdown, so that no reader ever sees the encoded form.
- **FR-022**: A converted value holding two or more statements MUST become a Markdown bullet list
  with one bullet per statement, preserving the order of the statements.
- **FR-023**: A converted value holding exactly one statement MUST become that statement as plain
  text, without any list markup.
- **FR-024**: A value that is not entirely an encoded list MUST be left byte-identical, including
  values that merely contain bracket characters.
- **FR-025**: The conversion MUST preserve each statement's own text unchanged, including markup and
  entity references, and MUST only alter the structure surrounding the statements.
- **FR-026**: The conversion MUST be repeatable and MUST leave already-converted values unchanged
  when applied again.
- **FR-027**: The conversion MUST apply to the shipped data regardless of whether it is part of the
  publicly available data set or a separately delivered one, so it MUST derive its result from the
  stored value rather than from a fixed list of known records.

### Key Entities *(include if feature involves data)*

- **Cosm**: A reality or world in the game setting. Has a stable slug used for references and
  filtering, and a human-readable display name used for presentation. 19 cosms exist.
- **Catalog Entry**: Any codex record that can be assigned to a cosm. Covers items, miracle lists,
  perks, power lists, shards, spell lists, threats and vehicles.
- **Cosm Assignment**: The association between a catalog entry and a cosm. An entry may hold many
  assignments; an assignment names exactly one cosm; the same cosm appears at most once per entry.
- **Unlocking Perk Assignment**: The association between a miracle, power or spell list and a perk
  that unlocks it. A list may hold many assignments; an assignment names exactly one perk; the same
  perk appears at most once per list.
- **Prose Field**: A descriptive text on a catalog entry that is authored as markup and rendered for
  readers. Covers the additional features of an item, the quote of a threat and the notes of a
  spell list. It carries formatted text, never a structured list of references.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Zero catalog entries hold an encoded multi-value cosm reference after the upgrade,
  down from 62 today.
- **SC-002**: All 62 previously unreachable entries are returned when filtering by each of the
  cosms they belong to, for a total of 100% recall on the affected records.
- **SC-003**: Every catalog entry that was returned by a cosm filter before the change is still
  returned by that same filter afterwards, i.e. zero regressions on existing results.
- **SC-004**: No user-facing or machine-facing catalog response contains a raw encoded list value.
- **SC-005**: Repeating the same catalog request twice yields byte-identical cosm information,
  confirming stable ordering within the combined display value.
- **SC-005a**: Every catalog entry assigned exactly one cosm returns byte-identical cosm
  information before and after the change, confirming the field shape is preserved.
- **SC-006**: Catalog list and detail responses continue to feel instantaneous to users, with no
  perceptible slowdown compared to before the change.
- **SC-007**: Upgrading an existing installation completes without operator intervention. The
  upgrade requires a short service interruption for the duration of the release, because the
  previous and the new version cannot serve traffic at the same time once the legacy storage is
  removed.
- **SC-008**: After the upgrade, the legacy single-value cosm storage is gone for all eight
  catalog entry types, verified by the schema reporting zero remaining legacy cosm columns.
- **SC-009**: Zero catalog entries hold an encoded multi-value unlocking-perk reference after the
  upgrade, down from 5 today, and the legacy single-value unlocking-perk storage is gone for all
  three affected entry types.
- **SC-010**: All 5 previously unreachable lists are returned when looking them up by each of the
  perks that unlock them, for a total of 100% recall on the affected records.
- **SC-011**: Zero prose fields across the whole catalog hold an encoded list value after the
  upgrade, down from 97 today, and every previously correct prose value is returned byte-identical.
- **SC-012**: Applying the conversion a second time changes no record, confirming repeatability
  across all three defect families.

## Assumptions

- The multi-cosm capability is applied uniformly to all eight catalog entry types that carry a
  cosm reference today, even though encoded multi-value data currently only occurs in perks,
  miracle lists and power lists. A mixed model, where some entry types allow many cosms and others
  only one, would be inconsistent for both users and consumers.
- The order of cosms in the source data is not meaningful. The same combination occurs in
  different orders across records, so an entry's cosms are treated as an unordered set and
  presented in a deterministic, system-defined order within the combined display value.
- The cosm field keeps its current shape by explicit user decision. Multiple cosms are expressed
  as a comma-separated list of display names inside that field rather than as a new collection
  field, so existing consumers keep parsing the same structure.
- A comma followed by a space is the separator for the combined display value, matching normal
  written German and English enumeration.
- There is no concept of a primary or leading cosm for an entry. All assigned cosms are equal.
- The existing separation of cosm slug for filtering and cosm display name for presentation,
  introduced previously, stays in force and is the basis for this feature.
- The set of 19 known cosms is unchanged by this feature. No new cosms are introduced and none are
  merged or removed.
- Records that reference a cosm which is not among the 19 known cosms are data-quality issues, not
  a supported case. They are preserved and surfaced, but no repair logic is specified here.
- Shipped catalog data files are immutable once released, so the conversion is delivered as an
  additional, forward-only upgrade step rather than by editing existing data files.
- The system has no production installation that depends on a rolling update, so removing the
  legacy cosm storage in the same release is acceptable by explicit user decision. This is a
  deliberate, documented exception to the otherwise binding three-phase migration rule and does
  not change that rule for future features.
- Existing behaviour for product ownership and content censoring is out of scope and must remain
  untouched.
- Unlocking-perk references are treated exactly like cosm references by explicit user decision:
  same many-valued model, same retained single field, same removal of the legacy storage in this
  release. The only deliberate difference is that unlocking perks are never translated into display
  names, because the field does not resolve them today either.
- The many-valued unlocking-perk capability is applied uniformly to all three list types that carry
  the reference today, even though encoded data currently occurs only in miracle lists, for the same
  consistency reason that applies to cosms.
- Prose fields are a formatting defect, not a modelling defect. They stay single text values and are
  repaired in place; no collection is introduced for them.
- A prose value is only converted when its entire content is an encoded list. This is safe because a
  scan of the shipped data found no case where a list literal is embedded inside longer text.
- The prose conversion is delivered as a post-load migration step rather than by editing the shipped
  data files, by explicit user decision, so that separately delivered data is converted too.
- The row counts given for prose fields (93 item, 3 threat, 1 spell list) are measured on the
  publicly available data set. A separately delivered data set is expected to contain further
  affected rows, which is why the conversion derives its result from the stored value (FR-027).
