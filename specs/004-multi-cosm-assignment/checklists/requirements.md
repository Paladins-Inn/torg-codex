# Specification Quality Checklist: Multi-Cosm Assignment for Catalog Entries

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-09-12
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no implementation details)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

## Notes

- **Scope extended (Session 2026-09-12)**: A full column scan found encoded list literals in three
  families, not one. `unlocking_perk` (5 rows, miracle lists) is the same defect class as `cosm` and
  is treated identically (FR-016 to FR-020). The prose fields `additional_features`, `quote` and
  `notes` (97 rows) are a formatting defect and are converted to Markdown (FR-021 to FR-027).
- **Resolved (Session 2026-09-12)**: Prose conversion runs as a post-load migration, not by editing
  the shipped data files, so separately delivered data is covered too.
- **Resolved (Session 2026-09-12)**: Multiple unlocking perks are joined as comma-separated slugs,
  never resolved to display names, keeping the single-value case byte-identical.
- **Resolved (Session 2026-09-12)**: A single-statement prose value becomes plain text, not a
  one-item bullet list. This is the majority case (44 of 93 item rows).
- **Resolved (Session 2026-09-12)**: The "old cosm field" to be removed is the **storage column**,
  not the API field. The API field keeps its shape (FR-013); the legacy storage is dropped in the
  same release (FR-014, FR-015, SC-008).
- **Resolved (Session 2026-09-12)**: Dropping the legacy storage in the same release conflicts with
  the three-phase migration rule. Recorded as a deliberate, justified exception in the plan's
  Complexity Tracking; the rule stays in force for future features.
- **Resolved**: FR-013 was clarified by the user — the existing cosm field is kept rather than
  replaced. When an entry has several cosms, the field carries a comma-separated list of their
  display names and no single cosm identity (FR-004a to FR-004c).
- The user input mentioned "78 rows"; measurement of the shipped data set found **62** rows with
  encoded multi-value cosm references (58 perks, 3 miracle lists, 1 power list). The specification
  uses the measured figure. The earlier estimate counted encoded list values in other columns too.
- All checklist items pass. The specification is ready for `/speckit.plan`.
